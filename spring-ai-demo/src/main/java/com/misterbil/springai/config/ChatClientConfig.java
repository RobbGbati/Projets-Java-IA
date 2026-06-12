package com.misterbil.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration centrale du ChatClient.
 *
 * ─── La pile d'abstractions de Spring AI, de bas en haut ───────────────
 *
 *  ChatModel        → interface bas niveau : "envoie un Prompt, reçois une
 *                     ChatResponse". C'est le PORT. OpenAiChatModel et
 *                     OllamaChatModel en sont les ADAPTERS. L'auto-config
 *                     en crée un selon spring.ai.model.chat.
 *
 *  ChatClient       → API fluide de haut niveau construite AU-DESSUS du
 *                     ChatModel. C'est elle que tu utilises au quotidien.
 *                     Analogie directe : ChatModel est à ChatClient ce que
 *                     le DataSource est au JdbcClient. Tu pourrais parler
 *                     au DataSource directement, mais tu ne le fais jamais.
 *
 *  ChatClient.Builder → fourni automatiquement par l'auto-configuration,
 *                     déjà branché sur le ChatModel actif. On s'en sert
 *                     ici pour fabriquer NOTRE bean ChatClient avec des
 *                     valeurs par défaut communes à toute l'application.
 *
 * ─── Pourquoi déclarer le bean ici plutôt que dans chaque contrôleur ? ──
 * Pour la même raison qu'on centralise un RestClient ou un ObjectMapper :
 * un seul endroit où définir le comportement par défaut (system prompt,
 * advisors, options). Les contrôleurs restent focalisés sur leur métier.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                /*
                 * Le SYSTEM PROMPT : instruction permanente envoyée au LLM
                 * avant chaque message utilisateur. C'est le "contrat de
                 * comportement" du modèle. Dans l'API du fournisseur, il
                 * part dans le tableau messages avec role: "system" —
                 * Spring AI construit ce JSON pour toi.
                 */
                .defaultSystem("""
                        Tu es un assistant intégré dans une application backend Java.
                        Tu réponds en français, de manière concise et précise.
                        Si tu ne sais pas, tu le dis explicitement.
                        """)
                /*
                 * SimpleLoggerAdvisor : intercepte chaque appel au LLM et
                 * logge le prompt complet (system + user) et la réponse,
                 * au niveau DEBUG. C'est lui qui produit les logs — le
                 * niveau DEBUG dans application.yml ne suffit pas seul.
                 */
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
