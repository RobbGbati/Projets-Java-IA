# Spring AI Demo — Intégrer un LLM dans un backend Spring Boot

Mini-projet pédagogique pour comprendre, brique par brique, comment un LLM s'intègre dans une application Java. Deux endpoints : un chat simple (le "hello world"), puis une sortie structurée (le vrai pattern d'entreprise).

L'objectif n'est pas de copier-coller : chaque section explique ce que Spring AI abstrait et ce qui se passe réellement à chaque appel.

---

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| JDK | 17 minimum (21 recommandé) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Fournisseur LLM | OpenAI (clé API) **ou** Ollama (local, gratuit) | voir section 6 |

Versions utilisées : Spring Boot 3.5.x et Spring AI **1.1.7**, la dernière version stable de la branche 1.1 (juin 2026). La 2.0 existe en Release Candidate mais n'est pas encore GA : pour apprendre, on reste sur du stable.

---

## 2. Vue d'ensemble du projet

```
spring-ai-demo/
├── pom.xml                          ← BOM Spring AI + les deux starters
└── src/main/
    ├── java/com/misterbil/springai/
    │   ├── SpringAiDemoApplication.java     ← point d'entrée classique
    │   ├── config/
    │   │   └── ChatClientConfig.java        ← le bean ChatClient + system prompt
    │   ├── chat/
    │   │   └── ChatController.java          ← Endpoint 1 : POST /chat
    │   └── analysis/
    │       ├── ReviewAnalysisController.java ← Endpoint 2 : POST /analyze
    │       └── ReviewAnalysis.java          ← le contrat de sortie typé
    └── resources/
        └── application.yml                  ← TOUTE la config LLM est ici
```

Remarque la chose la plus importante de cette arborescence : **aucune classe ne fait du HTTP vers un LLM**. Pas d'URL, pas de clé, pas de parsing JSON. C'est précisément ce que Spring AI abstrait.

---

## 3. Les briques, une par une

### 3.1 Le BOM (`spring-ai-bom`)

Un catalogue de versions importé dans `<dependencyManagement>`. Ensuite, toutes les dépendances `org.springframework.ai` se déclarent **sans numéro de version** : le BOM garantit leur compatibilité mutuelle. Même mécanisme que le BOM Spring Cloud.

### 3.2 Les starters (`spring-ai-starter-model-openai` / `-ollama`)

Chaque starter apporte trois choses :

1. le client bas niveau vers le fournisseur (auth, retry, sérialisation),
2. une implémentation de l'interface `ChatModel`,
3. **l'auto-configuration** : Spring lit `spring.ai.openai.*` (ou `spring.ai.ollama.*`) et crée les beans tout seul.

Le projet embarque les deux starters, et la propriété `spring.ai.model.chat` choisit lequel est actif. Un seul bean `ChatModel` existe à l'exécution.

### 3.3 `ChatModel` vs `ChatClient` — la distinction clé

| | Rôle | Analogie JDBC |
|---|---|---|
| `ChatModel` | Interface bas niveau : un `Prompt` entre, une `ChatResponse` sort | `DataSource` |
| `ChatClient` | API fluide de haut niveau, construite au-dessus | `JdbcClient` |

`ChatModel` est le **port** ; `OpenAiChatModel` et `OllamaChatModel` sont les **adapters**. Ton code dépend du port, jamais de l'adapter : c'est de l'architecture hexagonale appliquée par le framework lui-même. Changer de fournisseur ne touche pas une ligne de Java.

### 3.4 Le system prompt (dans `ChatClientConfig`)

Instruction permanente envoyée avant chaque message utilisateur — le "contrat de comportement" du modèle. Dans le JSON envoyé au fournisseur, il devient un message `role: "system"`. On le centralise dans le bean `ChatClient` pour qu'il s'applique à toute l'application.

### 3.5 Les prompt templates (dans `ReviewAnalysisController`)

Un texte à trous avec des `{placeholders}` remplis via `.param()`. C'est le `PreparedStatement` du prompt : on sépare la structure de l'instruction des données variables, et on ne concatène jamais du texte utilisateur dans l'instruction. Réflexe d'hygiène contre l'injection de prompt.

---

## 4. Le flux de bout en bout

Que se passe-t-il réellement quand tu appelles `POST /chat` ?

```
Client HTTP (curl)
   │  POST /chat {"message": "..."}
   ▼
ChatController                        ← ton code
   │  chatClient.prompt().user(...).call()
   ▼
ChatClient                            ← Spring AI
   │  assemble le Prompt : [system prompt par défaut] + [message user]
   ▼
ChatModel actif (OpenAI ou Ollama)    ← Spring AI
   │  traduit le Prompt au format JSON du fournisseur
   │  POST https://api.openai.com/v1/chat/completions
   │  (clé API en header, retry géré, erreurs typées)
   ▼
LLM ──────► réponse JSON
   │
   ▼
ChatModel : désérialise en ChatResponse (texte + métadonnées tokens)
   │
   ▼
.content() : extrait juste le texte → renvoyé au client
```

Deux choses à retenir :

- **Chaque appel est sans mémoire.** Le LLM ne se souvient de rien entre deux requêtes. Une conversation suivie implique de renvoyer l'historique à chaque appel (Spring AI propose des advisors `ChatMemory` pour ça — prochaine étape naturelle).
- **`.call()` est bloquant.** Il existe `.stream()` pour renvoyer la réponse token par token en SSE, comme l'interface de ChatGPT.

---

