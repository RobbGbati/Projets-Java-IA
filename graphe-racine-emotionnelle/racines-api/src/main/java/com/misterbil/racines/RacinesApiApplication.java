package com.misterbil.racines;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Architecture hexagonale : le {@code domain/} ne connaît aucun framework.
 * Les adaptateurs ({@code adapter/out/persistence}, {@code adapter/out/ai},
 * {@code adapter/in/web}) implémentent ou appellent les ports. Le câblage
 * Spring vit dans {@code config/}. Voir CLAUDE.md + SPEC-backend-java-spring.md.</p>
 */
@SpringBootApplication
public class RacinesApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RacinesApiApplication.class, args);
    }
}
