# PRD — Les Racines

**Type** : app de bien-être émotionnel · **Catégorie** : Guérison · **Moteur** : GraphRAG (graphe de connaissances + LLM)
**Statut** : nouveau projet · app sœur de *L'Arbre des pardons*, même famille visuelle
**Principe fondateur** : *Le houppier montre ce qui pousse. Les racines révèlent ce qui relie. Elles ne jugent pas — elles relient.*

---

> **Note — PRD vs spécification.** Ce document est le **PRD** : il décrit le *quoi* et le *pourquoi*, indépendamment de la techno. Il est accompagné de deux **spécifications techniques** (le *comment*), une par projet :
> - `SPEC-backend-java-spring.md` → le projet **`racines-api`** (Java / Spring Boot)
> - `SPEC-frontend-react.md` → le projet **`racines-web`** (React)
>
> Pour développer avec Claude Code : dépose ce PRD à la racine des deux repos (ou dans un dossier `docs/` partagé), et la spec correspondante dans chaque repo.

---

## 1. Vision

Une carte vivante de la vie intérieure. L'utilisateur écrit librement, ou fait un court check-in. Au fil des jours, ses mots tissent un **réseau de racines** : ses émotions reliées à leurs situations, à leurs pensées, à ses besoins, et à ce qui l'a déjà apaisé. Puis il peut « interroger ses racines » en langage naturel — et le système, en **traversant** le graphe, fait surgir des liens qu'aucun journal linéaire ne montrerait : deux moments sans rapport apparent qui partagent une même racine.

Le tout dans une esthétique organique, douce, jamais clinique — fidèle à *L'Arbre des pardons*.

## 2. Problème / opportunité

- Les journaux émotionnels classiques **stockent** des entrées isolées ; ils ne **relient** rien. On relit, on ne comprend pas.
- Les apps de bien-être analysent par mots-clés ou humeurs moyennes — elles ratent la structure : *pourquoi* telle émotion revient, *à quelle racine* elle se rattache.
- Un graphe de connaissances émotionnel répond précisément aux questions « multi-sauts » : « ces deux poids partagent-ils une racine ? », « qu'est-ce qui m'a déjà apaisé quand cette émotion montait ? ». C'est exactement là que le GraphRAG dépasse la recherche de texte.
- Intersection rare : foi (versets en option), émotions, et une représentation organique vivante.

## 3. Principes de conception (non négociables)

1. **Jamais culpabilisant.** Aucune métrique de « performance émotionnelle », aucun streak. Une racine douloureuse n'est pas un échec.
2. **La carte témoigne, elle ne diagnostique pas.** Toute connexion révélée est une **invitation**, jamais un verdict (« un fil semble relier ces deux moments — est-ce que ça te parle ? »).
3. **Le design penche vers l'apaisement.** On ramène vers les ressources qui ont aidé, on ne fait pas ruminer la douleur. Surface les `Resource`, pas seulement les blessures.
4. **Consentement à l'extraction.** Le LLM *propose* les nœuds extraits ; l'utilisateur valide, corrige ou refuse. On ne met jamais de mots dans sa bouche.
5. **Confidentialité radicale.** Données très sensibles → local-first par défaut, personnes anonymisées, aucun cloud imposé. Si appel LLM externe, le strict nécessaire et avec consentement explicite.
6. **Sobriété & silence.** Pas de notifications harcelantes. Au retour après absence, accueil doux.
7. **Compagnon, pas thérapie.** Ce n'est pas un dispositif de soin. Pour les racines les plus profondes, l'app suggère avec délicatesse d'en parler à un proche ou à un professionnel.
8. **Mouvement respectueux.** `prefers-reduced-motion` honoré partout ; l'animation sert le sens, jamais le spectacle.

## 4. Utilisateurs cibles

- Personne en cheminement émotionnel et/ou spirituel, à l'aise avec une métaphore poétique.
- Quelqu'un qui tient (ou aimerait tenir) un journal mais veut *comprendre*, pas seulement archiver.
- Usage intime, ponctuel ou quotidien.

## 5. Le concept GraphRAG (niveau produit)

