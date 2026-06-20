package com.misterbil.racines.adapter.out.ai;

import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.GraphSchema;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.misterbil.racines.domain.port.out.GraphExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptateur GraphExtractor via Spring AI — sortie STRUCTURÉE (phase 3, SPEC §5.2).
 *
 * <p>Le LLM ne reçoit qu'un schéma contraint et renvoie un JSON typé (via
 * {@code .entity(...)}). On mappe ensuite vers le modèle de domaine. Les ids
 * sont les {@code tempId} proposés : {@code ConfirmProposal} les remappe à la
 * validation (dédoublonnage). Best-effort : sur erreur → proposition vide.</p>
 */
@Component
public class SpringAiGraphExtractor implements GraphExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpringAiGraphExtractor.class);

    private final ObjectProvider<ChatModel> models;

    public SpringAiGraphExtractor(ObjectProvider<ChatModel> models) {
        this.models = models;
    }

    // DTO de sortie du LLM (séparé du domaine : c'est un détail d'adaptateur).
    // @JsonProperty obligatoire : sans noms de paramètres compilés, Jackson ne
    // reconnaît pas le constructeur canonique du record (« setterless property »).
    record LlmNode(@JsonProperty("tempId") String tempId,
                   @JsonProperty("type") String type,
                   @JsonProperty("label") String label) {}

    record LlmEdge(@JsonProperty("type") String type,
                   @JsonProperty("sourceTempId") String sourceTempId,
                   @JsonProperty("targetTempId") String targetTempId) {}

    record LlmGraph(@JsonProperty("nodes") List<LlmNode> nodes,
                    @JsonProperty("edges") List<LlmEdge> edges) {}

    @Override
    public ExtractionProposal extract(String rawText, GraphSchema schema) {
        ChatModel model = models.getIfAvailable();
        if (model == null) return ExtractionProposal.empty();   // IA désactivée
        try {
            String system = """
                    Tu extrais une carte émotionnelle d'un texte intime, en français.
                    Reste FIDÈLE au texte : n'invente aucun élément non exprimé.
                    Anonymise toute personne (alias « P. ») — jamais de nom réel.

                    Types de nœuds autorisés (champ "type", en MAJUSCULES) : %s
                    Types de relations autorisés (champ "type") : %s
                    Relations valides :
                      SITUATION -TRIGGERS-> EMOTION
                      EMOTION   -FED_BY-> BELIEF
                      EMOTION   -EXPRESSED_AS-> SENSATION
                      BELIEF    -TOUCHES-> NEED
                      RESOURCE  -SOOTHES-> EMOTION

                    Donne à chaque nœud un "tempId" court et unique (n1, n2, …) et
                    référence ces tempId dans les arêtes (sourceTempId/targetTempId).
                    """.formatted(typeNames(schema), edgeNames(schema));

            LlmGraph out = ChatClient.create(model).prompt()
                    .system(system)
                    .user(rawText)
                    .call()
                    .entity(LlmGraph.class);

            return toProposal(out);
        } catch (Exception e) {
            log.warn("Extraction indisponible ({}) — proposition vide", e.getMessage());
            return ExtractionProposal.empty();
        }
    }

    private ExtractionProposal toProposal(LlmGraph out) {
        if (out == null || out.nodes() == null) return ExtractionProposal.empty();
        List<Node> nodes = new ArrayList<>();
        for (LlmNode ln : out.nodes()) {
            NodeType type = parseNodeType(ln.type());
            if (type == null || ln.tempId() == null || ln.label() == null) continue;
            nodes.add(new Node(NodeId.of(ln.tempId()), type, ln.label(), Map.of(), null, Instant.now()));
        }
        List<Edge> edges = new ArrayList<>();
        if (out.edges() != null) {
            for (LlmEdge le : out.edges()) {
                EdgeType type = parseEdgeType(le.type());
                if (type == null || le.sourceTempId() == null || le.targetTempId() == null) continue;
                edges.add(Edge.of(type, NodeId.of(le.sourceTempId()), NodeId.of(le.targetTempId())));
            }
        }
        return new ExtractionProposal(nodes, edges);
    }

    private static String typeNames(GraphSchema s) {
        return s.nodeTypes().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String edgeNames(GraphSchema s) {
        return s.edgeTypes().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static NodeType parseNodeType(String s) {
        try {
            return NodeType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static EdgeType parseEdgeType(String s) {
        try {
            return EdgeType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}
