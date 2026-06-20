package com.misterbil.racines.adapter.out.ai;

import com.misterbil.racines.domain.port.out.EmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Adaptateur EmbeddingPort via Spring AI (OpenAI ou Ollama selon la config).
 *
 * <p>Le modèle est résolu PARESSEUSEMENT via {@link ObjectProvider} : si aucun
 * {@code EmbeddingModel} n'est configuré ({@code spring.ai.model.embedding=none},
 * le défaut), on renvoie un tableau VIDE au lieu de planter. Ainsi les phases
 * 0/1 démarrent hors-ligne ; seule la recherche d'ancres (phase 2) reste muette.</p>
 */
@Component
public class SpringAiEmbedding implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiEmbedding.class);
    private static final float[] EMPTY = new float[0];

    private final ObjectProvider<EmbeddingModel> models;

    public SpringAiEmbedding(ObjectProvider<EmbeddingModel> models) {
        this.models = models;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return EMPTY;
        EmbeddingModel model = models.getIfAvailable();
        if (model == null) return EMPTY;   // IA désactivée → pas de vecteur
        try {
            return model.embed(text);
        } catch (Exception e) {
            log.warn("Embedding indisponible ({}) — on continue sans vecteur", e.getMessage());
            return EMPTY;
        }
    }
}
