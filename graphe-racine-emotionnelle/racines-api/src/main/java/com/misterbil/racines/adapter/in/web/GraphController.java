package com.misterbil.racines.adapter.in.web;

import com.misterbil.racines.adapter.in.web.dto.Dtos.CreateEdgeRequest;
import com.misterbil.racines.adapter.in.web.dto.Dtos.CreateNodeRequest;
import com.misterbil.racines.adapter.in.web.dto.Dtos.EdgeDto;
import com.misterbil.racines.adapter.in.web.dto.Dtos.GraphDto;
import com.misterbil.racines.adapter.in.web.dto.Dtos.NodeDto;
import com.misterbil.racines.domain.model.NodeId;
import com.misterbil.racines.domain.port.in.GetGraph;
import com.misterbil.racines.domain.port.in.WriteGraph;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lecture/écriture de la carte (phase 0) + export JSON (phase 1). */
@RestController
@RequestMapping("/api")
public class GraphController {

    private final GetGraph getGraph;
    private final WriteGraph writeGraph;

    public GraphController(GetGraph getGraph, WriteGraph writeGraph) {
        this.getGraph = getGraph;
        this.writeGraph = writeGraph;
    }

    @GetMapping("/graph")
    public GraphDto graph() {
        return WebMapper.toGraph(getGraph.full());
    }

    @PostMapping("/nodes")
    public NodeDto createNode(@Valid @RequestBody CreateNodeRequest req) {
        Map<String, Object> extra = req.extra() == null ? Map.of() : req.extra();
        return WebMapper.node(writeGraph.addNode(WebMapper.nodeType(req.type()), req.label(), extra));
    }

    @PostMapping("/edges")
    public EdgeDto createEdge(@Valid @RequestBody CreateEdgeRequest req) {
        return WebMapper.edge(writeGraph.addEdge(
                WebMapper.edgeType(req.type()), NodeId.of(req.source()), NodeId.of(req.target())));
    }

    /** Export du cheminement (US9) : même contrat que /graph, en pièce jointe. */
    @GetMapping("/export")
    public ResponseEntity<GraphDto> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"racines-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(WebMapper.toGraph(getGraph.full()));
    }
}
