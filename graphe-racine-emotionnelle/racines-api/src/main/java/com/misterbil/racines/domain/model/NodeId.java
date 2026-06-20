package com.misterbil.racines.domain.model;

import java.util.UUID;

/**
 * Identité d'un nœud. Un simple wrapper typé autour d'un String : impossible
 * de confondre un id de nœud avec une autre chaîne, et le code se lit mieux.
 */
public record NodeId(String value) {

    public NodeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NodeId vide");
        }
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }

    /** Nouvel id aléatoire (création de nœud). */
    public static NodeId newId() {
        return new NodeId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
