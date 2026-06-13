package com.misterbil.ragbatch.batch;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.Resource;

/**
 * PROCESSOR — l'étape "Transform" du pipeline, version Spring Batch.
 *
 * Entrée  : un fichier (Resource)
 * Sortie  : ses chunks prêts à être embeddés (List<Document>)
 *
 * C'est exactement le même travail que dans le RagConfig de
 * spring-ai-demo, mais découpé selon le contrat ItemProcessor de
 * Spring Batch : une méthode PURE, un item entre, un item sort.
 * Le framework s'occupe du reste (itération, transactions, reprise).
 *
 * Rappel sur le CHUNKING (l'étape critique du RAG) : on ne vectorise
 * jamais un fichier entier. Son embedding moyennerait tous les sujets
 * qu'il contient (sens dilué → recherche imprécise), et le retrouver
 * entier ferait exploser le prompt. TokenTextSplitter découpe en
 * morceaux de ~800 tokens : c'est le CHUNK qui est vectorisé, retrouvé
 * par similarité, puis injecté dans le prompt côté application.
 *
 * Chaque Document porte des métadonnées (dont le fichier source, ajouté
 * par TextReader) : elles atterrissent dans la colonne metadata (JSON)
 * de la table vector_store — de quoi citer ses sources ou filtrer.
 *
 * Pour d'autres formats, seul le reader interne change :
 * PagePdfDocumentReader (dép. spring-ai-pdf-document-reader) pour du
 * PDF, TikaDocumentReader (spring-ai-tika-document-reader) pour
 * Word/HTML. La signature du processor, elle, ne bouge pas.
 */
public class FileToChunksProcessor implements ItemProcessor<Resource, List<Document>> {

    private final TokenTextSplitter splitter = new TokenTextSplitter();

    @Override
    public List<Document> process(Resource file) {
        List<Document> wholeFile;
        try {
            wholeFile = new TextReader(file).get();              // Extract
        } catch (RuntimeException e) {
            // Traduction en exception typée : c'est elle que le step
            // déclare skippable (.skip(UnreadableSourceException.class)).
            // Un fichier corrompu ne doit pas condamner tout le corpus.
            throw new UnreadableSourceException(file.getFilename(), e);
        }
        return splitter.apply(wholeFile);                        // Transform
    }
}
