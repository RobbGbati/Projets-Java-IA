package com.misterbil.racines.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Tous les DTO HTTP, regroupés. Le web ne renvoie JAMAIS d'entité de domaine ni
 * de nœud annoté : il mappe vers ces records (contrats §6 de la SPEC).
 */
public final class Dtos {

    private Dtos() {}

    // ---- communs (contrat { nodes, edges }) -----------------------------
    public record NodeDto(String id, String type, String label, Map<String, Object> extra) {}

    public record EdgeDto(String id, String type, String source, String target) {}

    public record GraphDto(List<NodeDto> nodes, List<EdgeDto> edges) {}

    // ---- écriture directe (phase 0) -------------------------------------
    public record CreateNodeRequest(@NotBlank String type, @NotBlank String label, Map<String, Object> extra) {}

    public record CreateEdgeRequest(@NotBlank String type, @NotBlank String source, @NotBlank String target) {}

    // ---- dépôt structuré (phase 1) --------------------------------------
    public record DepositRequest(
            String rawText, String sky,
            String emotion, String situation, String belief,
            String sensation, String need, String resource, String person) {}

    // ---- interrogation (phase 2) ----------------------------------------
    public record AskRequest(@NotBlank String question) {}

    public record AskResponse(String answer, GraphDto subgraph) {}

    public record CommonRootDto(NodeDto belief, List<NodeDto> situations, String revelation) {}

    // ---- extraction (phase 3) -------------------------------------------
    public record ExtractRequest(@NotBlank String text) {}
    // La confirmation réutilise GraphDto (proposition validée par l'utilisateur).
}
