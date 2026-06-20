package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.SubGraph;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transforme un sous-graphe en texte structuré, lisible par le LLM (SPEC §5.1 étape 4).
 * Pas d'IA ici : pure mise en forme du contexte.
 */
public final class ContextBuilder {

    private ContextBuilder() {}

    public static String from(SubGraph sub) {
        if (sub == null || sub.isEmpty()) {
            return "(aucun élément pertinent trouvé sur la carte)";
        }
        Map<NodeId, String> labels = sub.nodes().stream()
                .collect(Collectors.toMap(Node::id, ContextBuilder::describe, (a, b) -> a));

        String nodes = sub.nodes().stream()
                .map(ContextBuilder::describe)
                .collect(Collectors.joining("\n- ", "- ", ""));

        Function<NodeId, String> name = id -> labels.getOrDefault(id, id.value());
        String edges = sub.edges().stream()
                .map(e -> "  " + name.apply(e.source()) + " " + verb(e) + " " + name.apply(e.target()))
                .collect(Collectors.joining("\n"));

        return """
                Nœuds présents :
                %s

                Liens :
                %s
                """.formatted(nodes, edges.isBlank() ? "  (aucun)" : edges);
    }

    private static String describe(Node n) {
        return switch (n.type()) {
            case EMOTION -> "émotion « " + n.label() + " »";
            case SITUATION -> "situation « " + n.label() + " »";
            case BELIEF -> "croyance « " + n.label() + " »";
            case SENSATION -> "sensation « " + n.label() + " »";
            case NEED -> "besoin « " + n.label() + " »";
            case RESOURCE -> "ressource apaisante « " + n.label() + " »";
            case PERSON -> "personne « " + n.label() + " »";
            case ENTRY -> "entrée de journal";
        };
    }

    private static String verb(Edge e) {
        return switch (e.type()) {
            case TRIGGERS -> "déclenche";
            case FED_BY -> "est nourrie par";
            case EXPRESSED_AS -> "s'exprime par";
            case TOUCHES -> "touche au besoin";
            case SOOTHES -> "apaise";
            case MENTIONS -> "mentionne";
        };
    }
}
