package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.InnerGraph;

/** Cas d'usage : persister une proposition VALIDÉE par l'utilisateur (phase 3). */
public interface ConfirmProposal {
    InnerGraph confirm(ExtractionProposal validated);
}
