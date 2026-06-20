package com.misterbil.racines.domain.model;

import java.util.List;

/**
 * Schéma contraint passé à l'extracteur LLM (phase 3) : la liste des types de
 * nœuds et de relations autorisés. Borne ce que le modèle peut proposer →
 * extraction prévisible, pas de label fantaisiste.
 */
public record GraphSchema(List<NodeType> nodeTypes, List<EdgeType> edgeTypes) {

    /** Schéma par défaut = tout le modèle des Racines. */
    public static GraphSchema racinesDefault() {
        return new GraphSchema(List.of(NodeType.values()), List.of(EdgeType.values()));
    }
}