## 5. Endpoint 2 : pourquoi la sortie structurée plutôt qu'un mini-RAG ?

Les deux étaient de bons candidats. J'ai choisi la sortie structurée pour trois raisons :

1. **C'est le pattern le plus réutilisable.** Un `/chat` qui renvoie du texte libre, ton backend ne peut rien en faire. Avec une sortie typée, le LLM devient une fonction `String → ReviewAnalysis` : persistable, filtrable, branchable sur un workflow. C'est 80 % des intégrations LLM en entreprise : extraction, classification, routage.
2. **Zéro infrastructure en plus.** Le RAG exige un modèle d'embeddings et un vector store. La sortie structurée fonctionne à l'identique avec OpenAI ou Ollama, dans le même projet minimal.
3. **Elle enseigne le mécanisme le plus instructif** : voir le framework faire du prompt engineering à ta place (section suivante).

Le RAG est l'étape d'après, une fois ce socle maîtrisé.

### Ce que fait `.entity(ReviewAnalysis.class)` sous le capot

1. Spring AI génère un **schéma JSON** à partir du record (champs, types, enum) via un `BeanOutputConverter`.
2. Il **injecte ce schéma dans le prompt** avec la consigne "réponds uniquement en JSON conforme à ce schéma".
3. Le LLM répond en JSON brut.
4. **Jackson désérialise** vers le record. JSON invalide → exception. On échoue bruyamment plutôt que de propager un objet silencieusement faux.

Le record Java est donc **la source de vérité du contrat**, et le LLM est traité comme une source de données non fiable qu'on valide à la frontière. Si tu utilises Zod côté React, c'est exactement le même réflexe.

Limite honnête : l'étape 2 est une consigne, pas une garantie absolue. Les modèles récents la respectent très bien, mais en production on ajoute un retry ou une validation métier derrière.

**Pour le voir de tes yeux** : décommente le bloc `logging` dans `application.yml` et observe dans les logs le prompt réellement envoyé, schéma JSON inclus. C'est le meilleur exercice du projet.

---

## 6. Configuration de la clé API

### Option A — OpenAI (par défaut)

1. Crée une clé sur https://platform.openai.com/api-keys
2. Exporte-la en variable d'environnement (**jamais en dur dans un fichier versionné**) :

```bash
# Linux / macOS
export OPENAI_API_KEY="sk-..."

# Windows PowerShell
$env:OPENAI_API_KEY = "sk-..."
```

`application.yml` la lit via `${OPENAI_API_KEY}` au démarrage.

### Option B — Ollama, sans aucune clé (100 % local et gratuit)

1. Installe Ollama : https://ollama.com/download
2. Télécharge un modèle léger : `ollama pull llama3.2`
3. Dans `application.yml`, change une seule ligne :

```yaml
spring:
  ai:
    model:
      chat: ollama   # au lieu de openai
```

C'est tout. Aucun code Java ne change — c'est la démonstration concrète du point 3.3. Note simplement qu'un petit modèle local respecte moins fidèlement les sorties structurées qu'un modèle hébergé : si `/analyze` échoue parfois avec llama3.2, c'est le modèle, pas ton code.

---

## 7. Lancer et tester

```bash
mvn spring-boot:run
```

L'application démarre sur http://localhost:8080.

### Endpoint 1 — chat simple

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explique-moi l'\''inversion de contrôle en deux phrases."}'
```

Réponse attendue :

```json
{ "answer": "L'inversion de contrôle consiste à ..." }
```

### Endpoint 2 — sortie structurée

```bash
curl -X POST http://localhost:8080/analyze \
  -H "Content-Type: application/json" \
  -d '{"review": "Livraison rapide et produit conforme, mais le service client ne répond pas depuis une semaine concernant ma facture. Très déçu sur ce point."}'
```

Réponse attendue (générée par le LLM, validée par ton record) :

```json
{
  "sentiment": "NEUTRE",
  "estimatedRating": 3,
  "summary": "Client satisfait du produit mais bloqué par un service client injoignable.",
  "keyPoints": ["Livraison rapide", "Produit conforme", "Service client injoignable"],
  "needsSupportFollowUp": true
}
```

Relance le même appel deux fois : le texte varie (le LLM n'est pas déterministe), mais la **structure** est toujours identique. C'est tout l'intérêt.

---

## 8. Exercices pour ancrer le mécanisme

1. **Observe le prompt réel** : active les logs DEBUG et repère le schéma JSON injecté par `.entity()`.
2. **Casse le contrat** : ajoute un champ `String detectedLanguage` au record `ReviewAnalysis`, relance, et constate que le LLM le remplit sans que tu aies touché au prompt. Le type pilote le contrat.
3. **Change de fournisseur** : bascule sur Ollama et vérifie que les deux endpoints fonctionnent sans modifier le Java.
4. **Baisse la température** à 0.2 pour `/analyze` et compare la stabilité des réponses.

## 9. Pour aller plus loin (dans l'ordre)

1. **Streaming** : remplace `.call()` par `.stream()` et renvoie un `Flux<String>` en SSE.
2. **Mémoire de conversation** : advisors `ChatMemory` pour un chat multi-tours.
3. **RAG** : ajoute `spring-ai-starter-vector-store-*` + un modèle d'embeddings, ingère quelques documents, et laisse le LLM répondre à partir de TES données.
4. **Tool calling** : expose des méthodes Java que le LLM peut décider d'appeler.

Chaque étape réutilise exactement le `ChatClient` que tu maîtrises maintenant.
