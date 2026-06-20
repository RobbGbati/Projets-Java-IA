package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.Answer;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeRef;
import com.misterbil.racines.domain.model.NodeType;
import com.misterbil.racines.domain.model.RagSettings;
import com.misterbil.racines.domain.model.SubGraph;
import com.misterbil.racines.domain.port.out.ChatPort;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le pipeline GraphRAG testé SANS vraie base ni vrai LLM : tous les ports sont
 * bouchonnés. C'est l'intérêt direct de l'architecture hexagonale.
 */
class GraphRagServiceTest {

    @Test
    void ask_enchaine_embed_search_traverse_generate_et_renvoie_le_sous_graphe() {
        GraphStore store = mock(GraphStore.class);
        EmbeddingPort embeddings = mock(EmbeddingPort.class);
        ChatPort chat = mock(ChatPort.class);

        float[] vec = {0.1f, 0.2f, 0.3f};
        NodeRef anchor = new NodeRef(NodeId.of("e1"), NodeType.EMOTION, "honte", 0.92);
        SubGraph context = new SubGraph(
                List.of(new Node(NodeId.of("e1"), NodeType.EMOTION, "honte", java.util.Map.of(), null, null)),
                List.of());

        when(embeddings.embed("qu'est-ce qui m'apaise ?")).thenReturn(vec);
        when(store.vectorSearch(eq(vec), anyInt())).thenReturn(List.of(anchor));
        when(store.traverse(any(), anyInt())).thenReturn(context);
        when(chat.generate(any(), any(), eq("qu'est-ce qui m'apaise ?"))).thenReturn("Un fil doux relie…");

        GraphRagService service = new GraphRagService(store, embeddings, chat, new RagSettings(6, 3));
        Answer answer = service.ask("qu'est-ce qui m'apaise ?");

        assertThat(answer.answer()).isEqualTo("Un fil doux relie…");
        assertThat(answer.subgraph()).isEqualTo(context);
        verify(store).vectorSearch(vec, 6);
        verify(store).traverse(List.of(NodeId.of("e1")), 3);
    }
}
