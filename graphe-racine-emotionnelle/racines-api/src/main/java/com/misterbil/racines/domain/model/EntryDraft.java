package com.misterbil.racines.domain.model;

/**
 * Saisie STRUCTURÉE (phase 1) : l'utilisateur choisit lui-même les éléments,
 * pas d'IA. Seuls {@code rawText} et au moins une émotion ou situation sont
 * attendus ; les autres champs sont optionnels (null = absent).
 *
 * <p>La phase 3 (extraction libre) produira, elle, une {@link ExtractionProposal}.</p>
 */
public record EntryDraft(
        String rawText,     // le texte confié
        String sky,         // l'humeur du jour (« ciel »)
        String emotion,
        String situation,
        String belief,
        String sensation,
        String need,
        String resource,
        String person       // alias anonymisé (« P. ») — jamais de nom réel
) {
    public boolean has(String s) {
        return s != null && !s.isBlank();
    }
}
