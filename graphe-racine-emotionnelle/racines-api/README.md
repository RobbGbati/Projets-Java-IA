# racines-api

Backend **GraphRAG** des *Racines* : un graphe émotionnel intérieur (situations,
émotions, croyances, besoins…) interrogeable en langage naturel via un LLM.

Spring Boot 3.5 · Java 21 · architecture **hexagonale** (ports & adapters) · Neo4j ·
Spring AI (OpenAI | Ollama | **Anthropic/Claude**).

> Pour le *comment* détaillé : `SPEC-backend-java-spring.md`. Pour le *quoi* (produit) :
> `../docs/PRD-Les-Racines.md`. Repères de contribution : `CLAUDE.md`.

---

## 1. En 2 minutes (zéro base, zéro IA)

Phase 0 : in-memory, données semées, hors-ligne. Aucune clé, aucun Docker.

```bash
mvn spring-boot:run
curl localhost:8080/api/graph
curl localhost:8080/api/insights/common-roots
```

L'appli démarre avec un graphe d'exemple en mémoire. Les endpoints IA (`/api/ask`,
`/api/entries/extract`) répondent par un message de repli tant qu'aucun fournisseur
n'est configuré — **c'est normal**, le cœur tourne sans IA.

Prérequis : **JDK 21** + Maven (ou le wrapper `./mvnw` si présent).

---

## 2. Architecture (la règle d'or)

**Les dépendances pointent vers l'intérieur.** Le paquet `domain/` ne contient
**aucune** annotation framework (ni Spring, ni Neo4j, ni Spring AI). Un adaptateur
dépend du domaine ; jamais l'inverse. Changer de base ou de fournisseur LLM = ajouter
ou configurer un adaptateur, le cœur ne bouge pas.

```
domain/model      records purs (Node, Edge, InnerGraph, Answer…)        ← aucun import framework
domain/port/in    cas d'usage (GetGraph, DepositEntry, AskQuestion…)
domain/port/out   besoins du domaine (GraphStorePort, ChatPort, EmbeddingPort, GraphExtractorPort)
domain/service    la logique (GraphService, GraphRagService, ExtractionService…)
adapter/in/web    controllers REST + DTO + mapper (aucune logique métier)
adapter/out/persistence/inmemory   InMemoryGraphStore (+ SeedData)  — phase 0
adapter/out/persistence/neo4j      Neo4jGraphStore (Neo4jClient)    — phase 1+
adapter/out/ai    SpringAiChat / SpringAiEmbedding / SpringAiGraphExtractor
config            DomainWiring (@Bean du domaine), WebCorsConfig
```

Pipeline GraphRAG (`/api/ask`) : `question → embed → vectorSearch (ancres) →
traverse (sous-graphe) → chat (LLM) → réponse + sous-graphe`.

---

## 3. Les 4 phases

Chaque phase = un adaptateur de plus, le cœur ne change pas.

| Phase | Apporte | Endpoints | Dépendances |
|------:|---------|-----------|-------------|
| **0** | domaine + `InMemoryGraphStore` | `/api/graph`, `/api/nodes`, `/api/edges`, `/api/insights/common-roots` | aucune |
| **1** | `DepositEntry` + `Neo4jGraphStore` | `/api/entries`, `/api/export` | Neo4j |
| **2** | adaptateur IA + index vectoriel + GraphRAG | `/api/ask` | Neo4j + LLM + embeddings |
| **3** | `GraphExtractor` + extraction LLM | `/api/entries/extract`, `/api/entries/confirm` | Neo4j + LLM |

---

## 4. Configuration (12-factor : tout par variable d'env)

Tout est surchargeable. Valeurs par défaut dans `src/main/resources/application.yml`.

### Bascule de store
| Variable | Valeurs | Défaut | Effet |
|---|---|---|---|
| `RACINES_STORE` | `inmemory` \| `neo4j` | `inmemory` | choisit l'adaptateur de persistance |

### Fournisseurs LLM (chat et embedding **découplés**)
Le **chat** et l'**embedding** se règlent séparément, car Anthropic ne fournit pas
d'embedding.

| Variable | Valeurs | Défaut | Rôle |
|---|---|---|---|
| `LLM_PROVIDER` | `none` \| `openai` \| `ollama` \| `anthropic` | `none` | fournisseur de **chat** |
| `EMBEDDING_PROVIDER` | `none` \| `openai` \| `ollama` | = `LLM_PROVIDER` | fournisseur d'**embedding** |
| `ANTHROPIC_API_KEY` | — | — | clé Claude (si chat=anthropic) |
| `LLM_API_KEY` | — | — | clé OpenAI (si chat ou embedding=openai) |
| `LLM_CHAT_MODEL` | — | selon fournisseur | modèle de chat |
| `LLM_EMBED_MODEL` | — | selon fournisseur | modèle d'embedding |
| `LLM_MAX_TOKENS` | — | `1024` | plafond tokens en sortie |
| `RACINES_EMBEDDING_DIMENSIONS` | `768` \| `1536`… | `768` | **doit matcher le modèle d'embedding** |
| `OLLAMA_BASE_URL` | URL | `http://localhost:11434` | daemon Ollama |

