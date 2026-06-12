package com.misterbil.springai.streaming;

import com.misterbil.springai.dtos.ChatQuestion;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * EXTENSION 9.1 — Streaming : la réponse arrive token par token.
 *
 * POST /chat/stream  { "message": "..." }  →  flux SSE de fragments de texte
 *
 * ─── .call() vs .stream() ──────────────────────────────────────────────
 *
 * .call()   → BLOQUANT. Le serveur attend que le LLM ait généré TOUTE la
 *             réponse avant de renvoyer quoi que ce soit. Pour une réponse
 *             longue, l'utilisateur fixe un spinner pendant 10 secondes.
 *
 * .stream() → renvoie un Flux<String> (Reactor). Chaque élément du flux
 *             est un fragment (à peu près un token) émis par le LLM dès
 *             qu'il est généré. C'est exactement l'effet "machine à
 *             écrire" de l'interface de ChatGPT.
 *
 * Côté HTTP, le fournisseur expose ça en Server-Sent Events (SSE) : une
 * connexion qui reste ouverte et pousse des événements `data:` au fil de
 * l'eau. Spring AI consomme ce SSE entrant... et Spring MVC sait re-servir
 * un Flux en SSE sortant (produces = text/event-stream) — pas besoin de
 * WebFlux, reactor-core est déjà là via Spring AI.
 *
 * Le contrôleur ne fait que BRANCHER les deux flux : aucun fragment n'est
 * bufferisé côté serveur.
 *
 * Test (le -N de curl désactive son buffering, sinon tu ne vois rien
 * avant la fin) :
 *   curl -N -X POST http://localhost:8080/chat/stream \
 *     -H "Content-Type: application/json" \
 *     -d '{"message": "Compte de 1 à 10 en toutes lettres."}'
 */
@RestController
@RequestMapping("/chat/stream")
public class StreamingChatController {

    private final ChatClient chatClient;

    public StreamingChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatQuestion question) {
        return chatClient
                .prompt()
                .user(question.message())
                .stream()       // au lieu de .call() : tout est là
                .content();     // Flux<String> : un fragment par élément
    }
}
