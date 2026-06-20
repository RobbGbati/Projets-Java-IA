package com.misterbil.racines.domain.port.out;

/**
 * Calcule l'embedding d'un texte (phase 2). Impl : Spring AI.
 * Doit renvoyer un tableau vide (jamais null) si le service est indisponible,
 * pour que les phases 0/1 fonctionnent hors-ligne sans clé LLM.
 */
public interface EmbeddingPort {
    float[] embed(String text);
}
