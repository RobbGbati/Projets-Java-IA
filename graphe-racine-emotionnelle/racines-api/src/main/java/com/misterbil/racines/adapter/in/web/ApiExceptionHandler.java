package com.misterbil.racines.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Traduit les entrées invalides (type/enum inconnu, id vide…) en 400 propre. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onBadInput(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                e.getMessage() == null ? "Requête invalide" : e.getMessage());
        pd.setProperties(Map.of("error", "bad_request"));
        return pd;
    }
}
