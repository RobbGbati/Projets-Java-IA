package com.misterbil.racines.adapter.out.persistence.inmemory;

import com.misterbil.racines.domain.model.CommonRoot;
import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.GraphChange;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeRef;
import com.misterbil.racines.domain.model.NodeType;
import com.misterbil.racines.domain.model.SubGraph;
import com.misterbil.racines.domain.port.out.GraphStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adaptateur GraphStore EN MÉMOIRE (phase 0). Aucune base, aucune annotation de
 * persistance : il manipule directement le modèle de domaine — le meilleur
 * garde-fou contre le couplage prématuré (SPEC §4). Données semées au démarrage.
 *
 * <p>Actif par défaut, ou via {@code racines.store=inmemory}.</p>
 */
@Component
@ConditionalOnProperty(name = "racines.store", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryGraphStore implements GraphStore {

    private final Map<NodeId, Node> nodes = new LinkedHashMap<>();
    private final Set<Edge> edges = new LinkedHashSet<>();

    public InMemoryGraphStore() {
        SeedData.seed(this);
    }

    @Override
    public synchronized InnerGraph load() {
        return new InnerGraph(new ArrayList<>(nodes.values()), new ArrayList<>(edges));
    }

    @Override
    public synchronized void apply(GraphChange change) {
        change.nodesToUpsert().forEach(n -> nodes.put(n.id(), n));
        edges.addAll(change.edgesToUpsert());
    }

    @Override
    public synchronized List<NodeRef> vectorSearch(float[] embedding, int topK) {
        if (embedding == null || embedding.length == 0) return List.of();
        return nodes.values().stream()
                .filter(Node::hasEmbedding)
                .map(n -> new NodeRef(n.id(), n.type(), n.label(), cosine(embedding, n.embedding())))
                .sorted(Comparator.comparingDouble(NodeRef::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    @Override
    public synchronized SubGraph traverse(Collection<NodeId> anchors, int maxHops) {
        Set<NodeId> visited = new LinkedHashSet<>();
        Set<Edge> kept = new LinkedHashSet<>();
        Deque<NodeId> queue = new ArrayDeque<>();
        Map<NodeId, Integer> depth = new LinkedHashMap<>();

        for (NodeId a : anchors) {
            if (nodes.containsKey(a)) {
                queue.add(a);
                depth.put(a, 0);
                visited.add(a);
            }
        }
        while (!queue.isEmpty()) {
            NodeId current = queue.poll();
            int d = depth.get(current);
            if (d >= maxHops) continue;
            for (Edge e : edges) {
                NodeId other = null;
                if (e.source().equals(current)) other = e.target();
                else if (e.target().equals(current)) other = e.source();
                if (other == null) continue;
                kept.add(e);
                if (visited.add(other)) {
                    depth.put(other, d + 1);
                    queue.add(other);
                }
            }
        }
        List<Node> subNodes = visited.stream().map(nodes::get).filter(n -> n != null).toList();
        return new SubGraph(subNodes, new ArrayList<>(kept));
    }

    @Override
    public synchronized List<CommonRoot> commonRoots() {
        // Beliefs nourries par ≥2 émotions déclenchées par ≥2 situations distinctes.
        List<CommonRoot> roots = new ArrayList<>();
        for (Node belief : nodesOfType(NodeType.BELIEF)) {
            Set<Node> situations = new LinkedHashSet<>();
            for (Edge fedBy : edges) {                       // emotion -FED_BY-> belief
                if (fedBy.type() != EdgeType.FED_BY || !fedBy.target().equals(belief.id())) continue;
                NodeId emotionId = fedBy.source();
                for (Edge trig : edges) {                     // situation -TRIGGERS-> emotion
                    if (trig.type() == EdgeType.TRIGGERS && trig.target().equals(emotionId)) {
                        Node s = nodes.get(trig.source());
                        if (s != null) situations.add(s);
                    }
                }
            }
            if (situations.size() >= 2) {
                roots.add(new CommonRoot(belief, new ArrayList<>(situations)));
            }
        }
        return roots;
    }

    // ---- helpers internes / accès paquet pour le seed --------------------
    List<Node> nodesOfType(NodeType type) {
        return nodes.values().stream().filter(n -> n.type() == type).toList();
    }

    void put(Node n) {
        nodes.put(n.id(), n);
    }

    void put(Edge e) {
        edges.add(e);
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length != b.length) return -1;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return -1;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
