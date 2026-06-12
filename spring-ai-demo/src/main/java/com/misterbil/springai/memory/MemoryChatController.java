package com.misterbil.springai.memory;

import com.misterbil.springai.dtos.ChatAnswer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EXTENSION 9.2 — Mémoire de conversation : un chat multi-tours.
 *
 * POST /chat/memory  { "conversationId": "...", "message": "..." }
 *                  →  { "answer": "..." }
 *
 * ─── Rappel du problème (section 4 du README) ──────────────────────────
 * Le LLM est SANS ÉTAT : chaque appel HTTP repart de zéro. "Quel est mon
 * prénom ?" après "Je m'appelle Bil" → il ne sait pas. La mémoire vit
 * forcément CÔTÉ APPLICATION : c'est à nous de renvoyer l'historique à
 * chaque appel.
 *
 * ─── Ce que fait MessageChatMemoryAdvisor à chaque requête ─────────────
 *  1. AVANT l'appel : charge l'historique de la conversation depuis le
 *     ChatMemory et l'insère dans le prompt (messages user/assistant
 *     précédents, à la suite du system prompt).
 *  2. APRÈS l'appel : enregistre le nouveau message user + la réponse du
 *     modèle dans le ChatMemory.
 * Un advisor est un INTERCEPTEUR autour de l'appel au modèle — le même
 * rôle qu'un filtre servlet ou un aspect AOP. SimpleLoggerAdvisor (vu
 * dans ChatClientConfig) est bâti sur le même mécanisme.
 *
 * ─── D'où sort le bean ChatMemory ? ────────────────────────────────────
 * Auto-configuré par Spring AI : un MessageWindowChatMemory (fenêtre des
 * N derniers messages, 20 par défaut) sur un InMemoryChatMemoryRepository.
 * "Fenêtre", car on ne peut pas renvoyer l'historique infini : chaque
 * message ajouté = des tokens facturés et un contexte limité. En prod,
 * on brancherait un repository JDBC — même interface, autre adapter.
 *
 * Le conversationId isole les sessions : deux ids = deux historiques
 * étanches. C'est l'équivalent du cookie de session HTTP.
 *
 * ─── Pourquoi l'advisor est passé PAR REQUÊTE et pas dans le bean ──────
 * S'il était déclaré en defaultAdvisors dans ChatClientConfig, TOUS les
 * endpoints deviendraient à mémoire — y compris /analyze, où mélanger
 * les avis clients successifs serait un bug. L'advisor ne s'applique
 * qu'ici : l'existant reste sans état.
 *
 * Test :
 *   curl -X POST http://localhost:8080/chat/memory \
 *     -H "Content-Type: application/json" \
 *     -d '{"conversationId": "demo-1", "message": "Je m'\''appelle Bil."}'
 *   curl -X POST http://localhost:8080/chat/memory \
 *     -H "Content-Type: application/json" \
 *     -d '{"conversationId": "demo-1", "message": "Quel est mon prénom ?"}'
 *   → il répond "Bil". Refais le 2e appel avec "demo-2" : il ne sait plus.
 */
@RestController
@RequestMapping("/chat/memory")
public class MemoryChatController {

    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public MemoryChatController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        // Construit une fois : l'advisor est sans état, c'est le
        // ChatMemory derrière qui porte les historiques.
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @PostMapping
    public ChatAnswer chat(@RequestBody MemoryChatRequest request) {
        String answer = chatClient
                .prompt()
                .user(request.message())
                // Active la mémoire pour CET appel...
                .advisors(memoryAdvisor)
                // ...et dit à l'advisor QUELLE conversation charger.
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.conversationId()))
                .call()
                .content();

        return new ChatAnswer(answer);
    }

    public record MemoryChatRequest(String conversationId, String message) {}
}
