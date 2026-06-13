package com.misterbil.ragbatch.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.misterbil.ragbatch.batch.FileToChunksProcessor;
import com.misterbil.ragbatch.batch.UnreadableSourceException;
import com.misterbil.ragbatch.batch.VectorStoreWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * LE JOB D'INGESTION — l'architecture qu'on ne pouvait pas faire avec
 * le SimpleVectorStore en mémoire de spring-ai-demo :
 *
 *   ┌─────────────┐   écrit    ┌──────────────────┐   lit    ┌─────────────┐
 *   │  ragbatch    │ ────────► │ PostgreSQL        │ ◄─────── │ application │
 *   │  (ce job)    │           │ + pgvector        │          │ RAG         │
 *   └─────────────┘            └──────────────────┘          └─────────────┘
 *   lit les fichiers,          chunks + vecteurs              ne voit JAMAIS
 *   tourne PUIS s'arrête       persistants                    les fichiers
 *
 * Le job s'exécute en 3 steps, séquentiels — si un step échoue, les
 * suivants ne tournent pas et le job est marqué FAILED dans les tables
 * BATCH_* :
 *
 *   1. purge    : vide la table vector_store (idempotence : relancer le
 *                 job ne doit pas dupliquer les chunks)
 *   2. ingest   : fichiers → chunks → embeddings → PGVector
 *                 (le step "chunk-oriented" reader/processor/writer)
 *   3. verify   : recherche de similarité de contrôle — prouve que ce
 *                 qui vient d'être écrit est interrogeable
 *
 * ─── Vocabulaire Spring Batch, en une passe ─────────────────────────────
 * Job        = l'unité lancée ; Step = une phase du job.
 * Tasklet    = step "bloc de code unique" (purge, verify).
 * Chunk-oriented = step "flux d'items" : le framework appelle
 *   reader (1 item) → processor (transformation) → writer (par paquets),
 *   le tout transactionnel. chunk(2) = commit toutes les 2 écritures.
 * JobRepository = l'historique des exécutions, persisté dans les tables
 *   BATCH_* de la MÊME base PostgreSQL. C'est ce qui distingue un vrai
 *   batch d'un main() : statuts consultables, relance, reprise.
 * RunIdIncrementer = ajoute un paramètre run.id auto-incrémenté ; sans
 *   lui, Spring Batch refuse de relancer un job déjà COMPLETED avec les
 *   mêmes paramètres (protection anti-double-exécution volontaire).
 */
@Configuration
public class IngestionJobConfig {

    private static final Logger log = LoggerFactory.getLogger(IngestionJobConfig.class);

