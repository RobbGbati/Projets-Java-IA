package com.misterbil.racines.adapter.out.persistence.neo4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Au démarrage (profil Neo4j), crée la contrainte d'unicité sur {@code id} et
 * l'index vectoriel {@code racines_emb} (SPEC §4). Idempotent grâce à
 * {@code IF NOT EXISTS}. La dimension suit le modèle d'embedding choisi
 * ({@code racines.embedding-dimensions}).
 */
@Component
@ConditionalOnProperty(name = "racines.store", havingValue = "neo4j")
public class VectorIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexInitializer.class);

    private final Neo4jClient client;
    private final int dimensions;

    public VectorIndexInitializer(Neo4jClient client,
                                  @Value("${racines.embedding-dimensions:1536}") int dimensions) {
        this.client = client;
        this.dimensions = dimensions;
    }

    @Override
    public void run(ApplicationArguments args) {
        client.query("""
                CREATE CONSTRAINT racine_id IF NOT EXISTS
                FOR (n:%s) REQUIRE n.id IS UNIQUE
                """.formatted(Neo4jGraphStore.LABEL)).run();

        client.query("""
                CREATE VECTOR INDEX racines_emb IF NOT EXISTS
                FOR (n:%s) ON (n.embedding)
                OPTIONS { indexConfig: {
                    `vector.dimensions`: %d,
                    `vector.similarity_function`: 'cosine'
                }}
                """.formatted(Neo4jGraphStore.LABEL, dimensions)).run();

        log.info("Neo4j : contrainte id + index vectoriel 'racines_emb' (dim={}) prêts", dimensions);
    }
}
