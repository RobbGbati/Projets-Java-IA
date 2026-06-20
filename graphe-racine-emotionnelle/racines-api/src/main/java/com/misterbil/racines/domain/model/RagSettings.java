package com.misterbil.racines.domain.model;

/**
 * Réglages du pipeline GraphRAG : combien d'ancres ({@code topK}) et jusqu'où
 * traverser ({@code maxHops}). Valeurs injectées depuis application.yml.
 */
public record RagSettings(int topK, int maxHops) {

    public RagSettings {
        if (topK < 1) topK = 1;
        if (maxHops < 1) maxHops = 1;
    }
}
