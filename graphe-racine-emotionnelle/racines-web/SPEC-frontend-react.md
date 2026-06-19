# SPEC technique — `racines-web` (React)

> Spécification du *comment* pour le frontend, **axée visuel**. À lire avec `PRD-Les-Racines.md`.
> Objectif : une app *super friendly*, vivante, mémorable — pas une interface de plus. La carte de racines **est** le produit.

---

## 1. Stack

| Brique | Choix | Rôle |
|---|---|---|
| Base | **React 19 + Vite + TypeScript** | rapidité, typage du contrat API |
| Animation (cœur) | **Motion** (ex-Framer Motion, `motion.dev`) | animations déclaratives, `layout`, gestes |
| Layout du graphe | **d3-force** | disposition organique force-directed du réseau |
| Rendu du graphe | **SVG** (v1) → Canvas/WebGL si ça grossit | racines dessinées, animables au trait |
| Séquençage fin | **GSAP** (gratuit, tout inclus) | la « révélation » orchestrée (timeline) |
| Micro-interactions | **Rive** (optionnel) | un nœud qui respire/réagit, machine à états, léger |
| 3D (phase 5) | **React Three Fiber + Drei** | racines en volume |
| Transitions d'écran | **View Transitions API** | passages doux entre vues |
| Style | CSS Modules / vanilla-extract + tokens | l'organique est sur-mesure, pas utilitaire |

> Ne pas empiler les libs d'animation « parce qu'elles existent ». Motion fait 90 % du travail ; GSAP seulement pour la révélation ; Rive seulement si une micro-interaction à états le mérite.

## 2. Direction artistique

**Thèse visuelle (le hero)** : à l'ouverture, on ne voit pas un tableau de bord — on voit **un réseau de racines vivant** qui respire doucement sous une lumière de ciel. Tout part de là.

**L'élément signature** (la seule audace, soignée à fond) : **la révélation de racine commune**. Quand deux situations se révèlent reliées, deux racines s'illuminent et une nouvelle racine pousse vers un nœud partagé, avec une pulsation douce et une phrase-invitation. C'est le moment qu'on retient. Tout le reste est calme autour.

### Palette (cohérente avec *L'Atelier*)
```
--terre-profonde : #3a2c1e   /* sol, fond des racines */
--terre-douce    : #5c4630   /* racines, branches */
--racine-claire  : #8a6a44   /* tracé des racines */
--sauge          : #5a8f4e   /* vie, nœuds émotion positive */
--ocre           : #b98a2e   /* tension */
--corail         : #d85a30   /* LA racine commune — accent rare */
--ciel-paix      : #cfeaf1   /* humeurs (varient selon le ciel) */
--creme          : #faf4e8   /* surfaces d'écriture */
--encre          : #2b3326   /* texte principal */
```
Le corail est **rare** : réservé à la racine commune. C'est ce qui lui donne son poids.

### Typographie
- Display : **Fraunces** (italique pour les invitations douces, comme « la guérison n'est pas une ligne droite »).
- Corps / UI : **Spline Sans**.
- Aucune CAPS, sentence case partout, le ton est celui d'un ami sage.

## 3. Les écrans

1. **Le Jardin** (vue principale) — la carte de racines plein écran. Pan/zoom, les nœuds respirent, toucher un nœud ouvre son histoire. Le ciel colore le fond.
2. **Déposer** — écriture libre (phase 3) ou guidée (phase 1) + sélecteur de **ciel**. Surface crème, calme, généreuse.
3. **Valider** (phase 3) — les entités proposées par le LLM, à accepter / corriger / refuser, une par une, sans pression.
4. **Demander** — un champ unique. La réponse arrive douce ; le sous-graphe concerné s'illumine dans le Jardin.
5. **Le Fil** — journal chronologique des dépôts et révélations (lecture, pas métrique).

## 4. Le système d'animation (le paquet)

### Principes de mouvement
- **Lent et organique.** Easing type `cubic-bezier(.2,.8,.2,1)`, durées 0.5–1.2 s. Rien de sec.
- **Vivant au repos.** Les nœuds « respirent » (scale 1 → 1.03 en boucle lente, déphasés). La carte n'est jamais figée.
- **Le mouvement porte le sens**, jamais le décor. Une racine qui pousse = un lien qui se crée. Une pulsation corail = une racine commune.

### Animations clés
| Moment | Technique |
|---|---|
| Croissance d'une racine | tracé SVG animé (`stroke-dashoffset` via Motion) — la racine se *dessine* |
| Naissance d'un nœud | `scale` 0 → 1 avec léger overshoot (Motion spring) |
| Respiration des nœuds | boucle `scale`/`opacity` déphasée par index |
| Formation d'une arête | ligne qui se trace de la source vers la cible |
| **Révélation de racine commune** | GSAP timeline : 1) les 2 situations s'assombrissent autour, 2) leurs racines s'illuminent, 3) une racine corail pousse vers le nœud partagé, 4) pulsation + apparition de la phrase-invitation |
| Surlignage d'un sous-graphe (réponse) | le reste de la carte se désature, le sous-graphe garde sa couleur |
| Changement de ciel | cross-fade du fond (1.2 s), pas de coupure |
| Disposition | d3-force en continu, amorti ; les nœuds se replacent en douceur quand la carte grandit |

