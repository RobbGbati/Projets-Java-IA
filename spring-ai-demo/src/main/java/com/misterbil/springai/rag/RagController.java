package com.misterbil.springai.rag;

import com.misterbil.springai.dtos.ChatAnswer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EXTENSION 9.3 — RAG, partie REQUÊTE : répondre à partir de NOS données.
 *
 * POST /rag  { "question": "..." }  →  { "answer": "..." }
 *
 * ─── Ce que fait QuestionAnswerAdvisor à chaque appel ───────────────────
 *  1. Calcule l'embedding de la QUESTION (même modèle que l'ingestion —
 *     indispensable, sinon les vecteurs ne sont pas comparables).
 *  2. Recherche de similarité dans le vector store → les documents les
 *     plus proches sémantiquement.
 *  3. Injecte ces documents dans le prompt avec une consigne du type
 *     "réponds à partir du contexte ci-dessous".
 *  4. Appel normal au LLM, qui répond en s'appuyant sur ce contexte.
 *
 * Encore un advisor : le RAG complet tient dans un intercepteur, et le
 * code du contrôleur reste identique à un chat simple. Active les logs
 * DEBUG pour voir le prompt enrichi — les documents TechNova y sont,
 * en clair.
 *
 * Même logique que pour la mémoire : advisor passé PAR REQUÊTE, le RAG
 * ne s'applique qu'à cet endpoint.
 *
 * ─── Le contrôleur ne sait pas D'OÙ viennent les documents ──────────────
 * Il dépend de l'INTERFACE VectorStore, pas d'une implémentation. Selon
 * le profil Spring actif, Spring injecte ici :
 *   - profil par défaut : un SimpleVectorStore en mémoire (RagConfig),
 *     alimenté au démarrage depuis les fichiers du classpath ;
 *   - profil "pgvector" : un PgVectorStore branché sur la base
 *     PostgreSQL alimentée par le projet ragbatch.
 * Pas une ligne à changer ici pour passer de l'un à l'autre : c'est le
 * même pattern port/adapter que ChatModel (OpenAI/Ollama). Lance avec
 *   mvn spring-boot:run -Dspring-boot.run.profiles=pgvector
 * (la base ragbatch doit tourner — voir ragbatch/README.md).
 *
 * Test (fait inventé, donc réponse impossible sans le vector store) :
 *   curl -X POST http://localhost:8080/rag \
 *     -H "Content-Type: application/json" \
 *     -d '{"question": "Combien de jours ai-je pour retourner un produit TechNova ?"}'
 *   → "45 jours" (avec le code RETOUR-45). Pose ensuite une question
 *     hors corpus pour voir le modèle dire qu'il ne sait pas.
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor ragAdvisor;

    public RagController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
    }

    @PostMapping
    public ChatAnswer ask(@RequestBody RagQuestion question) {
        String answer = chatClient
                .prompt()
                .user(question.question())
                .advisors(ragAdvisor)   // toute la mécanique RAG est là
                .call()
                .content();

        return new ChatAnswer(answer);
    }

    public record RagQuestion(String question) {}
}
