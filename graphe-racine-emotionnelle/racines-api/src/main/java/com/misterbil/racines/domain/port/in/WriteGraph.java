package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeType;

import java.util.Map;

/**
 * Écriture directe d'un nœud / d'une arête (phase 0 : POST /api/nodes, /api/edges).
 * Sert à peupler le graphe à la main avant toute IA.
 */
public interface WriteGraph {
    Node addNode(NodeType type, String label, Map<String, Object> extra);

    Edge addEdge(EdgeType type, NodeId source, NodeId target);
}
