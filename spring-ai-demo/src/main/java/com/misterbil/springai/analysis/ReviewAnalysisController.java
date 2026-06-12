package com.misterbil.springai.analysis;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENDPOINT 2 — Sortie structurée : le LLM devient un composant TYPÉ.
 *
 * POST /analyze  { "review": "texte d'un avis client" }
 *              →  un objet ReviewAnalysis (JSON structuré et validé)
 *
 * ─── Pourquoi c'est LE concept à maîtriser après le chat simple ────────
 *
 * Un endpoint /chat renvoie du texte libre : utile pour un chatbot, mais
 * inexploitable par le reste de ton backend. Tu ne peux pas brancher de
 * la logique métier sur de la prose.
 *
 * La sortie structurée change la nature de l'intégration : le LLM cesse
 * d'être un "générateur de texte" et devient une FONCTION
 *
 *      String (non structuré)  →  ReviewAnalysis (typé)
 *
 * Et un objet typé, ton backend sait quoi en faire : le persister, le
 * filtrer, déclencher un workflow si needsSupportFollowUp est vrai, etc.
 * C'est le pattern que tu réutiliseras dans 80 % des intégrations LLM
 * en entreprise : extraction, classification, routage, enrichissement.
 *
 * ─── Ce que fait .entity(ReviewAnalysis.class) sous le capot ───────────
 *
 *  1. Spring AI inspecte le record ReviewAnalysis et génère son schéma
 *     JSON (via un BeanOutputConverter).
 *  2. Il ajoute au prompt une consigne du type : "Réponds uniquement avec
 *     un JSON conforme à ce schéma, sans texte autour" + le schéma.
 *     → Oui, c'est du prompt engineering automatisé. Tu peux le vérifier
 *       en mettant les logs en DEBUG (voir application.yml).
 *  3. Le LLM répond (normalement) avec du JSON brut.
 *  4. Jackson désérialise ce JSON vers ReviewAnalysis. Si le JSON est
 *     invalide ou non conforme → exception.
 *
 * Limite : l'étape 2 est une CONSIGNE, pas une garantie absolue.
 * Les modèles récents la respectent très bien, mais en production on
 * ajoute un retry ou une validation métier derrière. Garde ce réflexe.
 */
@RestController
@RequestMapping("/analyze")
public class ReviewAnalysisController {

    /**
     * PROMPT TEMPLATE : un texte à trous, version IA du PreparedStatement.
     *
     * Les {placeholders} sont remplis à l'exécution via .param(). Deux
     * intérêts, les mêmes qu'en SQL :
     *  - séparer la STRUCTURE de l'instruction des DONNÉES variables,
     *  - ne jamais concaténer du texte utilisateur dans l'instruction
     *    (réflexe d'hygiène face à l'injection de prompt).
     */
    private static final String ANALYSIS_TEMPLATE = """
            Analyse l'avis client suivant.

            Contraintes :
            - keyPoints contient au maximum 3 éléments, formulés en français.
            - estimatedRating est un entier entre 1 et 5.
            - needsSupportFollowUp est vrai uniquement si le client décrit un
              problème concret non résolu (panne, remboursement, blocage).

            Avis client :
            {review}
            """;

    private final ChatClient chatClient;

    public ReviewAnalysisController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ReviewAnalysis analyze(@RequestBody ReviewRequest request) {
        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text(ANALYSIS_TEMPLATE)          // le template...
                        .param("review", request.review())) // ...et la donnée injectée
                /*
                 * Mode JSON natif d'Ollama : le serveur contraint le modèle
                 * à produire du JSON syntaxiquement valide. Sans ça, un
                 * petit modèle comme llama3.2 ajoute parfois de la prose ou
                 * des balises ```json``` autour de la réponse, et la
                 * désérialisation Jackson de .entity() échoue → erreur 500
                 * intermittente. C'est la limite "consigne ≠ garantie"
                 * décrite plus haut, comblée côté fournisseur.
                 * Note : cette option est spécifique à Ollama ; en repassant
                 * sur OpenAI, la retirer (OpenAI a son propre mode strict).
                 */
                .options(OllamaChatOptions.builder().format("json").build())
                .call()
                /*
                 * La ligne magique. Compare avec l'endpoint /chat :
                 *   .content()                  → String libre
                 *   .entity(ReviewAnalysis.class) → objet typé et validé
                 * Une ligne de différence, un changement de paradigme.
                 */
                .entity(ReviewAnalysis.class);
    }

    public record ReviewRequest(String review) {}
}
