# racines-web — repères pour Claude Code

Frontend des **Racines** (carte vivante de la vie intérieure). Lire
`SPEC-frontend-react.md` (le *comment*) et `../docs/PRD-Les-Racines.md` (le *quoi*).

## Règle d'or (non négociable)
**La carte vivante et la révélation de racine commune sont la signature — tout le
reste reste calme.** Ne pas empiler les libs d'animation « parce qu'elles existent »
(SPEC §1) : Motion fait 90 % du travail ; GSAP seulement pour la révélation.

## Stack
React 19 + Vite + TypeScript · **Motion** (animations) · **GSAP** (révélation) ·
**d3-force** (layout) · **Zustand** (état de vue) · CSS Modules + tokens.

## Carte des dossiers (`src/`)
```
api/            client REST typé (client.ts) + types du contrat (types.ts) — SEUL à parler au backend
graph/          ForceLayout (d3-force), Garden (carte SVG pan/zoom), RootNode, RootEdge, Revelation (GSAP)
screens/        Deposer · Demander · Valider · Fil
sky/            SkyContext (humeur du jour → fond, cross-fade)
ui/             primitives douces (Field, SoftButton, Invitation)
theme/          tokens.css (palette), motion.ts (easing + helper reduced-motion)
store/          useGraphStore (Zustand : graphe, sélection, ciel, surlignage, fil, révélation)
```

## Contrat backend
`VITE_API_BASE` (défaut `http://localhost:8080`). Endpoints consommés :
`GET /api/graph`, `POST /api/entries`, `POST /api/ask`, `GET /api/insights/common-roots`,
`POST /api/entries/extract`, `POST /api/entries/confirm`, `GET /api/export`.
Types dans `src/api/types.ts`, alignés sur `racines-api/.../dto/Dtos.java`.

## Non négociable (PRD §3.8)
`prefers-reduced-motion` honoré partout (centralisé dans `theme/motion.ts` +
`tokens.css`) ; focus clavier visible ; nœuds atteignables au clavier ; responsive mobile.

## Lancer
```bash
npm install
npm run dev        # http://localhost:5173 (le backend doit tourner sur :8080)
npm run build      # tsc -b && vite build
```

## Voix (SPEC §8)
Inviter, jamais juger. Nommer par ce que vit l'utilisateur (« déposer », pas
« créer une entrée »). Sentence case partout, aucune CAPS. Jamais de ton clinique.

## Hors périmètre v1 (phase 5)
3D (React Three Fiber), pont vers *L'Arbre des pardons*, export PDF, versets.
```
