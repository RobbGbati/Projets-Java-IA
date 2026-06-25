/**
 * d3-force → positions. Disposition organique force-directed (SPEC §4).
 * Réglages doux pour un mouvement « sous l'eau ». Les objets-nœuds sont
 * réutilisés d'un calcul à l'autre : un nouveau nœud naît au centre, les autres
 * gardent leur place → la carte se replace en douceur, jamais de saut.
 *
 * prefers-reduced-motion : alphaDecay élevé → la simulation se fige presque
 * instantanément (positions calculées sans mouvement perceptible).
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  forceCenter,
  forceCollide,
  forceLink,
  forceManyBody,
  forceSimulation,
  type Simulation,
  type SimulationLinkDatum,
  type SimulationNodeDatum,
} from 'd3-force';
import type { GraphDto } from '../api/types';
import { prefersReducedMotion } from '../theme/motion';

export interface PositionedNode extends SimulationNodeDatum {
  id: string;
}
type Link = SimulationLinkDatum<PositionedNode>;

export type Positions = Record<string, { x: number; y: number }>;

export interface Layout {
  positions: Positions;
  /** Début de drag : épingle le nœud et réchauffe la simulation. */
  dragStart: (id: string) => void;
  /** Drag en cours : déplace le nœud (coordonnées monde). */
  dragMove: (id: string, x: number, y: number) => void;
  /** Fin de drag : la simulation se calme (le nœud reste épinglé). */
  dragEnd: () => void;
  /** Libère un nœud épinglé (il rejoint la disposition). */
  unpin: (id: string) => void;
}

export function useForceLayout(
  graph: GraphDto,
  width: number,
  height: number,
): Layout {
  const nodesRef = useRef<Map<string, PositionedNode>>(new Map());
  const simRef = useRef<Simulation<PositionedNode, Link> | null>(null);
  const [positions, setPositions] = useState<Positions>({});

  useEffect(() => {
    if (width === 0 || height === 0) return;
    const reduced = prefersReducedMotion();
    const cx = width / 2;
    const cy = height / 2;

    // Réutiliser les objets-nœuds existants pour conserver les positions.
    const nodes: PositionedNode[] = graph.nodes.map((n) => {
      const prev = nodesRef.current.get(n.id);
      return (
        prev ?? {
          id: n.id,
          x: cx + (Math.random() - 0.5) * 120,
          y: cy + (Math.random() - 0.5) * 120,
        }
      );
    });
    const map = new Map(nodes.map((n) => [n.id, n]));
    nodesRef.current = map;

    const links: Link[] = graph.edges
      .filter((e) => map.has(e.source) && map.has(e.target))
      .map((e) => ({ source: e.source, target: e.target }));

    const sim = forceSimulation<PositionedNode>(nodes)
      .force(
        'link',
        forceLink<PositionedNode, Link>(links)
          .id((d) => d.id)
          .distance(96)
          .strength(0.22),
      )
      .force('charge', forceManyBody().strength(-190))
      .force('center', forceCenter(cx, cy))
      .force('collide', forceCollide(36))
      .alpha(0.9)
      .alphaDecay(reduced ? 0.3 : 0.026)
      .velocityDecay(0.42);

    sim.on('tick', () => {
      const next: Positions = {};
      for (const n of nodes) next[n.id] = { x: n.x ?? cx, y: n.y ?? cy };
      setPositions(next);
    });

    simRef.current = sim;
    return () => {
      sim.stop();
    };
  }, [graph, width, height]);

  const dragStart = useCallback((id: string) => {
    const n = nodesRef.current.get(id);
    const sim = simRef.current;
    if (!n || !sim) return;
    n.fx = n.x ?? null;
    n.fy = n.y ?? null;
    sim.alphaTarget(0.3).restart();
  }, []);

  const dragMove = useCallback((id: string, x: number, y: number) => {
    const n = nodesRef.current.get(id);
    if (n) {
      n.fx = x;
      n.fy = y;
    }
  }, []);

  const dragEnd = useCallback(() => {
    simRef.current?.alphaTarget(0); // le nœud reste épinglé (fx/fy conservés)
  }, []);

  const unpin = useCallback((id: string) => {
    const n = nodesRef.current.get(id);
    if (n) {
      n.fx = null;
      n.fy = null;
    }
    simRef.current?.alphaTarget(0.1).restart();
  }, []);

  return { positions, dragStart, dragMove, dragEnd, unpin };
}
