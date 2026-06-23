package com.misterbil.racines.domain.port.out;

import com.misterbil.racines.domain.model.CommonRoot;
import com.misterbil.racines.domain.model.GraphChange;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeRef;
import com.misterbil.racines.domain.model.SubGraph;

import java.util.Collection;
import java.util.List;

/**
 * Le store du graphe (graphe + vecteurs). Fourni par un adaptateur :
 * {@code InMemoryGraphStore} (phase 0) ou {@code Neo4jGraphStore} (phase 1+).
 *
 * <p>Regroupe graphe et recherche vectorielle parce que Neo4j fait les deux
 * (SPEC §3). On scindera si ça grossit — pas avant.</p>
 */
public interface GraphStorePort {

    /** Charge la carte complète. */
    InnerGraph load();

    /** Applique un changement (MERGE des nœuds/arêtes par id). */
    void apply(GraphChange change);

    /** Recherche d'ancres par similarité vectorielle (phase 2). */
    List<NodeRef> vectorSearch(float[] embedding, int topK);

    /** Traverse le graphe depuis des ancres jusqu'à {@code maxHops} sauts (phase 2). */
    SubGraph traverse(Collection<NodeId> anchors, int maxHops);

    /** Racines communes via requête pure graphe (US7). */
    List<CommonRoot> commonRoots();
}
