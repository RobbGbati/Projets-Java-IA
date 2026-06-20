package com.misterbil.racines.adapter.out.persistence.inmemory;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeType;

import java.util.Map;

/**
 * Données semées (phase 0 : 15-20 nœuds qu'on peut afficher tout de suite).
 *
 * <p>Le jeu illustre la « racine commune » du PRD : deux situations sans rapport
 * textuel (« réunion d'équipe » et « dîner en famille ») déclenchent deux émotions
 * qui pointent vers la MÊME croyance « je ne suis pas à la hauteur ».
 * {@code GET /api/insights/common-roots} doit donc la révéler.</p>
 */
final class SeedData {

    private SeedData() {}

    static void seed(InMemoryGraphStore store) {
        // --- Situations (déclencheurs) ---
        Node reunion = Node.create(NodeType.SITUATION, "une réunion d'équipe");
        Node diner = Node.create(NodeType.SITUATION, "un dîner en famille");
        Node retard = Node.create(NodeType.SITUATION, "être en retard le matin");

        // --- Émotions ---
        Node honte = Node.create(NodeType.EMOTION, "honte", Map.of("valence", "négative"));
        Node anxiete = Node.create(NodeType.EMOTION, "anxiété", Map.of("valence", "négative"));
        Node colere = Node.create(NodeType.EMOTION, "colère", Map.of("valence", "négative"));

        // --- Croyances ---
        Node pasHauteur = Node.create(NodeType.BELIEF, "je ne suis pas à la hauteur");
        Node troublerai = Node.create(NodeType.BELIEF, "je vais déranger les autres");

        // --- Sensations ---
        Node bouleVentre = Node.create(NodeType.SENSATION, "boule au ventre");
        Node gorgeSerree = Node.create(NodeType.SENSATION, "gorge serrée");

        // --- Besoins ---
        Node reconnaissance = Node.create(NodeType.NEED, "reconnaissance");
        Node securite = Node.create(NodeType.NEED, "sécurité");

        // --- Ressources (ce qui apaise) ---
        Node respiration = Node.create(NodeType.RESOURCE, "respiration lente", Map.of("kind", "corps"));
        Node marche = Node.create(NodeType.RESOURCE, "une marche dehors", Map.of("kind", "mouvement"));
        Node proche = Node.create(NodeType.RESOURCE, "appeler un proche", Map.of("kind", "lien"));

        // --- Personne (anonymisée) ---
        Node personneP = Node.create(NodeType.PERSON, "P.");

        // --- Entrée source ---
        Node entry = Node.create(NodeType.ENTRY, "Entrée",
                Map.of("rawText", "Journée lourde, la réunion m'a serré la gorge.", "sky", "ciel gris"));

        for (Node n : new Node[]{
                reunion, diner, retard, honte, anxiete, colere, pasHauteur, troublerai,
                bouleVentre, gorgeSerree, reconnaissance, securite,
                respiration, marche, proche, personneP, entry}) {
            store.put(n);
        }

        // --- Relations ---
        for (Edge e : new Edge[]{
                // La racine commune : deux situations → deux émotions → même croyance.
                Edge.of(EdgeType.TRIGGERS, reunion.id(), honte.id()),
                Edge.of(EdgeType.TRIGGERS, diner.id(), anxiete.id()),
                Edge.of(EdgeType.FED_BY, honte.id(), pasHauteur.id()),
                Edge.of(EdgeType.FED_BY, anxiete.id(), pasHauteur.id()),

                // Une autre branche.
                Edge.of(EdgeType.TRIGGERS, retard.id(), colere.id()),
                Edge.of(EdgeType.FED_BY, colere.id(), troublerai.id()),

                // Sensations.
                Edge.of(EdgeType.EXPRESSED_AS, anxiete.id(), bouleVentre.id()),
                Edge.of(EdgeType.EXPRESSED_AS, honte.id(), gorgeSerree.id()),

                // Besoins.
                Edge.of(EdgeType.TOUCHES, pasHauteur.id(), reconnaissance.id()),
                Edge.of(EdgeType.TOUCHES, troublerai.id(), securite.id()),

                // Ressources apaisantes.
                Edge.of(EdgeType.SOOTHES, respiration.id(), anxiete.id()),
                Edge.of(EdgeType.SOOTHES, marche.id(), colere.id()),
                Edge.of(EdgeType.SOOTHES, proche.id(), honte.id()),

                // Traçabilité de l'entrée.
                Edge.of(EdgeType.MENTIONS, entry.id(), reunion.id()),
                Edge.of(EdgeType.MENTIONS, entry.id(), honte.id()),
                Edge.of(EdgeType.MENTIONS, entry.id(), gorgeSerree.id()),
        }) {
            store.put(e);
        }
    }
}
