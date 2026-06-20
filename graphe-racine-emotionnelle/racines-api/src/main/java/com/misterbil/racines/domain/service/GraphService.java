package com.misterbil.racines.domain.service;

import com.misterbil.racines.domain.model.CommonRoot;
import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.EntryDraft;
import com.misterbil.racines.domain.model.GraphChange;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeType;
import com.misterbil.racines.domain.port.in.DepositEntry;
import com.misterbil.racines.domain.port.in.FindCommonRoots;
import com.misterbil.racines.domain.port.in.GetGraph;
import com.misterbil.racines.domain.port.in.WriteGraph;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cœur des cas d'usage « carte » : lecture, écriture directe, dépôt structuré,
 * racines communes (phases 0, 1 ; US7). Ne connaît QUE des ports (interfaces)
 * → testable avec un GraphStore et un EmbeddingPort bouchonnés.
 */
public class GraphService implements GetGraph, WriteGraph, DepositEntry, FindCommonRoots {

    private final GraphStore store;
    private final EmbeddingPort embeddings;

    public GraphService(GraphStore store, EmbeddingPort embeddings) {
        this.store = store;
        this.embeddings = embeddings;
    }

    // ---- GetGraph -------------------------------------------------------
    @Override
    public InnerGraph full() {
        return store.load();
    }

    // ---- WriteGraph -----------------------------------------------------
    @Override
    public Node addNode(NodeType type, String label, Map<String, Object> extra) {
        Node node = withEmbedding(Node.create(type, label, extra));
        store.apply(GraphChange.of(List.of(node), List.of()));
        return node;
    }

    @Override
    public Edge addEdge(EdgeType type, NodeId source, NodeId target) {
        Edge edge = Edge.of(type, source, target);
        store.apply(GraphChange.of(List.of(), List.of(edge)));
        return edge;
    }

    // ---- DepositEntry (phase 1, structuré) ------------------------------
    @Override
    public InnerGraph deposit(EntryDraft draft) {
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        // Nœuds, créés seulement pour les champs renseignés.
        Node emotion = draft.has(draft.emotion()) ? node(NodeType.EMOTION, draft.emotion(), nodes) : null;
        Node situation = draft.has(draft.situation()) ? node(NodeType.SITUATION, draft.situation(), nodes) : null;
        Node belief = draft.has(draft.belief()) ? node(NodeType.BELIEF, draft.belief(), nodes) : null;
        Node sensation = draft.has(draft.sensation()) ? node(NodeType.SENSATION, draft.sensation(), nodes) : null;
        Node need = draft.has(draft.need()) ? node(NodeType.NEED, draft.need(), nodes) : null;
        Node resource = draft.has(draft.resource()) ? node(NodeType.RESOURCE, draft.resource(), nodes) : null;
        Node person = draft.has(draft.person()) ? node(NodeType.PERSON, draft.person(), nodes) : null;

        // L'entrée elle-même : texte source + ciel (humeur).
        Node entry = Node.create(NodeType.ENTRY, "Entrée",
                Map.of("rawText", draft.rawText() == null ? "" : draft.rawText(),
                        "sky", draft.sky() == null ? "" : draft.sky()));
        nodes.add(entry);

        // Relations du modèle (SPEC §4), uniquement entre nœuds présents.
        link(situation, EdgeType.TRIGGERS, emotion, edges);
        link(emotion, EdgeType.FED_BY, belief, edges);
        link(emotion, EdgeType.EXPRESSED_AS, sensation, edges);
        link(belief, EdgeType.TOUCHES, need, edges);
        link(resource, EdgeType.SOOTHES, emotion, edges);

        // Traçabilité : l'entrée mentionne tous les nœuds « de fond » créés.
        for (Node n : new ArrayList<>(nodes)) {
            if (n != entry) edges.add(Edge.of(EdgeType.MENTIONS, entry.id(), n.id()));
        }

        store.apply(GraphChange.of(nodes, edges));
        return store.load();
    }

    // ---- FindCommonRoots (US7, pure graphe) -----------------------------
    @Override
    public List<CommonRoot> find() {
        return store.commonRoots();
    }

    // ---- helpers --------------------------------------------------------
    private Node node(NodeType type, String label, List<Node> sink) {
        Node n = withEmbedding(Node.create(type, label));
        sink.add(n);
        return n;
    }

    private void link(Node from, EdgeType type, Node to, List<Edge> sink) {
        if (from != null && to != null) {
            sink.add(Edge.of(type, from.id(), to.id()));
        }
    }

    /** Attache un embedding aux nœuds « parlants ». Best-effort : si le service
     *  IA est absent (phase 0/1 hors-ligne), embed renvoie [] → pas de vecteur. */
    private Node withEmbedding(Node n) {
        if (!n.type().isEmbeddable()) return n;
        float[] v = embeddings.embed(n.label());
        return (v != null && v.length > 0) ? n.withEmbedding(v) : n;
    }
}
