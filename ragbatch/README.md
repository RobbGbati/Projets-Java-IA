# ragbatch — Job d'ingestion RAG vers PGVector

Le complément de `spring-ai-demo` : l'architecture d'ingestion **de production**, celle qu'un vector store en mémoire ne permet pas.

```
┌──────────────┐   écrit     ┌───────────────────┐    lit     ┌──────────────┐
│   ragbatch    │ ──────────► │ PostgreSQL         │ ◄───────── │ application  │
│   (ce job)    │             │ + pgvector         │            │ RAG          │
└──────────────┘             └───────────────────┘            └──────────────┘
 lit les fichiers,            chunks + embeddings               ne voit JAMAIS
 tourne PUIS s'arrête         persistants                       les fichiers
```

Dans `spring-ai-demo`, l'ingestion tourne au démarrage et tout vit en mémoire (`SimpleVectorStore`). Ici, un **job Spring Batch séparé** lit les fichiers, les découpe, calcule les embeddings et écrit le tout dans PostgreSQL. Le fichier source a fini son rôle après l'ingestion : ce qui vit en base, ce sont les chunks et leurs vecteurs. Une application RAG n'a plus qu'à faire des recherches de similarité — quelques chunks chargés par question, jamais le corpus entier.

## Prérequis

| Outil | Détail |
|---|---|
| JDK 21, Maven 3.9+ | comme spring-ai-demo |
| Docker | daemon démarré (OrbStack/Docker Desktop) — PostgreSQL est lancé automatiquement |
| Ollama | `ollama pull nomic-embed-text` (mêmes embeddings que la démo) |

Versions : Spring Boot 3.5.x + Spring AI **1.1.7** — alignées sur `spring-ai-demo`. Indispensable côté embeddings : la base n'est interrogeable qu'avec le **même modèle d'embeddings** que celui de l'ingestion.

## Lancer le job

```bash
mvn spring-boot:run
```

Ce qui se passe, dans l'ordre :

1. `spring-boot-docker-compose` démarre PostgreSQL+pgvector (`compose.yaml`) s'il ne tourne pas déjà (`start-only` : la base survit au job).
2. Spring AI crée la table `vector_store` (+ extension `vector`, index HNSW) ; Spring Batch crée ses tables de métadonnées `BATCH_*`. Le tout dans la même base `ragdb`.
3. Le job `ingestionJob` s'exécute : **purge → ingest → verify**, puis l'application s'arrête (pas de serveur web : c'est un batch).

## Les 3 steps

| Step | Type | Rôle |
|---|---|---|
| `purgeStep` | tasklet | vide `vector_store` — relancer le job ne duplique rien (stratégie "full reload") |
| `ingestStep` | chunk-oriented | un fichier par item : `ListItemReader` (fichiers de `docs/`) → `FileToChunksProcessor` (TextReader + TokenTextSplitter) → `VectorStoreWriter` (embedding + INSERT) |
| `verifyStep` | tasklet | recherche de similarité de contrôle ; aucun résultat → job FAILED |

Le pipeline ETL est le même que dans `RagConfig` de la démo (Extract → Transform → Load), mais découpé selon les contrats Spring Batch : transactions par paquets (`chunk(2)`), reprise sur erreur, et chaque exécution historisée.

## Inspecter la base

```bash
# Les chunks et leurs sources
docker exec ragbatch-postgres-1 psql -U rag -d ragdb \
  -c "SELECT metadata->>'source' AS source, left(content, 60) FROM vector_store;"

# L'historique des exécutions du job (le vrai apport de Spring Batch)
docker exec ragbatch-postgres-1 psql -U rag -d ragdb \
  -c "SELECT job_execution_id, status, start_time FROM batch_job_execution;"

# La recherche de similarité, en SQL brut — ce que fait vectorStore.similaritySearch()
# (l'opérateur <=> est la distance cosinus de pgvector)
```

## Exercices

1. **Ajoute un fichier** dans `docs/`, relance le job : il est ingéré sans toucher au code.
2. **Casse la vérification** : vide `docs/` et relance — observe le job FAILED et son statut dans `BATCH_*`.
3. **Ingestion incrémentale** : remplace la purge totale par une suppression ciblée des chunks dont le `metadata->>'source'` correspond aux fichiers ré-ingérés.
4. **Branche un client** : pointe le `/rag` d'une app Spring AI sur cette base (starter pgvector + même config embeddings) — le code contrôleur de `spring-ai-demo` fonctionne tel quel, seule la config change.
