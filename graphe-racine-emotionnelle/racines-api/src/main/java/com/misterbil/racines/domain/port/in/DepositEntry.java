package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.EntryDraft;
import com.misterbil.racines.domain.model.InnerGraph;

/** Cas d'usage : déposer une entrée STRUCTURÉE et faire pousser les racines (phase 1). */
public interface DepositEntry {
    InnerGraph deposit(EntryDraft draft);
}
