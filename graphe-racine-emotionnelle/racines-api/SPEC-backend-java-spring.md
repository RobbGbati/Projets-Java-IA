# SPEC technique — `racines-api` (Java / Spring Boot)

> Spécification du *comment* pour le backend. À lire avec `PRD-Les-Racines.md` (le *quoi*).
> Cible Claude Code : ce fichier décrit l'architecture, les ports, le modèle, le pipeline GraphRAG, les endpoints et le Docker. Construis **phase par phase** (voir §10).

---

## 1. Stack

| Brique | Choix | Rôle |
|---|---|---|
| Langage | Java 21 (LTS) | — |
| Framework | Spring Boot (dernière stable) | REST, DI, config |
| Données graphe | **Neo4j 5.26 LTS** (ou 2026.x) | graphe **et** index vectoriel natif (≥ 5.13) |
| Accès données | Spring Data Neo4j | `@Node` / `@Relationship`, repositories |
| IA | **Spring AI** | `ChatClient` (génération) + `EmbeddingModel` (vecteurs) |
| Extraction (option) | LangChain4j `LLMGraphTransformer` | texte libre → nœuds + relations (phase 3) |
| Conteneurs | Docker + docker-compose | dev local & déploiement |

LLM/embeddings : via clé OpenAI **ou** Anthropic (Spring AI abstrait le fournisseur). Local-first possible avec Ollama si l'on veut zéro appel externe.

## 2. Architecture : hexagonale (légère) + DDD tactique

Décision : **architecture hexagonale** (ports & adaptateurs) comme squelette, **DDD tactique** (modèle riche, langage ubiquitaire) pour le cœur. **Pas** de cérémonie DDD lourde : un seul bounded context, pas d'agrégats élaborés, pas d'event sourcing.

Pourquoi l'hexagonale gagne ici : deux pièces d'infra sont **interchangeables** — le store graphe (Neo4j, mais aussi Postgres ou in-memory) et le LLM (OpenAI / Anthropic / Ollama via Spring AI). Les ports rendent ces échanges triviaux *et* rendent la logique GraphRAG testable sans vraie base ni vrai LLM.

### Paquets
```
domain/                 ← le cœur. AUCUN import Spring / Neo4j / Spring AI.
  model/                Emotion, Belief, Resource… + InnerGraph, SubGraph, CommonRoot, EntryDraft
  port/in/              cas d'usage (interfaces) : GetGraph, DepositEntry, AskQuestion…
  port/out/             besoins du domaine (interfaces) : GraphStore, EmbeddingPort, ChatPort…
  service/              la logique : implémente les ports IN, dépend des ports OUT

adapter/
  in/web/               controllers REST → appellent les ports IN ; mapping DTO
  out/persistence/      Neo4j (Spring Data) → implémente GraphStore  (+ InMemoryGraphStore en phase 0)
  out/ai/               Spring AI → implémente EmbeddingPort, ChatPort, GraphExtractor

config/                 le câblage Spring : quel adaptateur branché sur quel port (déclare les @Bean du domaine)
```

### Règle d'or
**Les dépendances pointent vers l'intérieur.** Un adaptateur dépend du domaine (il implémente ses interfaces) ; jamais l'inverse. Le paquet `domain/` ne contient aucune annotation framework. Conséquence directe et utile : on peut développer tout le domaine + le web avec un `InMemoryGraphStore`, puis brancher l'adaptateur Neo4j, puis l'adaptateur IA — **sans toucher au cœur**. Chaque phase = un adaptateur de plus.

## 3. Les ports (les interfaces que Claude Code doit créer en premier)

### Ports d'entrée — `domain/port/in/` (offerts par le domaine, appelés par le web)
```java
public interface GetGraph        { InnerGraph full(); }
public interface DepositEntry    { InnerGraph deposit(EntryDraft draft); }          // phase 1 (structuré)
public interface ProposeFromText { ExtractionProposal propose(String rawText); }    // phase 3
public interface ConfirmProposal { InnerGraph confirm(ExtractionProposal validated);}// phase 3
public interface AskQuestion     { Answer ask(String question); }                   // phase 2
public interface FindCommonRoots { List<CommonRoot> find(); }                        // US7
```

