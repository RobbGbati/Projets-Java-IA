package com.misterbil.racines.domain.model;

import java.util.List;

/**
 * Un changement à appliquer au store : nœuds et arêtes à créer/fusionner.
 * Le store fait un MERGE (idempotent par id), jamais un doublon aveugle.
 */
public record GraphChange(List<Node> nodesToUpsert, List<Edge> edgesToUpsert) {

    public GraphChange {
        nodesToUpsert = (nodesToUpsert == null) ? List.of() : List.copyOf(nodesToUpsert);
        edgesToUpsert = (edgesToUpsert == null) ? List.of() : List.copyOf(edgesToUpsert);
    }

    public static GraphChange of(List<Node> nodes, List<Edge> edges) {
        return new GraphChange(nodes, edges);
    }

    public boolean isEmpty() {
        return nodesToUpsert.isEmpty() && edgesToUpsert.isEmpty();
    }
}
