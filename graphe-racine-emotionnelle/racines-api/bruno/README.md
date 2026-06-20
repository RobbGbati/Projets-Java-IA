# Collection Bruno — racines-api

Collection de test des endpoints des Racines, organisée par phase.

## Ouvrir
Bruno → *Open Collection* → sélectionner ce dossier `bruno/`.
Choisir l'environnement **Local** (`baseUrl = http://localhost:8080`).

## Pré-requis
Backend démarré :
```bash
# Sans IA ni base (phases 0/1) :
mvn spring-boot:run
# Ou stack complète (Neo4j + IA), depuis racines-api/ :
cp .env.example .env   # renseigner NEO4J_PASSWORD (+ LLM pour phases 2/3)
docker compose up --build
```

## Ordre conseillé
1. **Phase 0 - Graphe** — joue les requêtes 01→13 **dans l'ordre** (le runner Bruno
   respecte le `seq`). Les `Create node` enregistrent leurs ids en variables
   (`beliefId`, `situation1Id`…) réutilisées par les `Edge`. Construit le scénario
   « racine commune ». La 14 vérifie un 400.
2. **Phase 2 - GraphRAG** — `Common roots` marche dès la phase 0 jouée.
   `Ask` nécessite l'IA activée **et** des embeddings (les nœuds doivent avoir été
   créés avec l'IA active).
3. **Phase 1 - Dépôt** — dépôt structuré indépendant.
4. **Phase 3 - Extraction** — `Extract` (IA requise) puis `Confirm` (exemple prêt
   à l'emploi fourni ; en vrai, colle la sortie d'Extract validée).

## Note IA
Phases 2/3 dégradent proprement sans IA (réponse de repli, proposition vide).
Pour les activer : `LLM_PROVIDER=openai` (+ `LLM_API_KEY`) ou `LLM_PROVIDER=ollama`
(modèles `llama3.2` + `nomic-embed-text`, `RACINES_EMBEDDING_DIMENSIONS=768`).
