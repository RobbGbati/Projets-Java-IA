package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.InnerGraph;

/** Cas d'usage : lire la carte complète (phase 0). */
public interface GetGraph {
    InnerGraph full();
}