### Disposition force-directed
- `d3-force` calcule les positions ; **Motion** anime la transition entre deux états de layout (pas de saut).
- Forces : `forceLink` (arêtes), `forceManyBody` (répulsion douce), `forceCenter`, `forceCollide` (pas de chevauchement). Réglages doux pour un mouvement « sous l'eau ».

## 5. Accessibilité & performance (plancher de qualité)

- **`prefers-reduced-motion`** : respiration et tracés désactivés ou réduits à un fondu ; l'app reste pleinement utilisable. Non négociable (PRD §3.8).
- Focus clavier visible ; chaque nœud atteignable au clavier ; rôles ARIA sur la carte.
- Responsive jusqu'au mobile (la carte se recadre, pan tactile).
- Perf : SVG tant que < ~200 nœuds ; au-delà, basculer le rendu en Canvas (`react-force-graph`) sans changer le modèle.

## 6. Architecture composants

```
src/
  api/            client REST typé (GraphDto, AskResponse) — un seul endroit qui parle au backend
  graph/
    ForceLayout.ts        d3-force → positions
    Garden.tsx            la carte (SVG), pan/zoom, respiration
    RootEdge.tsx          une arête (tracé animé)
    RootNode.tsx          un nœud (respiration, focus, clic)
    Revelation.tsx        la séquence signature (GSAP)
  screens/
    Garden / Deposer / Valider / Demander / Fil
  sky/            contexte « ciel » (humeur) + fonds
  ui/             primitives douces (Field, SoftButton, Invitation)
  theme/          tokens couleur/typo, helpers reduced-motion
```

État : léger (Zustand ou Context + reducer). La source de vérité du graphe vient du backend ; le frontend garde l'état de vue (zoom, sélection, ciel courant).

## 7. Contrat API consommé

```ts
type GraphDto = {
  nodes: { id: string; type: NodeType; label: string; extra?: Record<string, unknown> }[];
  edges: { id: string; type: EdgeType; source: string; target: string }[];
};
type AskResponse = { answer: string; subgraph: GraphDto };
```
Un seul module `api/` ; le reste de l'app ne connaît que ces types (voir endpoints dans la spec backend).

## 8. Copy (la voix de l'app)

Les mots sont du matériau de design (cf. principes front). Toujours :
- inviter, jamais juger : « un fil semble relier ces deux moments — est-ce que ça te parle ? »
- nommer par ce que vit l'utilisateur, pas par la technique (« déposer », pas « créer une entrée »).
- les écrans vides invitent : « ta première racine attend tes mots. »
- jamais d'excuse mécanique ni de ton clinique.

## 9. Ordre de construction (pour Claude Code)

1. **Phase 0** : `api/`, `Garden` qui affiche un `GraphDto` statique via d3-force + Motion, pan/zoom, clic nœud.
2. **Phase 1** : écran `Deposer` + ciel ; animation de croissance d'une racine à la création.
3. **Phase 2** : écran `Demander` + surlignage du sous-graphe ; rendu de la réponse douce.
4. **Phase 3** : écran `Valider` (propositions LLM).
5. **Phase 4** : `Revelation` (GSAP), respiration, transitions de ciel, micro-interactions, passe complète `prefers-reduced-motion` + responsive.
6. **Phase 5** : variante 3D (R3F), pont opt-in vers *L'Arbre des pardons*.

Suggestion : un `CLAUDE.md` à la racine pointant vers ce fichier + le PRD, et rappelant la règle d'or : **la carte vivante et la révélation de racine commune sont la signature — tout le reste reste calme.**
