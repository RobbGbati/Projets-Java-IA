package com.misterbil.racines.domain.model;

/**
 * Les relations typées du graphe (SPEC §4).
 * <pre>
 * (Situation)-[:TRIGGERS]->(Emotion)
 * (Emotion)  -[:FED_BY]->(Belief)
 * (Emotion)  -[:EXPRESSED_AS]->(Sensation)
 * (Belief)   -[:TOUCHES]->(Need)
 * (Resource) -[:SOOTHES]->(Emotion)
 * (Entry)    -[:MENTIONS]->(…)        // traçabilité vers la source
 * </pre>
 */
public enum EdgeType {
    TRIGGERS,      // Situation → Emotion
    FED_BY,        // Emotion → Belief
    EXPRESSED_AS,  // Emotion → Sensation
    TOUCHES,       // Belief → Need
    SOOTHES,       // Resource → Emotion
    MENTIONS       // Entry → n'importe quel nœud
}
