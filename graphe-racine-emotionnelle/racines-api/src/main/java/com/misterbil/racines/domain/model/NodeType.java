package com.misterbil.racines.domain.model;

/**
 * Les types de nœuds de la vie intérieure (PRD §5, SPEC §4).
 * Le libellé Neo4j est le nom de la constante (EMOTION → :Emotion à l'affichage).
 */
public enum NodeType {
    EMOTION,    // peur, colère, honte, joie…
    SITUATION,  // un déclencheur (« une réunion »)
    BELIEF,     // pensée/croyance (« je ne suis pas à la hauteur »)
    SENSATION,  // corporelle (« boule au ventre »)
    NEED,       // sécurité, reconnaissance, repos…
    RESOURCE,   // ce qui apaise (respiration, marche, un proche)
    PERSON,     // anonymisée (« P. ») — jamais de nom réel
    ENTRY;      // le texte source daté + l'humeur

    /**
     * Nœuds « parlants » porteurs d'un embedding et indexés pour la recherche
     * d'ancres (phase 2). Les autres n'ont pas besoin de vecteur.
     */
    public boolean isEmbeddable() {
        return this == EMOTION || this == SITUATION || this == BELIEF || this == RESOURCE;
    }
}
