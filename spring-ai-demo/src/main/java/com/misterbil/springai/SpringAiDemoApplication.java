package com.misterbil.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *   1. @SpringBootApplication déclenche l'auto-configuration.
 *   2. Le starter Spring AI détecte tes propriétés spring.ai.* dans
 *      application.yml.
 *   3. Il crée automatiquement un bean ChatModel (l'implémentation OpenAI
 *      ou Ollama selon spring.ai.model.chat) et un ChatClient.Builder.
 *   4. Tes contrôleurs reçoivent ces beans par injection de constructeur,
 *      exactement comme un Repository ou un Service.
 *
 * Le LLM devient une dépendance injectée comme une autre. C'est toute
 * l'idée : pas de SDK exotique, pas de client HTTP à écrire à la main.
 */
@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