### Pourquoi un graphe
Une émotion ne flotte jamais seule : elle est **déclenchée** par une situation, **nourrie** par une pensée, **logée** dans une sensation, reliée à un **besoin**, et **apaisée** par certaines ressources. Ce sont des nœuds et des arêtes. Les questions les plus utiles sont des traversées, pas des recherches de texte.

### Le modèle conceptuel
- **Nœuds** : Émotion · Situation (déclencheur) · Croyance/Pensée · Sensation corporelle · Besoin · Ressource (ce qui apaise) · Personne (anonymisée) · Entrée (le texte source daté).
- **Relations** : *déclenche*, *nourrie par*, *s'exprime par*, *touche*, *apaise*, *mentionnée dans*.

### La « magie » de la racine commune
Quand deux situations sans similarité textuelle (« réunion d'équipe » et « dîner en famille ») déclenchent deux émotions qui **pointent vers la même croyance** (« je ne suis pas à la hauteur »), la traversée du graphe révèle la racine partagée. Une recherche vectorielle classique ne le verrait jamais. C'est le cœur de valeur de l'app.

### Les questions que l'app sait traiter
- « Quelle croyance revient le plus souvent sous mes moments de tristesse ? »
- « La dernière fois que la honte est montée, qu'est-ce qui m'a apaisé ? » → trousse de premiers secours personnalisée.
- « Ces deux poids que je porte partagent-ils une racine ? »

## 6. User stories

| # | En tant que… | je veux… | afin de… |
|---|---|---|---|
| US1 | utilisateur | déposer une entrée d'écriture libre ou guidée | confier ce que je traverse |
| US2 | utilisateur | choisir mon « ciel » (humeur du jour) | que l'ambiance reflète mon état |
| US3 | utilisateur | que l'app me propose les émotions/pensées repérées dans mes mots, et les valider | que ma carte soit juste et m'appartienne |
| US4 | utilisateur | voir mon réseau de racines grandir | sentir que mon intériorité prend forme |
| US5 | utilisateur | toucher un nœud pour lire son histoire | me souvenir du chemin |
| US6 | utilisateur | poser une question en langage naturel | recevoir une lumière douce, pas un diagnostic |
| US7 | utilisateur | qu'on me montre quand deux choses partagent une racine | comprendre ce qui me relie en profondeur |
| US8 | utilisateur | retrouver ce qui m'a déjà apaisé pour une émotion | savoir vers quoi revenir |
| US9 | utilisateur | exporter mon cheminement (JSON) | le garder, ou le partager avec un accompagnant |

## 7. Architecture d'ensemble (deux projets)

```
┌──────────────────────┐        REST / JSON        ┌──────────────────────────┐
│  racines-web (React) │  ───────────────────────▶ │  racines-api (Spring)    │
│  - carte vivante     │  ◀─────────────────────── │  - domaine + GraphRAG    │
│  - écriture / ciel   │     graphe, réponses      │  - Neo4j (graphe+vecteur)│
│  - révélations       │                           │  - Spring AI (LLM)       │
└──────────────────────┘                           └──────────────────────────┘
            (Vercel / Render statique)                   (Render / Railway / Docker)
```

- **Local-first** : en dev et pour la vie privée, le backend tourne en local (Docker) ; la base reste sur la machine de l'utilisateur tant que possible.
- Contrat d'échange : JSON simple `{ nodes, edges }` pour la visu, `{ answer, subgraph }` pour les questions. Détails dans les specs.

## 8. Découpage en étapes (chaque phase est livrable)

> Rythme « vendredi soir » : on construit la mécanique de bout en bout d'abord, l'intelligence ensuite, le paquet visuel pour finir. On apprend le GraphRAG là où il a du sens, sans tout faire d'un coup.

### Phase 0 — Le graphe nu (sans IA)
- **But** : voir un graphe s'afficher et se traverser, de bout en bout.
- **Backend** : Neo4j en Docker ; modèle `@Node`/`@Relationship` ; endpoints CRUD nœuds/arêtes ; un endpoint `GET /api/graph`.
- **Frontend** : la carte vivante (réseau SVG/force) qui affiche un graphe statique ; pan/zoom ; clic sur un nœud.
- **Fait quand** : on crée à la main 15-20 nœuds, ils s'affichent et se déplacent dans la carte.

### Phase 1 — Saisie & croissance (toujours sans IA)
- **But** : l'utilisateur saisit, la carte grandit.
- **Backend** : `POST /api/entries` (saisie *structurée* : on choisit soi-même émotion/situation/croyance) ; persistance ; calcul du graphe utilisateur.
- **Frontend** : écran « Déposer » + sélecteur de ciel ; animation de croissance d'une racine quand un nœud naît.
- **Fait quand** : déposer une entrée fait pousser de nouvelles racines, persistées au rechargement.

### Phase 2 — Interrogation GraphRAG
- **But** : poser une question et recevoir réponse + sous-graphe.
- **Backend** : Spring AI ; embeddings + index vectoriel Neo4j (ancres) ; traversée Cypher (contexte) ; génération de la réponse ; `POST /api/ask`.
- **Frontend** : écran « Demander » ; surlignage du sous-graphe pertinent ; réponse au ton doux.
- **Fait quand** : « qu'est-ce qui m'a apaisé quand j'étais en colère ? » renvoie une réponse fondée sur la carte.

### Phase 3 — Extraction automatique (le LLM tisse)
- **But** : écrire en texte libre, la carte se tisse seule.
- **Backend** : extraction LLM (entités + relations) depuis l'entrée libre, avec schéma contraint ; renvoi de propositions à valider.
- **Frontend** : écran de **validation douce** des entités proposées (accepter/corriger/refuser).
- **Fait quand** : un paragraphe libre produit des nœuds proposés que l'utilisateur valide en un geste.

### Phase 4 — Le paquet visuel
- **But** : une app *super friendly*, vivante, mémorable.
- **Frontend** : la **révélation de racine commune** animée (signature), respiration des nœuds, transitions de ciel, micro-interactions, soin de chaque microcopy ; perf + `prefers-reduced-motion`.
- **Fait quand** : la découverte d'une racine partagée provoque un moment visuel doux et marquant.

### Phase 5 — Profondeur & ponts
- **But** : aller plus loin une fois le socle solide.
- Racines en **3D** (React Three Fiber) ; **pont opt-in** avec *L'Arbre des pardons* (le houppier « montre » ce que les racines ont découvert) ; export PDF ; versets en option (Free Use Bible API).

## 9. Critères d'acceptation (produit)

- [ ] Déposer une entrée fait apparaître les bonnes racines, persistées.
- [ ] Aucun texte n'emploie un vocabulaire culpabilisant ; toute connexion est formulée comme invitation.
- [ ] Une question en langage naturel renvoie une réponse **fondée sur la carte** + le sous-graphe correspondant.
- [ ] Une racine commune entre deux situations est détectée et présentée avec douceur.
- [ ] L'extraction LLM passe toujours par une validation utilisateur.
- [ ] Les données survivent au rechargement ; export disponible.
- [ ] `prefers-reduced-motion` est respecté ; navigation clavier et focus visibles.

## 10. Risques & points d'attention

- **Sensibilité émotionnelle** : chaque microcopy testée auprès de vrais utilisateurs. Le ton prime sur la fonctionnalité.
- **Sur-analyse** : ne pas transformer l'app en machine à ruminer. Pencher vers les ressources et la douceur.
- **Données sensibles** : local-first ; si backend distant, chiffrer au repos ; anonymiser les personnes ; minimiser ce qui part vers le LLM.
- **Pas un dispositif médical** : le préciser, sans alourdir, et savoir orienter vers une aide humaine.
- **Coût/latence LLM** : préférer des appels ciblés ; cacher les embeddings ; envisager LazyGraphRAG/extraction par lots si le volume grandit.

## 11. Déploiement

- **Front** : Vercel ou Render (statique, 0 €).
- **Backend** : Docker en local (vie privée) ; pour une démo en ligne, Render (free, cold start accepté) ou Railway (~5 €/mo) ; ou backend conteneurisé + **Neo4j AuraDB** (tier gratuit managé).

## 12. Glossaire express

- **Nœud / racine** : une entité de la vie intérieure (émotion, situation, croyance…).
- **Arête** : un lien typé entre deux nœuds (*déclenche*, *apaise*…).
- **Ancre** : nœud trouvé par similarité vectorielle, point de départ d'une traversée.
- **Sous-graphe** : la portion de carte ramenée pour répondre à une question.
- **Ciel** : l'humeur du jour, qui colore l'ambiance.