### Ports de sortie — `domain/port/out/` (besoins du domaine, fournis par les adaptateurs)
```java
public interface GraphStore {                 // impl : Neo4j  | Postgres | InMemory
    InnerGraph load();
    void apply(GraphChange change);
    List<NodeRef> vectorSearch(float[] embedding, int topK);   // phase 2
    SubGraph traverse(Collection<NodeId> anchors, int maxHops); // phase 2
    List<CommonRoot> commonRoots();                             // US7
}
public interface EmbeddingPort { float[] embed(String text); }                 // impl : Spring AI
public interface ChatPort      { String generate(String system, String context, String question); } // impl : Spring AI
public interface GraphExtractor{ ExtractionProposal extract(String rawText, GraphSchema schema); }   // phase 3, impl : Spring AI / LangChain4j
```
> `GraphStore` regroupe les opérations « du store » (graphe + vecteurs) parce que Neo4j fait les deux. Si ça grossit, on peut le scinder (`GraphStore` / `VectorSearchPort`) — pas avant d'en avoir besoin.

### Où vit la logique GraphRAG — `domain/service/`
```java
// implémente AskQuestion, ne connaît que des interfaces → aucun import infra
public class GraphRagService implements AskQuestion {
    private final GraphStore graph;
    private final EmbeddingPort embeddings;
    private final ChatPort chat;
    private final RagSettings cfg;            // topK, maxHops

    public Answer ask(String question) {
        float[] q          = embeddings.embed(question);
        List<NodeRef> anch = graph.vectorSearch(q, cfg.topK());
        SubGraph context   = graph.traverse(ids(anch), cfg.maxHops());
        String prompt      = ContextBuilder.from(context);          // sous-graphe → texte
        String answer      = chat.generate(SYSTEM_DOUX, prompt, question);
        return new Answer(answer, context);                          // réponse + sous-graphe
    }
}
```

### Câblage — `config/`
Le domaine reste sans Spring ; on l'instancie dans une `@Configuration` :
```java
@Configuration
class DomainWiring {
    @Bean AskQuestion askQuestion(GraphStore g, EmbeddingPort e, ChatPort c, RagSettings s) {
        return new GraphRagService(g, e, c, s);
    }
    // idem pour DepositEntry, ProposeFromText, ConfirmProposal, FindCommonRoots…
}
```
Les adaptateurs (`Neo4jGraphStore`, `SpringAiChat`, …) sont des `@Component`/`@Repository` classiques.

## 4. Modèle de domaine & persistance

### Nœuds (modèle de domaine, en anglais ; libellés affichés côté front)
- `Emotion { id, label, valence, createdAt }` — peur, colère, honte, joie…
- `Situation { id, label, createdAt }` — un déclencheur (« une réunion »)
- `Belief { id, text, createdAt }` — pensée/croyance (« je ne suis pas à la hauteur »)
- `Sensation { id, label }` — corporelle (« boule au ventre »)
- `Need { id, label }` — sécurité, reconnaissance, repos…
- `Resource { id, label, kind }` — ce qui apaise (respiration, marche, verset, un proche)
- `Person { id, alias }` — **anonymisée** (« P. ») — jamais de nom réel
- `Entry { id, rawText, sky, createdAt }` — le texte source daté + l'humeur

### Relations
```
(Situation)-[:TRIGGERS]->(Emotion)
(Emotion)  -[:FED_BY]->(Belief)
(Emotion)  -[:EXPRESSED_AS]->(Sensation)
(Belief)   -[:TOUCHES]->(Need)
(Resource) -[:SOOTHES]->(Emotion)
(Entry)    -[:MENTIONS]->(:Emotion|:Situation|:Belief|...)   // traçabilité vers la source
```

### Le point de friction à connaître : modèle de domaine vs entité de persistance
Spring Data Neo4j veut poser `@Node` / `@Relationship` sur les classes. Le purisme hexagonal voudrait un **modèle de domaine pur** (`domain/model/Belief`) **+** une **entité de persistance annotée** (`adapter/out/persistence/BeliefEntity`) avec un mapper entre les deux.

**Reco pragmatique pour le v1 (solo, en apprentissage)** : ne paie pas tout de suite la taxe du mapping. Au choix :
- (a) tolère les annotations `@Node` sur le modèle de domaine pour démarrer, et sépare plus tard si ça te gêne ; **ou**
- (b) ne sépare que les 2-3 nœuds qui portent une vraie logique.

