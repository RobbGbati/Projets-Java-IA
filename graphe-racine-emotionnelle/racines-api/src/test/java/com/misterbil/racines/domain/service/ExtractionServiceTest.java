package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.GraphChange;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeType;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphExtractor;
import com.misterbil.racines.domain.port.out.GraphStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vérifie le dédoublonnage : une émotion déjà présente n'est pas recréée, et
 *  l'arête de la proposition est remappée sur l'id existant. */
class ExtractionServiceTest {

    @Test
    void confirm_dedoublonne_par_label_et_remappe_les_aretes() {
        GraphExtractor extractor = mock(GraphExtractor.class);
        GraphStore store = mock(GraphStore.class);
        EmbeddingPort embeddings = mock(EmbeddingPort.class);
        when(embeddings.embed(any())).thenReturn(new float[0]);

        Node existingHonte = new Node(NodeId.of("real-honte"), NodeType.EMOTION, "honte", Map.of(), null, Instant.now());
        when(store.load()).thenReturn(new InnerGraph(List.of(existingHonte), List.of()));

        // Proposition : la même émotion (casse différente) + une croyance neuve, reliées.
        Node propHonte = new Node(NodeId.of("n1"), NodeType.EMOTION, "Honte", Map.of(), null, Instant.now());
        Node propBelief = new Node(NodeId.of("n2"), NodeType.BELIEF, "je ne vaux rien", Map.of(), null, Instant.now());
        Edge propEdge = Edge.of(EdgeType.FED_BY, NodeId.of("n1"), NodeId.of("n2"));
        ExtractionProposal validated = new ExtractionProposal(List.of(propHonte, propBelief), List.of(propEdge));

        new ExtractionService(extractor, store, embeddings).confirm(validated);

        ArgumentCaptor<GraphChange> captor = ArgumentCaptor.forClass(GraphChange.class);
        verify(store).apply(captor.capture());
        GraphChange change = captor.getValue();

        // L'émotion existante n'est pas réécrite : seule la croyance neuve est ajoutée.
        assertThat(change.nodesToUpsert()).extracting(Node::type).containsExactly(NodeType.BELIEF);
        // L'arête pointe désormais vers l'id RÉEL de l'émotion existante.
        assertThat(change.edgesToUpsert()).hasSize(1);
        assertThat(change.edgesToUpsert().get(0).source()).isEqualTo(NodeId.of("real-honte"));
    }
}
