package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.Answer;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeRef;
import com.misterbil.racines.domain.model.RagSettings;
import com.misterbil.racines.domain.model.SubGraph;
import com.misterbil.racines.domain.port.in.AskQuestion;
import com.misterbil.racines.domain.port.out.ChatPort;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphStorePort;

import java.util.List;

/**
 * Le pipeline GraphRAG (phase 2, SPEC §5.1). Ne dépend que d'interfaces →
 * testable avec un GraphStorePort/EmbeddingPort/ChatPort bouchonnés, sans vraie
 * base ni vrai LLM. C'est tout l'intérêt de l'hexagonale.
 */
public class GraphRagService implements AskQuestion {

    private final GraphStorePort graph;
    private final EmbeddingPort embeddings;
    private final ChatPort chat;
    private final RagSettings cfg;

    public GraphRagService(GraphStorePort graph, EmbeddingPort embeddings, ChatPort chat, RagSettings cfg) {
        this.graph = graph;
        this.embeddings = embeddings;
        this.chat = chat;
        this.cfg = cfg;
    }

    @Override
    public Answer ask(String question) {
        float[] q = embeddings.embed(question);                          // 1. question → vecteur
        List<NodeRef> anchors = graph.vectorSearch(q, cfg.topK());       // 2. ancres
        SubGraph context = graph.traverse(ids(anchors), cfg.maxHops());  // 3. sous-graphe
        String prompt = ContextBuilder.from(context);                    // 4. → texte
        String answer = chat.generate(SystemPrompts.SYSTEM_DOUX, prompt, question); // 5. réponse
        return new Answer(answer, context);                              // 6. réponse + sous-graphe
    }

    private List<NodeId> ids(List<NodeRef> refs) {
        return refs.stream().map(NodeRef::id).toList();
    }
}
