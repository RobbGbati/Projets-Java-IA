/**
 * État de vue (Zustand). La source de vérité du graphe vient du backend ;
 * le front garde l'état de vue : sélection, ciel courant, sous-graphe surligné,
 * nœuds fraîchement nés (pour l'animation de croissance), et le Fil.
 */
import { create } from 'zustand';
import { api } from '../api/client';
import type { CommonRootDto, GraphDto } from '../api/types';

export type Screen = 'jardin' | 'deposer' | 'demander' | 'valider' | 'fil';

export interface FilItem {
  id: string;
  at: number;
  kind: 'depot' | 'revelation' | 'question';
  text: string;
}

interface Highlight {
  nodeIds: Set<string>;
  edgeIds: Set<string>;
}

interface GraphState {
  graph: GraphDto;
  status: 'idle' | 'loading' | 'ready' | 'error';
  error: string | null;

  screen: Screen;
  selectedNodeId: string | null;
  sky: string;
  highlight: Highlight | null;
  /** Nœuds nés depuis le dernier graphe — déclenchent l'animation de croissance. */
  freshNodeIds: Set<string>;
  fil: FilItem[];
  /** Révélation de racine commune en cours (séquence signature GSAP). */
  revelation: CommonRootDto | null;

  loadGraph: () => Promise<void>;
  /** Remplace le graphe en calculant les nouveaux nœuds (pour la croissance). */
  setGraph: (next: GraphDto) => void;
  goTo: (screen: Screen) => void;
  selectNode: (id: string | null) => void;
  setSky: (sky: string) => void;
  highlightSubgraph: (sub: GraphDto) => void;
  clearHighlight: () => void;
  addFil: (item: Omit<FilItem, 'id' | 'at'>) => void;
  setRevelation: (r: CommonRootDto | null) => void;
}

function diffNewNodes(prev: GraphDto, next: GraphDto): Set<string> {
  const before = new Set(prev.nodes.map((n) => n.id));
  return new Set(next.nodes.filter((n) => !before.has(n.id)).map((n) => n.id));
}

export const useGraphStore = create<GraphState>((set, get) => ({
  graph: { nodes: [], edges: [] },
  status: 'idle',
  error: null,

  screen: 'jardin',
  selectedNodeId: null,
  sky: 'ciel-paix',
  highlight: null,
  freshNodeIds: new Set(),
  fil: [],
  revelation: null,

  loadGraph: async () => {
    set({ status: 'loading', error: null });
    try {
      const graph = await api.getGraph();
      set({ graph, status: 'ready', freshNodeIds: new Set() });
    } catch (e) {
      set({ status: 'error', error: e instanceof Error ? e.message : 'erreur' });
    }
  },

  setGraph: (next) => {
    const fresh = diffNewNodes(get().graph, next);
    set({ graph: next, status: 'ready', freshNodeIds: fresh });
  },

  goTo: (screen) => set({ screen }),
  selectNode: (id) => set({ selectedNodeId: id }),
  setSky: (sky) => set({ sky }),

  highlightSubgraph: (sub) =>
    set({
      highlight: {
        nodeIds: new Set(sub.nodes.map((n) => n.id)),
        edgeIds: new Set(sub.edges.map((e) => e.id)),
      },
    }),
  clearHighlight: () => set({ highlight: null }),

  addFil: (item) =>
    set((s) => ({
      fil: [
        { ...item, id: crypto.randomUUID(), at: Date.now() },
        ...s.fil,
      ],
    })),

  setRevelation: (r) => set({ revelation: r }),
}));
