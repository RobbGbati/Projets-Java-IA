package com.misterbil.racines.domain.port.out;

/**
 * Génère une réponse en langage naturel (phase 2). Impl : Spring AI.
 *
 * @param system  le prompt système (ton, garde-fous — SYSTEM_DOUX)
 * @param context le contexte issu du sous-graphe (texte structuré)
 * @param question la question de l'utilisateur
 */
public interface ChatPort {
    String generate(String system, String context, String question);
}
