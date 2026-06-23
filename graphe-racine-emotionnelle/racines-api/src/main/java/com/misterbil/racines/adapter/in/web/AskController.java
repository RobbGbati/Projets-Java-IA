package com.misterbil.racines.adapter.in.web;

import lombok.RequiredArgsConstructor;
import com.misterbil.racines.adapter.in.web.dto.Dtos.AskRequest;
import com.misterbil.racines.adapter.in.web.dto.Dtos.AskResponse;
import com.misterbil.racines.domain.model.Answer;
import com.misterbil.racines.domain.port.in.AskQuestion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Interrogation GraphRAG (phase 2). Renvoie la réponse + le sous-graphe à surligner. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AskController {

    private final AskQuestion askQuestion;
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) {
        Answer answer = askQuestion.ask(req.question());
        return new AskResponse(answer.answer(), WebMapper.toGraph(answer.subgraph()));
    }
}
