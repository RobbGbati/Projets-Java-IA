package com.misterbil.racines.adapter.out.persistence.neo4j;

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
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Adaptateur GraphStore sur Neo4j via {@link Neo4jClient} (Cypher bas niveau).
 *
 * <p>Choix d'implémentation : un seul label {@code :RacineNode} + une propriété
 * {@code type} (au lieu d'un label Neo4j par type). Cela évite APOC pour poser
 * des labels dynamiques et garde un MERGE simple par {@code id}. L'index
 * vectoriel porte sur {@code :RacineNode(embedding)} (voir {@code VectorIndexInitializer}).
 * Les types de relations viennent de l'enum {@link EdgeType} (valeurs contrôlées,
 * donc interpolables sans risque d'injection).</p>
 *
 * <p>Actif via {@code racines.store=neo4j}.</p>
 */
@Repository
@ConditionalOnProperty(name = "racines.store", havingValue = "neo4j")
public class Neo4jGraphStore implements GraphStore {

    static final String LABEL = "RacineNode";
    private static final List<String> RESERVED = List.of("id", "type", "label", "embedding", "createdAt");

    private final Neo4jClient client;

    public Neo4jGraphStore(Neo4jClient client) {
        this.client = client;
    }

    // ---- lecture --------------------------------------------------------
    @Override
    public InnerGraph load() {
        List<Node> nodes = client.query("""
                        MATCH (n:%s)
                        RETURN n.id AS id, n.type AS type, n.label AS label,
                               n.embedding AS embedding, n.createdAt AS createdAt, properties(n) AS props
                        """.formatted(LABEL))
                .fetch().all().stream().map(Neo4jGraphStore::toNode).toList();

        List<Edge> edges = client.query("""
                        MATCH (a:%s)-[r]->(b:%s)
                        RETURN a.id AS source, b.id AS target, type(r) AS type
                        """.formatted(LABEL, LABEL))
                .fetch().all().stream().map(Neo4jGraphStore::toEdge).filter(e -> e != null).toList();

        return new InnerGraph(nodes, edges);
    }

    // ---- écriture (MERGE idempotent) ------------------------------------
    @Override
    @Transactional
    public void apply(GraphChange change) {
        if (!change.nodesToUpsert().isEmpty()) {
            List<Map<String, Object>> rows = change.nodesToUpsert().stream()
                    .map(Neo4jGraphStore::nodeRow).toList();
            client.query("""
                    UNWIND $nodes AS n
                    MERGE (x:%s {id: n.id})
                    SET x.type = n.type, x.label = n.label, x.createdAt = n.createdAt
                    SET x += n.props
                    FOREACH (_ IN CASE WHEN n.embedding IS NULL THEN [] ELSE [1] END |
                             SET x.embedding = n.embedding)
                    """.formatted(LABEL))
                    .bind(rows).to("nodes").run();
        }
        // Arêtes groupées par type (type d'enum interpolé — valeur contrôlée).
        Map<EdgeType, List<Map<String, Object>>> byType = change.edgesToUpsert().stream()
                .collect(Collectors.groupingBy(Edge::type,
                        Collectors.mapping(e -> Map.<String, Object>of(
                                "source", e.source().value(), "target", e.target().value()),
                                Collectors.toList())));
        byType.forEach((type, rows) -> client.query("""
                        UNWIND $edges AS e
                        MATCH (a:%s {id: e.source}), (b:%s {id: e.target})
                        MERGE (a)-[:%s]->(b)
                        """.formatted(LABEL, LABEL, type.name()))
                .bind(rows).to("edges").run());
    }

    // ---- recherche vectorielle (phase 2) --------------------------------
    @Override
    public List<NodeRef> vectorSearch(float[] embedding, int topK) {
        if (embedding == null || embedding.length == 0) return List.of();
        return client.query("""
                        CALL db.index.vector.queryNodes('racines_emb', $k, $vec)
                        YIELD node, score
                        RETURN node.id AS id, node.type AS type, node.label AS label, score
                        """)
                .bind(Math.max(1, topK)).to("k")
                .bind(toFloatList(embedding)).to("vec")
                .fetch().all().stream()
                .map(r -> new NodeRef(
                        NodeId.of((String) r.get("id")),
                        NodeType.valueOf((String) r.get("type")),
                        str(r.get("label")),
                        ((Number) r.get("score")).doubleValue()))
                .toList();
    }

    // ---- traversée (phase 2) --------------------------------------------
    @Override
    public SubGraph traverse(Collection<NodeId> anchors, int maxHops) {
        List<String> ids = anchors.stream().map(NodeId::value).toList();
        if (ids.isEmpty()) return SubGraph.empty();
        int hops = Math.max(1, maxHops);

        List<Node> nodes = client.query("""
                        MATCH (a:%s) WHERE a.id IN $ids
                        MATCH (a)-[*0..%d]-(m:%s)
                        RETURN DISTINCT m.id AS id, m.type AS type, m.label AS label,
                               m.embedding AS embedding, m.createdAt AS createdAt, properties(m) AS props
                        """.formatted(LABEL, hops, LABEL))
                .bind(ids).to("ids")
                .fetch().all().stream().map(Neo4jGraphStore::toNode).toList();

        List<String> reached = nodes.stream().map(n -> n.id().value()).toList();
        List<Edge> edges = client.query("""
                        MATCH (a:%s)-[r]->(b:%s)
                        WHERE a.id IN $ids AND b.id IN $ids
                        RETURN a.id AS source, b.id AS target, type(r) AS type
                        """.formatted(LABEL, LABEL))
                .bind(reached).to("ids")
                .fetch().all().stream().map(Neo4jGraphStore::toEdge).filter(e -> e != null).toList();

        return new SubGraph(nodes, edges);
    }

    // ---- racines communes (US7) -----------------------------------------
    @Override
    @SuppressWarnings("unchecked")
    public List<CommonRoot> commonRoots() {
        return client.query("""
                        MATCH (s:%s {type:'SITUATION'})-[:TRIGGERS]->(:%s {type:'EMOTION'})
                              -[:FED_BY]->(b:%s {type:'BELIEF'})
                        WITH b, collect(DISTINCT s) AS sits
                        WHERE size(sits) >= 2
                        RETURN b.id AS beliefId, b.label AS beliefLabel,
                               [x IN sits | {id: x.id, label: x.label}] AS situations
                        """.formatted(LABEL, LABEL, LABEL))
                .fetch().all().stream()
                .map(r -> {
                    Node belief = minimalNode(NodeType.BELIEF, (String) r.get("beliefId"), str(r.get("beliefLabel")));
                    List<Map<String, Object>> sits = (List<Map<String, Object>>) r.get("situations");
                    List<Node> situations = sits.stream()
                            .map(m -> minimalNode(NodeType.SITUATION, (String) m.get("id"), str(m.get("label"))))
                            .toList();
                    return new CommonRoot(belief, situations);
                })
                .toList();
    }

    // ---- mappers ligne ↔ domaine ----------------------------------------
    @SuppressWarnings("unchecked")
    private static Node toNode(Map<String, Object> r) {
        String id = (String) r.get("id");
        NodeType type = NodeType.valueOf((String) r.get("type"));
        String label = str(r.get("label"));
        Map<String, Object> props = (Map<String, Object>) r.getOrDefault("props", Map.of());
        Map<String, Object> extra = new LinkedHashMap<>();
        props.forEach((k, v) -> { if (!RESERVED.contains(k)) extra.put(k, v); });
        float[] emb = toFloatArray(r.get("embedding"));
        Instant created = parseInstant(r.get("createdAt"));
        return new Node(NodeId.of(id), type, label, extra, emb, created);
    }

    private static Edge toEdge(Map<String, Object> r) {
        try {
            return Edge.of(EdgeType.valueOf((String) r.get("type")),
                    NodeId.of((String) r.get("source")), NodeId.of((String) r.get("target")));
        } catch (Exception e) {
            return null; // relation inconnue → ignorée
        }
    }

    private static Node minimalNode(NodeType type, String id, String label) {
        return new Node(NodeId.of(id), type, label, Map.of(), null, Instant.now());
    }

    private static Map<String, Object> nodeRow(Node n) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", n.id().value());
        row.put("type", n.type().name());
        row.put("label", n.label());
        row.put("createdAt", n.createdAt().toString());
        row.put("props", n.extra());
        row.put("embedding", n.hasEmbedding() ? toFloatList(n.embedding()) : null);
        return row;
    }

    private static List<Float> toFloatList(float[] v) {
        List<Float> out = new ArrayList<>(v.length);
        for (float f : v) out.add(f);
        return out;
    }

    private static float[] toFloatArray(Object o) {
        if (!(o instanceof Iterable<?> it)) return null;
        List<Float> tmp = StreamSupport.stream(it.spliterator(), false)
                .map(x -> ((Number) x).floatValue()).toList();
        if (tmp.isEmpty()) return null;
        float[] out = new float[tmp.size()];
        for (int i = 0; i < out.length; i++) out[i] = tmp.get(i);
        return out;
    }

    private static Instant parseInstant(Object o) {
        try {
            return o == null ? Instant.now() : Instant.parse(o.toString());
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
