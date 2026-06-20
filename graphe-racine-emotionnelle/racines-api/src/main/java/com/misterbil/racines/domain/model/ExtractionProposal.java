package com.misterbil.racines.domain.model;

import java.util.List;

/**
 * Proposition d'extraction (phase 3) : ce que le LLM a REPÉRÉ dans un texte
 * libre, AVANT toute persistance. L'utilisateur valide / corrige / refuse
 * (consentement, PRD §3.4). Les ids sont temporaires jusqu'à la confirmation.
 */
public record ExtractionProposal(List<Node> nodes, List<Edge> edges) {

    public ExtractionProposal {
        nodes = (nodes == null) ? List.of() : List.copyOf(nodes);
        edges = (edges == null) ? List.of() : List.copyOf(edges);
    }

    public static ExtractionProposal empty() {
        return new ExtractionProposal(List.of(), List.of());
    }
}
