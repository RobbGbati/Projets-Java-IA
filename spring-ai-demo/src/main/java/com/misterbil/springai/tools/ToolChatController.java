package com.misterbil.springai.tools;

import com.misterbil.springai.dtos.ChatAnswer;
import com.misterbil.springai.dtos.ChatQuestion;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EXTENSION 9.4 — Tool calling : le LLM décide d'appeler ton code Java.
 *
 * POST /chat/tools  { "message": "..." }  →  { "answer": "..." }
 *
 * ─── La boucle tool-calling, étape par étape ────────────────────────────
 *
 *  1. Spring AI envoie le prompt + les descriptions JSON des outils
 *     (générées depuis les annotations @Tool de TimeTools).
 *  2. Le LLM répond soit par du texte (outil inutile), soit par une
 *     DEMANDE D'APPEL : "appelle getCurrentDateTime avec ces arguments".
 *     Le LLM n'exécute JAMAIS rien : il émet une intention en JSON.
 *  3. Spring AI intercepte cette demande, invoque la VRAIE méthode Java
 *     dans TA JVM, et renvoie le résultat au LLM dans un nouvel appel.
 *  4. Le LLM rédige sa réponse finale avec cette information.
 *
 * Donc : minimum DEUX allers-retours vers le modèle quand un outil est
 * utilisé. Toute la boucle (étapes 2-3-4) est gérée par .tools() —
 * invisible ici, mais visible dans les logs DEBUG.
 *
 * C'est LE mécanisme derrière tous les "agents" : un agent n'est qu'un
 * LLM en boucle avec des outils. Et l'implication sécurité est directe :
 * le LLM décide d'appeler TON code avec des arguments qu'IL choisit.
 * Des outils en lecture seule comme ici = sans risque ; un outil
 * "supprimerCommande" exigerait validation et contrôle d'accès.
 *
 * Tests :
 *   curl -X POST http://localhost:8080/chat/tools \
 *     -H "Content-Type: application/json" \
 *     -d '{"message": "Quelle heure est-il, précisément ?"}'
 *   → l'heure réelle : impossible sans l'outil. Compare avec /chat, qui
 *     n'a pas l'outil. Puis : "Quelle date serons-nous dans 45 jours ?"
 *     → le LLM choisit l'AUTRE outil, seul, d'après les descriptions.
 */
@RestController
@RequestMapping("/chat/tools")
public class ToolChatController {

    private final ChatClient chatClient;

    public ToolChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ChatAnswer chat(@RequestBody ChatQuestion question) {
        String answer = chatClient
                .prompt()
                .user(question.message())
                /*
                 * Expose les méthodes @Tool de TimeTools pour CET appel.
                 * Même philosophie que les advisors : capacité accordée
                 * par requête, les autres endpoints n'y ont pas accès.
                 */
                .tools(new TimeTools())
                .call()
                .content();

        return new ChatAnswer(answer);
    }
}
