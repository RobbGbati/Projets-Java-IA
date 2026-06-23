package com.misterbil.racines.adapter.in.web;

import lombok.RequiredArgsConstructor;
import com.misterbil.racines.adapter.in.web.dto.Dtos.DepositRequest;
import com.misterbil.racines.adapter.in.web.dto.Dtos.ExtractRequest;
import com.misterbil.racines.adapter.in.web.dto.Dtos.GraphDto;
import com.misterbil.racines.domain.model.EntryDraft;
import com.misterbil.racines.domain.port.in.ConfirmProposal;
import com.misterbil.racines.domain.port.in.DepositEntry;
import com.misterbil.racines.domain.port.in.ProposeFromText;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dépôt d'entrées : structuré (phase 1) puis extraction libre + validation (phase 3). */
@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryController {

    private final DepositEntry depositEntry;
    private final ProposeFromText proposeFromText;
    private final ConfirmProposal confirmProposal;
    /** Phase 1 : saisie structurée → la carte grandit. */
    @PostMapping
    public GraphDto deposit(@RequestBody DepositRequest req) {
        EntryDraft draft = new EntryDraft(req.rawText(), req.sky(), req.emotion(), req.situation(),
                req.belief(), req.sensation(), req.need(), req.resource(), req.person());
        return WebMapper.toGraph(depositEntry.deposit(draft));
    }

    /** Phase 3 : texte libre → proposition de nœuds/relations (NON persistée). */
    @PostMapping("/extract")
    public GraphDto extract(@Valid @RequestBody ExtractRequest req) {
        return WebMapper.toGraph(proposeFromText.propose(req.text()));
    }

    /** Phase 3 : l'utilisateur a validé/corrigé → on fusionne dans la carte. */
    @PostMapping("/confirm")
    public GraphDto confirm(@RequestBody GraphDto validated) {
        return WebMapper.toGraph(confirmProposal.confirm(WebMapper.toProposal(validated)));
    }
}
