package com.misterbil.racines.domain.port.out;

import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.GraphSchema;

/**
 * Extrait des nœuds + relations d'un texte libre selon un schéma contraint
 * (phase 3). Impl : Spring AI (sortie structurée) ou LangChain4j LLMGraphTransformer.
 * Ne persiste rien : renvoie une proposition à valider.
 */
public interface GraphExtractor {
    ExtractionProposal extract(String rawText, GraphSchema schema);
}
