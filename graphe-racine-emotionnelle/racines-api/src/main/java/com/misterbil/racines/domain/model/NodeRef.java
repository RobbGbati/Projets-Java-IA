package com.misterbil.racines.domain.model;

/**
 * Référence légère vers un nœud-ancre trouvé par similarité vectorielle,
 * avec son score (phase 2). Point de départ d'une traversée.
 */
public record NodeRef(NodeId id, NodeType type, String label, double score) {
}