L'`InMemoryGraphStore` de la phase 0, lui, n'a aucune annotation : il manipule directement le modèle de domaine. C'est le meilleur garde-fou contre le couplage prématuré.

### Embeddings
Les nœuds « parlants » (Emotion, Situation, Belief, Resource) portent `embedding: float[]`, indexé par un **vector index** Neo4j (recherche d'ancres, phase 2) :
```cypher
CREATE VECTOR INDEX racines_emb IF NOT EXISTS
FOR (n:Belief) ON (n.embedding)
OPTIONS { indexConfig: { `vector.dimensions`: 1536, `vector.similarity_function`: 'cosine' } };
```
(Un index par label concerné, ou un label commun `:Embeddable`.)

## 5. Le pipeline GraphRAG (dans `domain/service/`)

### 5.1 Interrogation (phase 2) — `GraphRagService.ask` (voir §3)
```
1. embeddings.embed(question)              → vecteur
2. graph.vectorSearch(vecteur, topK)       → nœuds-ancres
3. graph.traverse(ancres, maxHops)         → sous-graphe de contexte
4. ContextBuilder.from(sous-graphe)        → texte structuré
5. chat.generate(SYSTEM_DOUX, …)           → réponse
6. retour Answer{ answer, subgraph }
```
- **Prompt système (`SYSTEM_DOUX`)** : impose le ton — invitation, jamais verdict ; pas de conseil médical ; si détresse, suggérer une aide humaine.
- La réponse renvoie *toujours* le sous-graphe pour que le front le surligne.

### 5.2 Extraction (phase 3) — `ExtractionService` (implémente `ProposeFromText` + `ConfirmProposal`)
1. `graph.extractor.extract(rawText, schema)` avec **schéma contraint** + few-shot, température basse → `{ nodes, edges }`.
2. **Ne persiste pas** : renvoie une `ExtractionProposal` au front pour validation (consentement, PRD §3.4).
3. À la confirmation : fusion via `graph.apply(change)`, avec **dédoublonnage** par similarité de label/embedding (pas 10× « anxiété »).
> L'adaptateur `out/ai` peut s'appuyer sur `LLMGraphTransformer` (LangChain4j) pour l'étape 1.

### 5.3 Racine commune — `FindCommonRoots` (requête pure graphe, sans LLM)
```cypher
MATCH (s1:Situation)-[:TRIGGERS]->(:Emotion)-[:FED_BY]->(b:Belief)
      <-[:FED_BY]-(:Emotion)<-[:TRIGGERS]-(s2:Situation)
WHERE id(s1) < id(s2)
RETURN b AS root, collect(distinct s1)+collect(distinct s2) AS situations
```
Le LLM n'intervient qu'ensuite, pour *formuler* la révélation en douceur.

## 6. Endpoints REST — adaptateur `adapter/in/web/`

Les controllers ne contiennent **aucune logique** : ils mappent le HTTP vers un port IN, et le résultat vers un DTO. Jamais d'entité `@Node` exposée.

| Méthode | Route | Port appelé | Phase |
|---|---|---|---|
| GET | `/api/graph` | `GetGraph` | 0 |
| POST | `/api/nodes` · `/api/edges` | (écriture directe) | 0 |
| POST | `/api/entries` | `DepositEntry` | 1 |
| POST | `/api/entries/extract` | `ProposeFromText` | 3 |
| POST | `/api/entries/confirm` | `ConfirmProposal` | 3 |
| POST | `/api/ask` | `AskQuestion` | 2 |
| GET | `/api/insights/common-roots` | `FindCommonRoots` | 2 |
| GET | `/api/export` | `GetGraph` (→ JSON) | 1 |

Contrats clés :
```jsonc
// GET /api/graph
{ "nodes": [ { "id", "type", "label", "extra": {} } ],
  "edges": [ { "id", "type", "source", "target" } ] }

// POST /api/ask →
{ "answer": "…ton doux, fondé sur la carte…",
  "subgraph": { "nodes": [...], "edges": [...] } }
```

## 7. Configuration (`config/`, `application.yml`)

```yaml
spring:
  neo4j:
    uri: ${SPRING_NEO4J_URI:bolt://localhost:7687}
    authentication:
      username: ${SPRING_NEO4J_AUTHENTICATION_USERNAME:neo4j}
      password: ${SPRING_NEO4J_AUTHENTICATION_PASSWORD:password}
  ai:
    openai:                      # ou anthropic / ollama
      api-key: ${LLM_API_KEY:}
      chat.options.model: ${LLM_CHAT_MODEL:gpt-4o-mini}
      embedding.options.model: ${LLM_EMBED_MODEL:text-embedding-3-small}
racines:
  rag:
    top-k: 6
    max-hops: 3
  store: ${RACINES_STORE:neo4j}   # neo4j | inmemory  → choisit l'adaptateur GraphStore
```
- `racines.store` permet de **basculer l'adaptateur** (in-memory en phase 0/tests, Neo4j ensuite) via `@ConditionalOnProperty`.
- CORS : autoriser l'origine du front (`http://localhost:5173` en dev).

## 8. Docker

`Dockerfile` (multi-étapes : build Maven → image JRE légère) et `docker-compose.yml` orchestrant `neo4j` + `backend`.
Points clés à respecter :
- l'appli joint la base via le **nom de service** `bolt://neo4j:7687`, jamais `localhost` ;
- `healthcheck` sur Neo4j + `depends_on: condition: service_healthy` ;
- volume nommé sur `/data` pour persister le graphe ;
- secrets (mot de passe, clé LLM) via `.env`, pas en clair.

```yaml
services:
  neo4j:
    image: neo4j:5.26-community
    ports: ["7474:7474", "7687:7687"]
    environment:
      NEO4J_AUTH: neo4j/${NEO4J_PASSWORD}
    volumes: ["neo4j_data:/data"]
    healthcheck:
      test: ["CMD","cypher-shell","-u","neo4j","-p","${NEO4J_PASSWORD}","RETURN 1"]
      interval: 10s
      timeout: 5s
      retries: 10
  backend:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_NEO4J_URI: bolt://neo4j:7687
      SPRING_NEO4J_AUTHENTICATION_USERNAME: neo4j
      SPRING_NEO4J_AUTHENTICATION_PASSWORD: ${NEO4J_PASSWORD}
      RACINES_STORE: neo4j
      LLM_API_KEY: ${LLM_API_KEY}
    depends_on:
      neo4j: { condition: service_healthy }
volumes:
  neo4j_data:
```

## 9. Sécurité & vie privée

- **Local-first** par défaut. Si déploiement distant : chiffrer au repos, et n'envoyer au LLM **que le sous-graphe nécessaire**, pas tout le journal (l'adaptateur `out/ai` est le seul endroit qui sort des données → point de contrôle unique).
- Jamais de nom réel sur `Person` : alias uniquement.
- Pas d'authentification en v1 si mono-utilisateur local ; prévoir un `userId` dès le modèle pour le multi-utilisateur futur.

