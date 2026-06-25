# racines-api — repères pour Claude Code

Backend GraphRAG des **Racines** (graphe émotionnel + LLM). Lire `SPEC-backend-java-spring.md` (le *comment*) et `../docs/PRD-Les-Racines.md` (le *quoi*).

## Règle d'or (non négociable)
**Les dépendances pointent vers l'intérieur.** Le paquet `domain/` ne contient **aucune** annotation framework (ni Spring, ni Neo4j, ni Spring AI). Un adaptateur dépend du domaine ; jamais l'inverse.

## Carte des paquets
```
domain/model      records purs (Node, Edge, InnerGraph, EntryDraft, Answer…)
domain/port/in    cas d'usage (GetGraph, DepositEntry, AskQuestion, FindCommonRoots, ProposeFromText, ConfirmProposal, WriteGraph)
domain/port/out   besoins du domaine (GraphStore, EmbeddingPort, ChatPort, GraphExtractor)
domain/service    la logique (GraphService, GraphRagService, ExtractionService, ContextBuilder, SystemPrompts)
adapter/in/web    controllers REST + DTO + mapper (aucune logique)
adapter/out/persistence/inmemory  InMemoryGraphStore (+ SeedData) — phase 0, sans base
adapter/out/persistence/neo4j     Neo4jGraphStore (Neo4jClient) + VectorIndexInitializer — phase 1+
adapter/out/ai    SpringAiEmbedding / SpringAiChat / SpringAiGraphExtractor
config            DomainWiring (@Bean du domaine), WebCorsConfig
```

## Les 4 phases (chaque phase = un adaptateur de plus, le cœur ne bouge pas)
- **Phase 0** — domaine + `InMemoryGraphStore` + `/api/graph`, `/api/nodes`, `/api/edges`. Aucune base, aucune IA. Données semées.
- **Phase 1** — `DepositEntry` + `Neo4jGraphStore` (bascule `racines.store=neo4j`) + Docker + `/api/entries`, `/api/export`.
- **Phase 2** — adaptateur `out/ai` + index vectoriel + `GraphRagService` + `/api/ask`, `/api/insights/common-roots`.
- **Phase 3** — `GraphExtractor` + `ExtractionService` + dédoublonnage + `/api/entries/extract`, `/confirm`.

## Choix d'implémentation à connaître
- **Modèle générique** : un seul `Node` typé par enum `NodeType` (pas une classe par label). Un seul mapping, logique simple.
- **Neo4j via `Neo4jClient`** (Cypher), pas l'OGM `@Node` : le domaine reste sans annotation. Un label `:RacineNode` + propriété `type` (évite APOC pour les labels dynamiques).
- **Adaptateurs IA résilients** : sans clé/Ollama, `embed` renvoie `[]` et `chat` un message de repli → les phases 0/1 tournent **hors-ligne**.
- **Fournisseurs Spring AI découplés** : chat = `LLM_PROVIDER` (`openai` | `ollama` | `anthropic`), embedding = `EMBEDDING_PROVIDER` (défaut = `LLM_PROVIDER`). Anthropic = chat seul (pas d'embedding) → combo conseillé `chat=anthropic` + `embedding=ollama` (local, gratuit). Changer de fournisseur = config, pas de code (l'adaptateur `SpringAiChat`/`ChatPort` est inchangé).
- **Bascule de store** via `racines.store` (`inmemory` par défaut | `neo4j`) et `@ConditionalOnProperty`.

## Lancer
```bash
# Phase 0/1 sans base ni IA (in-memory, données semées) :
mvn spring-boot:run
curl localhost:8080/api/graph
curl localhost:8080/api/insights/common-roots

# Avec Neo4j + IA :
cp .env.example .env   # renseigner NEO4J_PASSWORD + LLM_API_KEY
docker compose up --build
```

## Conventions
DTO en sortie (jamais d'entité exposée) · `@Transactional` sur les écritures multi-nœuds · tests du domaine avec ports bouchonnés (Mockito).
