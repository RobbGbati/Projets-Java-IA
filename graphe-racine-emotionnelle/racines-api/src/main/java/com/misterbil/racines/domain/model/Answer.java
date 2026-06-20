package com.misterbil.racines.domain.model;

/**
 * Réponse à une question (phase 2). Renvoie TOUJOURS le sous-graphe qui a
 * fondé la réponse, pour que le front le surligne (PRD : « la carte témoigne »).
 */
public record Answer(String answer, SubGraph subgraph) {
}