    // ──────────────────────────────────────────────────────────────────
    // LE JOB : purge → ingest → verify
    // ──────────────────────────────────────────────────────────────────
    @Bean
    Job ingestionJob(JobRepository jobRepository, Step purgeStep, Step ingestStep, Step verifyStep) {
        return new JobBuilder("ingestionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(purgeStep)
                .next(ingestStep)
                .next(verifyStep)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // STEP 1 — PURGE (tasklet)
    // Stratégie d'idempotence la plus simple : on repart de zéro à
    // chaque exécution ("full reload"). Suffisant pour un corpus de
    // quelques centaines de fichiers. L'alternative incrémentale
    // (détecter les fichiers nouveaux/modifiés, supprimer leurs anciens
    // chunks via les métadonnées source) est l'exercice d'après.
    // ──────────────────────────────────────────────────────────────────
    @Bean
    Step purgeStep(JobRepository jobRepository, PlatformTransactionManager txManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("purgeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int deleted = jdbcTemplate.update("DELETE FROM vector_store");
                    log.info("Purge : {} anciens chunks supprimés", deleted);
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // STEP 2 — INGESTION (chunk-oriented)
    // item = UN FICHIER. reader liste les fichiers, processor découpe
    // en chunks de texte, writer embedde et insère. chunk(2) : commit
    // tous les 2 fichiers — si le 5e fichier plante, les 4 premiers
    // sont déjà en base et la reprise repart du bon endroit.
    //
    // ─── Tolérance aux pannes : retry vs skip ───────────────────────
    // faultTolerant() active la gestion fine des échecs, pilotée PAR
    // TYPE D'EXCEPTION — c'est pour ça qu'on ne fait pas de module de
    // retry maison : le framework sait déjà rejouer ou sauter un item.
    //
    //   retry = panne TRANSITOIRE, l'item est bon, on REJOUE.
    //     TransientAiException = le modèle d'embeddings (Ollama) a
    //     répondu timeout/surcharge. Le même fichier est re-soumis
    //     jusqu'à 3 fois avant que le step ne soit déclaré FAILED.
    //
    //   skip = item POURRI, le rejouer ne changera rien, on SAUTE.
    //     UnreadableSourceException = fichier corrompu/illisible
    //     (levée par FileToChunksProcessor). On le saute et on
    //     continue — jusqu'à 2 fois ; au 3e fichier pourri, le corpus
    //     lui-même est suspect et le step échoue.
    //
    // Toute AUTRE exception reste fatale : step FAILED immédiat, puis
    // reprise possible au bon endroit grâce au JobRepository.
    // Le SkipListener trace chaque fichier sauté — sinon un skip est
    // silencieux et le trou dans le corpus passe inaperçu.
    // ──────────────────────────────────────────────────────────────────
    @Bean
    Step ingestStep(JobRepository jobRepository, PlatformTransactionManager txManager,
            VectorStore vectorStore,
            @Value("${app.sources}") String sourcesPattern) throws IOException {

        // Résout le pattern (file:docs/*.md) en liste de fichiers.
        // ListItemReader les sert ensuite un par un au framework.
        List<Resource> sources = Arrays.asList(
                new PathMatchingResourcePatternResolver().getResources(sourcesPattern));
        log.info("Ingestion : {} fichier(s) trouvé(s) pour {}", sources.size(), sourcesPattern);

        return new StepBuilder("ingestStep", jobRepository)
                .<Resource, List<Document>>chunk(2, txManager)
                .reader(new ListItemReader<>(sources))
                .processor(new FileToChunksProcessor())
                .writer(new VectorStoreWriter(vectorStore))
                .faultTolerant()
                .retry(TransientAiException.class)
                .retryLimit(3)
                .skip(UnreadableSourceException.class)
                .skipLimit(2)
                .listener(new SkipListener<Resource, List<Document>>() {
                    @Override
                    public void onSkipInProcess(Resource file, Throwable t) {
                        log.warn("Fichier sauté (illisible) : {} — cause : {}",
                                file.getFilename(), t.getMessage());
                    }
                })
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // STEP 3 — VÉRIFICATION (tasklet)
    // Une recherche de similarité de contrôle, comme le ferait
    // l'application RAG : embedding de la question, puis SQL
    // "ORDER BY embedding <=> ? LIMIT 3" exécuté par PGVector.
    // Aucun résultat = données inexploitables = on FAIT ÉCHOUER le job
    // (exception → step FAILED) plutôt que de livrer une base morte.
    // ──────────────────────────────────────────────────────────────────
    @Bean
    Step verifyStep(JobRepository jobRepository, PlatformTransactionManager txManager,
            VectorStore vectorStore) {
        return new StepBuilder("verifyStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                String question = "Quelle est la politique de retour ?";
                    List<Document> results = vectorStore.similaritySearch(
                            SearchRequest.builder().query(question).topK(3).build());

                    if (results.isEmpty()) {
                        throw new IllegalStateException(
                                "Vérification échouée : aucun chunk trouvé pour « " + question + " »");
                    }
                    log.info("Vérification OK : {} chunks trouvés pour « {} »", results.size(), question);
                    results.forEach(doc -> log.info("  → [{}] {}...",
                            doc.getMetadata().get("source"),
                            doc.getText().substring(0, Math.min(80, doc.getText().length())).replace('\n', ' ')));
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