**Combo recommandé** (chat Claude payant + embeddings locaux gratuits) :
```bash
LLM_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
LLM_CHAT_MODEL=claude-opus-4-8
EMBEDDING_PROVIDER=ollama
LLM_EMBED_MODEL=nomic-embed-text
RACINES_EMBEDDING_DIMENSIONS=768   # 768 = nomic-embed-text · 1536 = OpenAI small
```

> Clé Anthropic : console.anthropic.com → Settings → API keys (crédits requis, pas de
> palier gratuit). Embeddings Ollama : `ollama serve` + `ollama pull nomic-embed-text`.

### Résilience
Sans clé / sans Ollama : `embed` renvoie `[]` et `chat` un message de repli. Les
phases 0/1 tournent donc **hors-ligne**. Le message de repli `/api/ask`
(« Je n'arrive pas à interroger ta carte… ») signale une IA non résolue.

---

## 5. Lancer

### A. Local sans base ni IA (phase 0)
```bash
mvn spring-boot:run
```

### B. Local dans IntelliJ avec Claude + Ollama
1. Lancer Ollama : `ollama serve` puis `ollama pull nomic-embed-text`.
2. Neo4j (optionnel, pour phases 1+) : `docker compose up neo4j`.
3. Run Configuration `RacinesApiApplication` → **Environment variables** (ou plugin
   *EnvFile* pour charger `.env`) :
   ```
   LLM_PROVIDER=anthropic;ANTHROPIC_API_KEY=sk-ant-...;EMBEDDING_PROVIDER=ollama;LLM_EMBED_MODEL=nomic-embed-text;RACINES_EMBEDDING_DIMENSIONS=768;RACINES_STORE=neo4j;SPRING_NEO4J_AUTHENTICATION_PASSWORD=...
   ```
   (En local, Ollama = `localhost`, pas besoin de `OLLAMA_BASE_URL`.)

### C. Tout en Docker (Neo4j + backend)
```bash
cp .env.example .env     # renseigner NEO4J_PASSWORD + ANTHROPIC_API_KEY
docker compose up --build
```

### Tests
```bash
mvn test                 # tests du domaine, ports bouchonnés (Mockito) — sans base ni IA
```

---

## 6. API REST

Base : `http://localhost:8080`. Contrat de sortie partout : `{ nodes, edges }`
(jamais d'entité de domaine exposée).

| Méthode | Chemin | Phase | Rôle |
|---|---|---|---|
| `GET` | `/api/graph` | 0 | carte complète |
| `POST` | `/api/nodes` | 0 | créer un nœud |
| `POST` | `/api/edges` | 0 | créer une arête |
| `GET` | `/api/insights/common-roots` | 0 | racines communes (US7) |
| `POST` | `/api/entries` | 1 | dépôt structuré → la carte grandit |
| `GET` | `/api/export` | 1 | export JSON (pièce jointe) |
| `POST` | `/api/ask` | 2 | question NL → réponse + sous-graphe |
| `POST` | `/api/entries/extract` | 3 | texte libre → proposition (non persistée) |
| `POST` | `/api/entries/confirm` | 3 | proposition validée → fusion dans la carte |

### Exemples

```bash
# Question GraphRAG (phase 2)
curl -s localhost:8080/api/ask -H 'Content-Type: application/json' \
  -d '{"question":"Quelles racines reviennent souvent ?"}'
# → { "answer": "...", "subgraph": { "nodes":[...], "edges":[...] } }

# Dépôt structuré (phase 1) — tous les champs sont optionnels
curl -s localhost:8080/api/entries -H 'Content-Type: application/json' \
  -d '{"rawText":"...","sky":"orageux","emotion":"peur","situation":"présentation",
       "belief":"je vais échouer","need":"sécurité"}'

# Extraction depuis texte libre (phase 3) — renvoie une proposition à valider
curl -s localhost:8080/api/entries/extract -H 'Content-Type: application/json' \
  -d '{"text":"Hier en réunion j'\''ai senti une boule au ventre..."}'
```

Collection **Bruno** prête à l'emploi dans `bruno/` (un dossier par phase).

---

## 7. Ré-embedder (changement de modèle d'embedding)

L'embedding est calculé **au dépôt** (`GraphService`). Aucun recalcul automatique des
vecteurs existants.

- **Store `inmemory`** : les données sont re-semées à chaque boot → **redémarrer** suffit.
- **Store `neo4j`** : vecteurs persistés + index figé à son ancienne dimension. Repartir
  propre :
  ```cypher
  // Neo4j Browser (http://localhost:7474)
  DROP INDEX racines_emb IF EXISTS;     // sinon l'index garde l'ancienne dimension
  MATCH (n:RacineNode) DETACH DELETE n; // vide le graphe
  ```
  Puis redémarrer l'appli (l'index est recréé à la nouvelle dimension par
  `VectorIndexInitializer`) et re-déposer via `POST /api/entries`.

> ⚠️ `RACINES_EMBEDDING_DIMENSIONS` doit toujours correspondre au modèle d'embedding
> actif, sinon l'index vectoriel Neo4j est incohérent.

---

## 8. Conventions

- DTO en sortie, jamais d'entité exposée.
- `@Transactional` sur les écritures multi-nœuds (adaptateur Neo4j).
- Tests du domaine avec ports bouchonnés (Mockito), sans base ni IA réelle.
- Neo4j via `Neo4jClient` (Cypher), pas l'OGM `@Node` → le domaine reste sans annotation.
  Un seul label `:RacineNode` + propriété `type`.
