package com.misterbil.racines.adapter.out.ai;

import lombok.RequiredArgsConstructor;
import com.misterbil.racines.domain.port.out.ChatPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Adaptateur ChatPort via Spring AI. C'est le SEUL endroit où le contexte
 * (sous-graphe) sort vers le LLM — point de contrôle unique de la vie privée
 * (SPEC §9).
 *
 * <p>Modèle résolu paresseusement : si aucun {@code ChatModel} n'est configuré
 * ({@code spring.ai.model.chat=none}, le défaut), renvoie un message de repli
 * doux au lieu de planter.</p>
 */
@Component
@RequiredArgsConstructor
public class SpringAiChat implements ChatPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChat.class);
    private static final String FALLBACK =
            "Je n'arrive pas à interroger ta carte pour l'instant. "
            + "Reviens dans un moment — tes racines, elles, restent là.";

    private final ObjectProvider<ChatModel> models;
    @Override
    public String generate(String system, String context, String question) {
        ChatModel model = models.getIfAvailable();
        if (model == null) return FALLBACK;   // IA désactivée
        // Trace le fournisseur réellement utilisé pour ce chat (AnthropicChatModel,
        // OllamaChatModel, OpenAiChatModel…) → on sait quel LLM répond à chaque appel.
        log.info("Chat LLM : {}", model.getClass().getSimpleName());
        try {
            return ChatClient.create(model).prompt()
                    .system(system)
                    .user("""
                            Voici ce que dit la carte (à n'utiliser que pour répondre) :
                            %s

                            La question : %s
                            """.formatted(context, question))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Chat indisponible ({}) — réponse de repli", e.getMessage());
            return FALLBACK;
        }
    }
}
