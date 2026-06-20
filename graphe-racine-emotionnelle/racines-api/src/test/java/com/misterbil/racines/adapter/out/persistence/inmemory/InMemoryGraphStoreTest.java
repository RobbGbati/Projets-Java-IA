package com.misterbil.racines.adapter.out.persistence.inmemory;

import com.misterbil.racines.domain.model.CommonRoot;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Vérifie le seed et l'algo de racine commune (sans aucune infra). */
class InMemoryGraphStoreTest {

    @Test
    void le_seed_charge_une_carte_non_vide() {
        InnerGraph g = new InMemoryGraphStore().load();
        assertThat(g.nodes()).hasSizeGreaterThanOrEqualTo(15);
        assertThat(g.nodesOfType(NodeType.SITUATION)).isNotEmpty();
        assertThat(g.edges()).isNotEmpty();
    }

    @Test
    void detecte_la_racine_commune_du_seed() {
        List<CommonRoot> roots = new InMemoryGraphStore().commonRoots();

        assertThat(roots).isNotEmpty();
        CommonRoot root = roots.stream()
                .filter(r -> r.belief().label().contains("à la hauteur"))
                .findFirst().orElseThrow();
        assertThat(root.situations()).hasSizeGreaterThanOrEqualTo(2);
    }
}
