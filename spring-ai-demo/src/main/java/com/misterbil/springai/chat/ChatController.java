package com.misterbil.springai.chat;

import com.misterbil.springai.dtos.ChatAnswer;
import com.misterbil.springai.dtos.ChatQuestion;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENDPOINT 1 — Le "hello world" de l'IA backend.
 *
 * POST /chat  { "message": "..." }  →  { "answer": "..." }
 *
 * ─── Ce qui se passe RÉELLEMENT quand on appelle .call() ──────────────
 *
 *  1. ChatClient assemble un Prompt : le system prompt par défaut
 *     (défini dans ChatClientConfig) + le message utilisateur.
 *  2. Le ChatModel actif traduit ce Prompt au format JSON du fournisseur
 *     (pour OpenAI : POST https://api.openai.com/v1/chat/completions avec
 *     un tableau "messages" [{role:system,...},{role:user,...}]).
 *  3. Requête HTTP sortante, avec ta clé API en header Authorization,
 *     gestion des erreurs et du retry incluses.
 *  4. La réponse JSON du fournisseur est désérialisée en ChatResponse
 *     (texte généré + métadonnées : tokens consommés, raison d'arrêt...).
 *  5. .content() extrait juste le texte. C'est un raccourci : on pourrait
 *     appeler .chatResponse() pour accéder aux métadonnées complètes.
 *
 * Spring AI abstrait les étapes 2 à 4. Le code ne contient ni URL, ni
 * clé API, ni parsing JSON : uniquement l'intention.
 *
 * ─── Point important : chaque appel est SANS MÉMOIRE ───────────────────
 * Le LLM ne se souvient de rien entre deux requêtes. Si on veut une
 * conversation suivie, c'est à nous de renvoyer l'historique (Spring AI
 * propose des "ChatMemory advisors" pour ça — hors périmètre ici, mais
 * voici le principe : l'état vit côté application, jamais côté modèle).
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    /*
     * Injection par constructeur — toujours.
     * Le contrôleur déclare ce dont il a besoin (un ChatClient) et le
     * reçoit. Il ne sait pas si derrière c'est OpenAI, Ollama ou un mock
     * de test. Dépendre de l'abstraction, pas de l'implémentation.
     */
    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ChatAnswer chat(@RequestBody ChatQuestion question) {
        String answer = chatClient
                .prompt()                       // démarre la construction du prompt
                .user(question.message())       // ajoute le message avec role "user"
                .call()                         // appel BLOQUANT au LLM (il existe .stream() pour du SSE)
                .content();                     // extrait le texte de la réponse

        return new ChatAnswer(answer);
    }
}
