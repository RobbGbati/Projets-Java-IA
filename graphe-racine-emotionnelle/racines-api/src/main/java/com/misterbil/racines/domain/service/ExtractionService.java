package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.GraphChange;
import com.misterbil.racines.domain.model.GraphSchema;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.port.in.ConfirmProposal;
import com.misterbil.racines.domain.port.in.ProposeFromText;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphExtractorPort;
import com.misterbil.racines.domain.port.out.GraphStorePort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extraction libre (phase 3, SPEC §5.2).
 * <ul>
 *   <li>{@code propose} : le LLM repère des nœuds/relations → proposition, SANS
 *       persistance (consentement, PRD §3.4).</li>
 *   <li>{@code confirm} : à la validation, fusion dans le store avec
 *       dédoublonnage par label normalisé (pas 10× « anxiété »).</li>
 * </ul>
 */
public class ExtractionService implements ProposeFromText, ConfirmProposal {

    private final GraphExtractorPort extractor;
    private final GraphStorePort store;
    private final EmbeddingPort embeddings;

    public ExtractionService(GraphExtractorPort extractor, GraphStorePort store, EmbeddingPort embeddings) {
        this.extractor = extractor;
        this.store = store;
        this.embeddings = embeddings;
    }

    @Override
    public ExtractionProposal propose(String rawText) {
        if (rawText == null || rawText.isBlank()) return ExtractionProposal.empty();
        return extractor.extract(rawText, GraphSchema.racinesDefault());
    }

    @Override
    public InnerGraph confirm(ExtractionProposal validated) {
        if (validated == null) return store.load();

        InnerGraph existing = store.load();
        Map<NodeId, NodeId> idMap = new HashMap<>();   // id proposé → id réel (existant ou nouveau)
        List<Node> toUpsert = new ArrayList<>();

        for (Node proposed : validated.nodes()) {
            Node match = findDuplicate(existing, proposed);
            if (match != null) {
                idMap.put(proposed.id(), match.id());           // réutilise le nœud existant
            } else {
                Node withEmb = withEmbedding(proposed);
                idMap.put(proposed.id(), withEmb.id());
                toUpsert.add(withEmb);
            }
        }

        // Remappe les arêtes sur les ids réels.
        List<Edge> edges = new ArrayList<>();
        for (Edge e : validated.edges()) {
            NodeId s = idMap.getOrDefault(e.source(), e.source());
            NodeId t = idMap.getOrDefault(e.target(), e.target());
            edges.add(Edge.of(e.type(), s, t));
        }

        store.apply(GraphChange.of(toUpsert, edges));
        return store.load();
    }

    /** Dédoublonnage simple : même type + même label normalisé. (Embedding-based à venir.) */
    private Node findDuplicate(InnerGraph graph, Node proposed) {
        String key = normalize(proposed.label());
        return graph.nodesOfType(proposed.type()).stream()
                .filter(n -> normalize(n.label()).equals(key))
                .findFirst()
                .orElse(null);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private Node withEmbedding(Node n) {
        if (!n.type().isEmbeddable()) return n;
        float[] v = embeddings.embed(n.label());
        return (v != null && v.length > 0) ? n.withEmbedding(v) : n;
    }
}
