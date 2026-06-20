package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.ExtractionProposal;

/** Cas d'usage : proposer des nœuds/relations extraits d'un texte libre (phase 3, à valider). */
public interface ProposeFromText {
    ExtractionProposal propose(String rawText);
}
