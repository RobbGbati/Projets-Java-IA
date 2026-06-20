package com.misterbil.racines.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Un nœud du graphe intérieur — modèle GÉNÉRIQUE.
 *
 * <p>Plutôt qu'une classe par label (Emotion, Belief…), on porte le label dans
 * {@link #type()} et les attributs spécifiques dans {@link #extra()}
 * (ex. {@code valence} pour une émotion, {@code text} pour une croyance,
 * {@code rawText}/{@code sky} pour une entrée, {@code alias} pour une personne).
 * Ce choix garde un seul type de domaine, un seul mapping de persistance, et
 * une logique GraphRAG simple. Voir SPEC §4 (« modèle de domaine vs entité »).</p>
 *
 * <p>{@code embedding} est optionnel (null tant qu'aucun vecteur n'a été calculé).
 * Il est volontairement HORS de l'égalité de record (on identifie un nœud par son
 * id), d'où l'override de {@code equals}/{@code hashCode}.</p>
 */
public record Node(
        NodeId id,
        NodeType type,
        String label,
        Map<String, Object> extra,
        float[] embedding,
        Instant createdAt
) {
    public Node {
        if (id == null) throw new IllegalArgumentException("Node.id null");
        if (type == null) throw new IllegalArgumentException("Node.type null");
        if (label == null) label = "";
        extra = (extra == null) ? Map.of() : Map.copyOf(extra);
        if (createdAt == null) createdAt = Instant.now();
    }

    /** Fabrique simple : id auto, sans embedding. */
    public static Node create(NodeType type, String label, Map<String, Object> extra) {
        return new Node(NodeId.newId(), type, label, extra, null, Instant.now());
    }

    public static Node create(NodeType type, String label) {
        return create(type, label, Map.of());
    }

    /** Copie avec un embedding attaché (immuabilité préservée). */
    public Node withEmbedding(float[] vector) {
        return new Node(id, type, label, extra, vector, createdAt);
    }

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }

    // Identité = id seul (un nœud reste « le même » quand son embedding évolue).
    @Override
    public boolean equals(Object o) {
        return o instanceof Node n && id.equals(n.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
