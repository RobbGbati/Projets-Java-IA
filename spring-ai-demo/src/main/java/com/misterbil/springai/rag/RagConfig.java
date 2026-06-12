package com.misterbil.springai.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EXTENSION 9.3 — RAG, partie INGESTION : remplir le vector store.
 *
 * ─── Le problème que résout le RAG ──────────────────────────────────────
 * Le LLM ne connaît que ses données d'entraînement. Il ne sait RIEN de
 * ton entreprise : tes procédures, tes produits, tes docs internes.
 * Demande-lui la politique de retour de "TechNova" → il invente.
 *
 * RAG = Retrieval Augmented Generation : avant de répondre, on RECHERCHE
 * les documents pertinents dans NOS données et on les colle dans le
 * prompt. Le LLM ne "sait" toujours rien : on lui souffle la réponse.
 *
 * ─── Les embeddings, la brique qui rend la recherche possible ───────────
 * Un modèle d'embeddings (ici nomic-embed-text, distinct du modèle de
 * chat) transforme un texte en VECTEUR de nombres qui capture son SENS.
 * Deux textes proches sémantiquement → vecteurs proches géométriquement.
 * "Recherche de similarité" = trouver les vecteurs les plus proches de
 * celui de la question. C'est pour ça qu'une recherche RAG trouve
 * "remboursement sous 30 jours" quand on demande "puis-je rendre un
 * produit ?" — là où un LIKE SQL ne trouverait rien.
 *
 * ─── SimpleVectorStore ──────────────────────────────────────────────────
 * Implémentation EN MÉMOIRE de l'interface VectorStore : une Map + un
 * calcul de similarité cosinus. Zéro infra, idéal pour apprendre ; tout
 * est perdu au redémarrage. En prod : PGVector, Chroma, etc. — même
 * interface, autre adapter (toujours le pattern de la section 3.3).
 *
 * Les documents ci-dessous sont des faits INVENTÉS sur une entreprise
 * fictive : c'est volontaire. Si le LLM répond juste, la réponse ne peut
 * venir QUE du vector store, pas de son entraînement. Preuve par
 * l'absurde que le RAG fonctionne.
 */
@Configuration
public class RagConfig {

    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        /*
         * INGESTION : chaque Document est passé au modèle d'embeddings,
         * et le couple (texte, vecteur) est stocké. Dans un vrai projet,
         * cette étape est un pipeline séparé (lecture PDF/HTML, découpage
         * en morceaux — "chunking" —, puis .add()) exécuté hors ligne,
         * pas à chaque démarrage.
         */
        vectorStore.add(List.of(
                new Document("""
                        Politique de retour TechNova : les clients peuvent retourner
                        tout produit sous 45 jours avec le code RETOUR-45. Les frais
                        de retour sont offerts à partir de 50 € d'achat."""),
                new Document("""
                        Le support technique TechNova est ouvert du lundi au samedi,
                        de 8h à 19h. Contact prioritaire pour les pannes bloquantes :
                        urgence@technova.example (réponse sous 2 heures)."""),
                new Document("""
                        La garantie constructeur TechNova dure 3 ans sur les produits
                        de la gamme Pro et 1 an sur la gamme Essentielle. L'extension
                        de garantie PremiumCare ajoute 2 ans supplémentaires."""),
                new Document("""
                        Programme de fidélité TechNova : 1 point par euro dépensé.
                        À partir de 500 points, le statut Gold offre la livraison
                        express gratuite et un accès anticipé aux ventes privées.""")
        ));

        return vectorStore;
    }
}
