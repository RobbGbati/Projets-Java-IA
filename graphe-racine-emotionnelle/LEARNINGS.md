# Ce que ce projet apprend

**Les Racines** est un projet pédagogique : une app de bien-être émotionnel propulsée
par un moteur GraphRAG (graphe de connaissances + LLM). Backend Java/Spring
(`racines-api`), frontend React (`racines-web`). Ce document récapitule les
compétences et concepts que le projet permet d'apprendre.

## Concepts (le cœur)

- **GraphRAG** — pourquoi un graphe de connaissances dépasse la recherche
  vectorielle pure sur les questions « multi-sauts ». Exemple central : deux
  situations sans similarité textuelle (« réunion d'équipe », « dîner en
  famille ») qui pointent vers la même croyance — la *racine commune* qu'une
  recherche de texte ne verrait jamais.
- **Graphe de connaissances** — modéliser un domaine en nœuds typés + arêtes
  typées : `Emotion`, `Situation`, `Belief`, `Sensation`, `Need`, `Resource`,
  `Person`, `Entry` reliés par `TRIGGERS`, `FED_BY`, `EXPRESSED_AS`, `TOUCHES`,
  `SOOTHES`, `MENTIONS`.
- **Produit dirigé par le ton** — des contraintes non techniques (jamais
  culpabilisant, consentement à l'extraction, confidentialité, mouvement
  respectueux) qui pilotent réellement les décisions de code.

## Backend — `racines-api` (Java / Spring Boot)

- **Architecture hexagonale** — domaine pur sans annotation framework ; ports
  `in`/`out` ; adaptateurs. Règle d'or : les dépendances pointent vers
  l'intérieur (un adaptateur dépend du domaine, jamais l'inverse).
- **Neo4j via `Neo4jClient`** (Cypher) plutôt que l'OGM `@Node` — garder le
  domaine libre d'annotations. Un label `:RacineNode` + propriété `type`.
- **Spring AI** — embeddings + chat ; fournisseurs découplés (OpenAI / Ollama /
  Anthropic) ; adaptateurs **résilients** : sans clé, `embed` renvoie `[]` et
  `chat` un message de repli → l'app tourne **hors-ligne**.
- **Livraison par phases** — mécanique de bout en bout d'abord (phase 0 :
  graphe nu), intelligence ensuite (phases 2/3 : GraphRAG, extraction LLM).
  Chaque phase = un adaptateur de plus ; le cœur ne bouge pas.
- **Conventions** — DTO en sortie (jamais l'entité exposée) ; bascule de store
  in-memory ↔ Neo4j via `@ConditionalOnProperty` ; tests du domaine avec ports
  bouchonnés (Mockito).

## Frontend — `racines-web` (React 19 + Vite + TypeScript)

- **Contrat API typé centralisé** — `src/api/client.ts` est le seul module qui
  parle au backend ; le reste de l'app ne connaît que les types.
- **Visualisation force-directed** — `d3-force` pour le layout, SVG pour le
  rendu ; pan/zoom ; **drag de nœuds** (épingler `fx/fy` + réchauffer la
  simulation, distinguer clic et glissé par un seuil de mouvement).
- **Animation déclarative** — Motion pour le cycle de vie des nœuds
  (naissance, respiration), GSAP pour la séquence signature (révélation de
  racine commune). Discipline : ne pas empiler les libs d'animation « parce
  qu'elles existent ».
- **Routing** — react-router, une URL dédiée par vue ; pages uniques.
- **État** — Zustand pour l'état de vue (sélection, ciel, surlignage) ; la
  source de vérité du graphe reste le backend.
- **Accessibilité** — `prefers-reduced-motion` honoré partout (centralisé),
  focus clavier visible, rôles ARIA, responsive. Un plancher, pas une option.
- **Design** — tokens de couleur/typo + CSS Modules ; code couleur sémantique
  par type de nœud + légende.

## Tests & outillage

- **Playwright (E2E)** — sélecteurs robustes (rôles, `aria-label`, texte) plutôt
  que classes CSS hachées ; simuler un drag SVG via `PointerEvent` ; modes
  `--headed` / `--ui` pour observer ; le mode UI exige le Chromium intégré
  (`npx playwright install chromium`, sans sudo sur macOS).
- **Capture d'écrans automatisée** — un script Playwright qui parcourt chaque
  route et enregistre les PNG (`npm run screenshots`).
- **Git discipliné** — commits conventionnels par unité logique ; `.gitignore`
  des artefacts (build, `test-results/`, `playwright-report/`).

## Méta (transférable à d'autres projets)

- Découper un produit IA en **phases livrables** au lieu de tout faire d'un coup.
- Séparer le *quoi / pourquoi* (PRD) du *comment* (specs techniques) — deux
  documents distincts, un par projet.
- Construire pour fonctionner **hors-ligne d'abord** (replis gracieux), l'IA
  externe en option avec consentement.
- Laisser les **contraintes produit** (ton, confidentialité, accessibilité)
  guider l'architecture, pas seulement l'esthétique.

---

*Projet pédagogique — voir `docs/PRD-Les-Racines.md` (le quoi), les specs
techniques dans chaque repo (le comment), et les `CLAUDE.md` de chaque projet.*
