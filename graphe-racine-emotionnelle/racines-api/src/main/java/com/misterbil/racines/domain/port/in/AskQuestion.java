package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.Answer;

/** Cas d'usage : poser une question en langage naturel (phase 2, GraphRAG). */
public interface AskQuestion {
    Answer ask(String question);
}
