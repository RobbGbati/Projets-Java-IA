package com.misterbil.racines.adapter.in.web;

import com.misterbil.racines.adapter.in.web.dto.Dtos.CommonRootDto;
import com.misterbil.racines.adapter.in.web.dto.Dtos.EdgeDto;
import com.misterbil.racines.adapter.in.web.dto.Dtos.GraphDto;
import com.misterbil.racines.adapter.in.web.dto.Dtos.NodeDto;
import com.misterbil.racines.domain.model.CommonRoot;
import com.misterbil.racines.domain.model.Edge;
import com.misterbil.racines.domain.model.EdgeType;
import com.misterbil.racines.domain.model.ExtractionProposal;
import com.misterbil.racines.domain.model.InnerGraph;
import com.misterbil.racines.domain.model.Node;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.model.NodeType;
import com.misterbil.racines.domain.model.SubGraph;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Conversion domaine ↔ DTO. Pas de logique métier ici. */
final class WebMapper {

    private WebMapper() {}

    static GraphDto toGraph(InnerGraph g) {
        return new GraphDto(g.nodes().stream().map(WebMapper::node).toList(),
                g.edges().stream().map(WebMapper::edge).toList());
    }

    static GraphDto toGraph(SubGraph g) {
        return new GraphDto(g.nodes().stream().map(WebMapper::node).toList(),
                g.edges().stream().map(WebMapper::edge).toList());
    }

    static GraphDto toGraph(ExtractionProposal p) {
        return new GraphDto(p.nodes().stream().map(WebMapper::node).toList(),
                p.edges().stream().map(WebMapper::edge).toList());
    }

    static NodeDto node(Node n) {
        return new NodeDto(n.id().value(), n.type().name(), n.label(), n.extra());
    }

    static EdgeDto edge(Edge e) {
        return new EdgeDto(e.id(), e.type().name(), e.source().value(), e.target().value());
    }

    static CommonRootDto commonRoot(CommonRoot r) {
        return new CommonRootDto(node(r.belief()),
                r.situations().stream().map(WebMapper::node).toList(),
                r.revelation());
    }

    // ---- DTO → domaine (confirmation d'une proposition validée) ----------
    static ExtractionProposal toProposal(GraphDto dto) {
        List<Node> nodes = dto.nodes().stream()
                .map(n -> new Node(NodeId.of(n.id()), NodeType.valueOf(n.type().toUpperCase()),
                        n.label(), n.extra() == null ? Map.of() : n.extra(), null, Instant.now()))
                .toList();
        List<Edge> edges = dto.edges().stream()
                .map(e -> Edge.of(EdgeType.valueOf(e.type().toUpperCase()),
                        NodeId.of(e.source()), NodeId.of(e.target())))
                .toList();
        return new ExtractionProposal(nodes, edges);
    }

    static NodeType nodeType(String s) {
        return NodeType.valueOf(s.trim().toUpperCase());
    }

    static EdgeType edgeType(String s) {
        return EdgeType.valueOf(s.trim().toUpperCase());
    }
}
