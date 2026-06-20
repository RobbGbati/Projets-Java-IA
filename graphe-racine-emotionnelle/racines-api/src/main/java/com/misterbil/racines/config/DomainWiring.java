package com.misterbil.racines.config;

import com.misterbil.racines.domain.model.RagSettings;
import com.misterbil.racines.domain.port.in.AskQuestion;
import com.misterbil.racines.domain.port.out.ChatPort;
import com.misterbil.racines.domain.port.out.EmbeddingPort;
import com.misterbil.racines.domain.port.out.GraphExtractor;
import com.misterbil.racines.domain.port.out.GraphStore;
import com.misterbil.racines.domain.service.ExtractionService;
import com.misterbil.racines.domain.service.GraphRagService;
import com.misterbil.racines.domain.service.GraphService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du domaine (SPEC §3). Le domaine n'a aucune annotation Spring ;
 * c'est ici qu'on l'instancie et qu'on branche chaque port OUT (fourni par un
 * adaptateur) sur les services. Un seul bean {@link GraphService} implémente
 * plusieurs ports IN : Spring l'injecte par n'importe laquelle de ses interfaces.
 */
@Configuration
public class DomainWiring {

    @Bean
    public RagSettings ragSettings(@Value("${racines.rag.top-k:6}") int topK,
                                   @Value("${racines.rag.max-hops:3}") int maxHops) {
        return new RagSettings(topK, maxHops);
    }

    /** Implémente GetGraph + WriteGraph + DepositEntry + FindCommonRoots. */
    @Bean
    public GraphService graphService(GraphStore store, EmbeddingPort embeddings) {
        return new GraphService(store, embeddings);
    }

    /** Implémente AskQuestion (pipeline GraphRAG). */
    @Bean
    public AskQuestion askQuestion(GraphStore store, EmbeddingPort embeddings,
                                   ChatPort chat, RagSettings settings) {
        return new GraphRagService(store, embeddings, chat, settings);
    }

    /** Implémente ProposeFromText + ConfirmProposal (extraction phase 3). */
    @Bean
    public ExtractionService extractionService(GraphExtractor extractor, GraphStore store,
                                               EmbeddingPort embeddings) {
        return new ExtractionService(extractor, store, embeddings);
    }
}