## 10. Ordre de construction (pour Claude Code)

Chaque phase ajoute **un adaptateur**, le cœur ne bouge pas.

1. **Phase 0** — `domain/model` + `domain/port` + un `InMemoryGraphStore` (adapter) + `GetGraph` + controllers `/api/graph`, `/api/nodes|edges`. Aucune base, aucune IA. Données semées.
2. **Phase 1** — `DepositEntry` (structuré) + `Neo4jGraphStore` (bascule `racines.store=neo4j`) + Neo4j en Docker + `/api/entries`, `/api/export`.
3. **Phase 2** — adaptateur `out/ai` (`EmbeddingPort`, `ChatPort`) + vector index + `GraphRagService` + `/api/ask`, `/api/insights/common-roots`.
4. **Phase 3** — `GraphExtractor` + `ExtractionService` (`ProposeFromText`/`ConfirmProposal`) + dédoublonnage + `/api/entries/extract`, `/confirm`.

Conventions : DTO en sortie ; `@Transactional` sur les écritures multi-nœuds ; **tests unitaires du domaine avec des ports bouchonnés** (mock `GraphStore`/`ChatPort`) — c'est tout l'intérêt de l'hexagonale.
Suggestion : un `CLAUDE.md` à la racine du repo pointant vers ce fichier + le PRD, rappelant la règle d'or (**dépendances vers l'intérieur, domaine sans framework**) et ces 4 phases.
