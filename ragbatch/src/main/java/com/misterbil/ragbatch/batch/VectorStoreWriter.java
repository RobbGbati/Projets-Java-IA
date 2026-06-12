package com.misterbil.ragbatch.batch;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * WRITER — l'étape "Load" : embeddings + insertion en base.
 *
 * vectorStore.add() fait DEUX choses pour chaque Document :
 *  1. appelle le modèle d'embeddings (nomic-embed-text via Ollama) pour
 *     calculer le vecteur du chunk — c'est ICI que le coût se paie,
 *     un appel modèle par chunk, et c'est pour ça qu'on ingère dans un
 *     batch hors ligne plutôt qu'au démarrage de l'application ;
 *  2. insère la ligne (id, content, metadata, embedding) dans la table
 *     vector_store de PostgreSQL.
 *
 * Le type VectorStore est l'INTERFACE : ce writer ne sait pas qu'il
 * écrit dans PGVector. Remplacer la dépendance pgvector par Chroma ou
 * Qdrant ne changerait pas une ligne ici — même pattern port/adapter
 * que ChatModel dans spring-ai-demo.
 *
 * Le Chunk reçu est le "commit interval" de Spring Batch (chunk(N) dans
 * la définition du step) : les N items écrits dans la même transaction.
 * À ne pas confondre avec les chunks de TEXTE du RAG — collision de
 * vocabulaire malheureuse mais standard.
 */
public class VectorStoreWriter implements ItemWriter<List<Document>> {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreWriter.class);

    private final VectorStore vectorStore;

    public VectorStoreWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void write(Chunk<? extends List<Document>> items) {
        for (List<Document> fileChunks : items) {
            vectorStore.add(fileChunks);
            log.info("Ingéré {} chunks (source : {})", fileChunks.size(),
                    fileChunks.isEmpty() ? "?" : fileChunks.getFirst().getMetadata().get("source"));
        }
    }
}
