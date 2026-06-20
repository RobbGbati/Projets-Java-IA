package com.misterbil.racines.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Le graphe intérieur complet d'un utilisateur (tous les nœuds + arêtes).
 * Renvoyé par {@code GetGraph.full()} et {@code DepositEntry.deposit(...)}.
 */
public record InnerGraph(List<Node> nodes, List<Edge> edges) {

    public InnerGraph {
        nodes = (nodes == null) ? List.of() : List.copyOf(nodes);
        edges = (edges == null) ? List.of() : List.copyOf(edges);
    }

    public static InnerGraph empty() {
        return new InnerGraph(List.of(), List.of());
    }

    public Optional<Node> node(NodeId id) {
        return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    public List<Node> nodesOfType(NodeType type) {
        return nodes.stream().filter(n -> n.type() == type).toList();
    }
}
