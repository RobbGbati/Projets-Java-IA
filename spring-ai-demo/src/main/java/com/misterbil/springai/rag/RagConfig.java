package com.misterbil.springai.rag;

import java.io.File;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

/**
 * EXTENSION 9.3 — RAG, partie INGESTION : remplir le vector store
 * à partir de FICHIERS (src/main/resources/docs/*.md).
 *
 * ─── Le problème que résout le RAG ──────────────────────────────────────
 * Le LLM ne connaît que ses données d'entraînement. Il ne sait RIEN de
 * ton entreprise : tes procédures, tes produits, tes docs internes.
 * Demande-lui la politique de retour de "TechNova" → il invente.
 *
 * RAG = Retrieval Augmented Generation : avant de répondre, on RECHERCHE
 * les passages pertinents dans NOS données et on les colle dans le
 * prompt. Le LLM ne "sait" toujours rien : on lui souffle la réponse.
 *
 * ─── Le pipeline ETL de Spring AI ───────────────────────────────────────
 *
 *   DocumentReader   →   DocumentTransformer   →   DocumentWriter
 *   (TextReader)         (TokenTextSplitter)       (vectorStore.add)
 *   lit le fichier       découpe en CHUNKS         embedde + stocke
 *      Extract               Transform                  Load
 *
 * Le CHUNKING est l'étape critique. On ne vectorise jamais un fichier
 * entier : son embedding moyennerait tous les sujets qu'il contient
 * (sens dilué → recherche imprécise), et le retrouver entier ferait
 * exploser le prompt. TokenTextSplitter découpe en morceaux de ~800
 * tokens : c'est le CHUNK, pas le fichier, qui est vectorisé, retrouvé
 * par similarité, et injecté dans le prompt. Les documents sources sont
 * des fichiers Markdown sur une entreprise FICTIVE : si le LLM répond
 * juste, la réponse ne peut venir QUE d'ici. Pour enrichir la base,
 * dépose un .md dans docs/ — aucun code à changer.
 *
 * Pour d'autres formats, seul le Reader change : PagePdfDocumentReader
 * (dépendance spring-ai-pdf-document-reader) pour du PDF,
 * TikaDocumentReader (spring-ai-tika-document-reader) pour Word/HTML.
 *
 * ─── Le cache disque, ou "pourquoi pas à chaque démarrage" ──────────────
 * Embedder coûte un appel modèle par chunk : acceptable pour 2 fichiers,
 * pas pour 500 PDF. SimpleVectorStore sait se sérialiser en JSON :
 * on ingère une seule fois, puis on recharge les vecteurs depuis le
 * disque aux démarrages suivants. C'est la version artisanale du vrai
 * pattern de prod : un job d'ingestion SÉPARÉ qui écrit dans une base
 * vectorielle persistante (PGVector...), l'application ne lisant jamais
 * les fichiers sources. Même interface VectorStore des deux côtés.
 *
 * Conséquence du cache : un .md modifié n'est PAS ré-ingéré tant que
 * data/vectorstore.json existe. Supprime ce fichier pour ré-ingérer.
 * (Et si tu changes de modèle d'embeddings, supprime-le aussi : des
 * vecteurs issus de modèles différents ne sont pas comparables.)
 *
 * ─── @Profile("!pgvector") ──────────────────────────────────────────────
 * Cette configuration n'est active QUE hors du profil "pgvector". Avec
 * le profil pgvector, ce bean disparaît et c'est le PgVectorStore
 * auto-configuré (branché sur la base ragbatch) qui prend sa place,
 * injecté dans RagController via l'interface VectorStore. Aucun des deux
 * mondes n'interfère avec l'autre.
 */
@Configuration
@Profile("!pgvector")
public class RagConfig {

    private static final File CACHE = new File("data/vectorstore.json");

    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel,
            // Spring résout le pattern en autant de Resource que de fichiers.
            @Value("classpath:docs/*.md") Resource[] sources) {

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        if (CACHE.exists()) {
            // Vecteurs déjà calculés lors d'un démarrage précédent :
            // rechargement instantané, zéro appel au modèle d'embeddings.
            vectorStore.load(CACHE);
            return vectorStore;
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        for (Resource source : sources) {
            // Extract : un Document par fichier (texte brut + métadonnées,
            // dont le nom du fichier source — utile pour citer ses sources).
            List<Document> fileDocs = new TextReader(source).get();
            // Transform + Load : découpage en chunks, puis chaque chunk
            // est embeddé et stocké.
            vectorStore.add(splitter.apply(fileDocs));
        }

        CACHE.getParentFile().mkdirs();
        vectorStore.save(CACHE);
        return vectorStore;
    }
}
