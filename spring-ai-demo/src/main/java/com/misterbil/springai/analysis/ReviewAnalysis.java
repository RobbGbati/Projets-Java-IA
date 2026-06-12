package com.misterbil.springai.analysis;

import java.util.List;

/**
 * Le CONTRAT de sortie de l'endpoint d'analyse.
 *
 * Ce record est le cœur pédagogique de l'endpoint 2 : c'est LUI qui
 * définit ce que le LLM a le droit de renvoyer. Spring AI va :
 *
 *   1. générer un schéma JSON à partir de ce record (champs, types, enum),
 *   2. injecter ce schéma dans le prompt avec l'instruction "réponds
 *      uniquement en JSON conforme à ce schéma",
 *   3. désérialiser la réponse du LLM vers ce record via Jackson.
 *
 * Autrement dit : le type Java EST la source de vérité du contrat.
 * Si tu fais du React avec Zod, c'est exactement le même réflexe que
 * z.object({...}) à la frontière d'une API : on ne laisse jamais entrer
 * des données non validées dans le système. Ici, la "donnée non fiable",
 * c'est la sortie du LLM.
 */
public record ReviewAnalysis(

        /** Sentiment global — l'enum CONTRAINT le LLM à 3 valeurs possibles.
         *  Si le modèle renvoie autre chose, la désérialisation échoue :
         *  on échoue plutôt que de propager une valeur inventée. */
        Sentiment sentiment,

        /** Note estimée de 1 à 5, déduite du ton de l'avis. */
        int estimatedRating,

        /** Résumé de l'avis en une phrase. */
        String summary,

        /** Points clés mentionnés par le client (3 maximum demandés dans le prompt). */
        List<String> keyPoints,

        /** Le client mentionne-t-il un problème nécessitant une action du support ? */
        boolean needsSupportFollowUp,

        String detectedLanguage
) {

    public enum Sentiment {
        POSITIF, NEUTRE, NEGATIF
    }
}
