package com.misterbil.racines.domain.model;

/** Une arête typée entre deux nœuds. */
public record Edge(String id, EdgeType type, NodeId source, NodeId target) {

    public Edge {
        if (type == null) throw new IllegalArgumentException("Edge.type null");
        if (source == null || target == null) throw new IllegalArgumentException("Edge: source/target null");
        if (id == null || id.isBlank()) id = source.value() + "-" + type + "-" + target.value();
    }

    public static Edge of(EdgeType type, NodeId source, NodeId target) {
        return new Edge(null, type, source, target);
    }
}
