package com.misterbil.racines.domain.model;

import java.util.List;

/**
 * Une portion du graphe ramenée pour répondre à une question (PRD §12).
 * Distinct d'{@link InnerGraph} par l'intention : un sous-graphe est un
 * extrait contextuel, pas la carte entière. Même forme, sémantique différente.
 */
public record SubGraph(List<Node> nodes, List<Edge> edges) {

    public SubGraph {
        nodes = (nodes == null) ? List.of() : List.copyOf(nodes);
        edges = (edges == null) ? List.of() : List.copyOf(edges);
    }

    public static SubGraph empty() {
        return new SubGraph(List.of(), List.of());
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
