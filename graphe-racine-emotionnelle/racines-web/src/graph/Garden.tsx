/**
 * La carte vivante (SPEC §3, écran 1). SVG plein écran, pan/zoom (souris,
 * molette, tactile + pinch), nœuds qui respirent, clic = l'histoire du nœud.
 * Quand un sous-graphe est surligné, le reste se désature. La Révélation
 * s'ouvre par-dessus quand une racine commune est découverte.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import styles from './Garden.module.css';
import { useGraphStore } from '../store/useGraphStore';
import { useForceLayout } from './ForceLayout';
import { RootEdge } from './RootEdge';
import { RootNode } from './RootNode';
import { Revelation } from './Revelation';
import { Legend } from './Legend';
import { useReducedMotion } from '../theme/motion';
import { NODE_LABELS } from '../api/types';
import type { NodeDto } from '../api/types';

interface Transform {
  x: number;
  y: number;
  k: number;
}

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));

export function Garden() {
  const graph = useGraphStore((s) => s.graph);
  const status = useGraphStore((s) => s.status);
  const error = useGraphStore((s) => s.error);
  const highlight = useGraphStore((s) => s.highlight);
  const freshNodeIds = useGraphStore((s) => s.freshNodeIds);
  const selectedNodeId = useGraphStore((s) => s.selectedNodeId);
  const selectNode = useGraphStore((s) => s.selectNode);
  const reduced = useReducedMotion();

  const containerRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ w: 0, h: 0 });
  const [t, setT] = useState<Transform>({ x: 0, y: 0, k: 1 });
  // transform courant lisible dans les handlers de drag (évite les closures périmées)
  const tRef = useRef(t);
  tRef.current = t;

  // gestes : on suit les pointeurs actifs
  const gesture = useRef<{
    pointers: Map<number, { x: number; y: number }>;
    lastDist: number | null;
  }>({ pointers: new Map(), lastDist: null });

  // drag d'un nœud (distinct du pan)
  const drag = useRef<{ id: string; moved: boolean; sx: number; sy: number } | null>(null);

  // mesure du conteneur (responsive)
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const ro = new ResizeObserver(() => {
      setSize({ w: el.clientWidth, h: el.clientHeight });
    });
    ro.observe(el);
    setSize({ w: el.clientWidth, h: el.clientHeight });
    return () => ro.disconnect();
  }, []);

  const layout = useForceLayout(graph, size.w, size.h);
  const positions = layout.positions;

  // pointeur écran → coordonnées monde (inverse du transform pan/zoom)
  const toWorld = useCallback((clientX: number, clientY: number) => {
    const rect = containerRef.current?.getBoundingClientRect();
    const cur = tRef.current;
    const px = clientX - (rect?.left ?? 0);
    const py = clientY - (rect?.top ?? 0);
    return { x: (px - cur.x) / cur.k, y: (py - cur.y) / cur.k };
  }, []);

  const onNodeDown = useCallback(
    (id: string, e: React.PointerEvent) => {
      e.stopPropagation(); // pas de pan
      (e.currentTarget as Element).setPointerCapture(e.pointerId);
      drag.current = { id, moved: false, sx: e.clientX, sy: e.clientY };
      layout.dragStart(id);
    },
    [layout],
  );

  const onNodeMove = useCallback(
    (e: React.PointerEvent) => {
      const d = drag.current;
      if (!d) return;
      if (!d.moved && Math.hypot(e.clientX - d.sx, e.clientY - d.sy) > 4) d.moved = true;
      const w = toWorld(e.clientX, e.clientY);
      layout.dragMove(d.id, w.x, w.y);
    },
    [layout, toWorld],
  );

  const onNodeUp = useCallback(
    (e: React.PointerEvent) => {
      const d = drag.current;
      if (!d) return;
      layout.dragEnd();
      if (!d.moved) selectNode(d.id); // simple clic = sélection
      drag.current = null;
      (e.currentTarget as Element).releasePointerCapture?.(e.pointerId);
    },
    [layout, selectNode],
  );

  const onPointerDown = useCallback((e: React.PointerEvent) => {
    // un clic sur un nœud (cercle) ne déclenche pas le pan
    if ((e.target as Element).tagName === 'circle') return;
    (e.currentTarget as Element).setPointerCapture(e.pointerId);
    gesture.current.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
  }, []);

  const onPointerMove = useCallback((e: React.PointerEvent) => {
    const g = gesture.current;
    if (!g.pointers.has(e.pointerId)) return;
    const prev = g.pointers.get(e.pointerId)!;
    g.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    const pts = [...g.pointers.values()];
    if (pts.length === 1) {
      const dx = e.clientX - prev.x;
      const dy = e.clientY - prev.y;
      setT((cur) => ({ ...cur, x: cur.x + dx, y: cur.y + dy }));
    } else if (pts.length === 2) {
      const [a, b] = pts;
      const dist = Math.hypot(a.x - b.x, a.y - b.y);
      if (g.lastDist != null) {
        const ratio = dist / g.lastDist;
        const mx = (a.x + b.x) / 2;
        const my = (a.y + b.y) / 2;
        setT((cur) => {
          const k = clamp(cur.k * ratio, 0.3, 3);
          const worldX = (mx - cur.x) / cur.k;
          const worldY = (my - cur.y) / cur.k;
          return { k, x: mx - worldX * k, y: my - worldY * k };
        });
      }
      g.lastDist = dist;
    }
  }, []);

  const onPointerUp = useCallback((e: React.PointerEvent) => {
    const g = gesture.current;
    g.pointers.delete(e.pointerId);
    if (g.pointers.size < 2) g.lastDist = null;
  }, []);

  const onWheel = useCallback((e: React.WheelEvent) => {
    const rect = (e.currentTarget as Element).getBoundingClientRect();
    const px = e.clientX - rect.left;
    const py = e.clientY - rect.top;
    const factor = e.deltaY < 0 ? 1.1 : 0.9;
    setT((cur) => {
      const k = clamp(cur.k * factor, 0.3, 3);
      const worldX = (px - cur.x) / cur.k;
      const worldY = (py - cur.y) / cur.k;
      return { k, x: px - worldX * k, y: py - worldY * k };
    });
  }, []);

  const selectedNode = graph.nodes.find((n) => n.id === selectedNodeId) ?? null;

  return (
    <div className={styles.garden} ref={containerRef}>
      <svg
        className={styles.svg}
        role="application"
        aria-label="carte des racines"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onWheel={onWheel}
      >
        <g transform={`translate(${t.x}, ${t.y}) scale(${t.k})`}>
          {graph.edges.map((e) => {
            const s = positions[e.source];
            const d = positions[e.target];
            if (!s || !d) return null;
            const fresh = freshNodeIds.has(e.source) || freshNodeIds.has(e.target);
            const dimmed = !!highlight && !highlight.edgeIds.has(e.id);
            return (
              <RootEdge
                key={e.id}
                x1={s.x}
                y1={s.y}
                x2={d.x}
                y2={d.y}
                fresh={fresh}
                dimmed={dimmed}
                reduced={reduced}
              />
            );
          })}

          {graph.nodes.map((n, i) => {
            const p = positions[n.id];
            if (!p) return null;
            const dimmed = !!highlight && !highlight.nodeIds.has(n.id);
            return (
              <RootNode
                key={n.id}
                node={n}
                x={p.x}
                y={p.y}
                index={i}
                fresh={freshNodeIds.has(n.id)}
                dimmed={dimmed}
                selected={n.id === selectedNodeId}
                reduced={reduced}
                onSelect={selectNode}
                onNodeDown={onNodeDown}
                onNodeMove={onNodeMove}
                onNodeUp={onNodeUp}
              />
            );
          })}
        </g>
      </svg>

      <Legend />

      {graph.nodes.length > 0 && (
        <span className={styles.zoomHint}>molette ou pince pour zoomer · glisser pour déplacer</span>
      )}

      {status === 'loading' && (
        <div className={styles.state}>
          <div className={styles.stateInner}>
            <p>la carte s'éveille…</p>
          </div>
        </div>
      )}
      {status === 'error' && (
        <div className={styles.state}>
          <div className={styles.stateInner}>
            <p>{error ?? "la carte n'a pas pu être jointe"}</p>
          </div>
        </div>
      )}
      {status === 'ready' && graph.nodes.length === 0 && (
        <div className={styles.state}>
          <div className={styles.stateInner}>
            <p style={{ fontFamily: 'var(--display)', fontStyle: 'italic', fontSize: '1.2rem' }}>
              ta première racine attend tes mots.
            </p>
          </div>
        </div>
      )}

      {selectedNode && <NodeStory node={selectedNode} onClose={() => selectNode(null)} />}

      <Revelation positions={positions} transform={t} />
    </div>
  );
}

function NodeStory({ node, onClose }: { node: NodeDto; onClose: () => void }) {
  const extraEntries = Object.entries(node.extra ?? {});
  return (
    <aside className={styles.story} aria-label="l'histoire du nœud">
      <button className={styles.storyClose} aria-label="fermer" onClick={onClose}>
        ✕
      </button>
      <p className={styles.storyType}>{NODE_LABELS[node.type]}</p>
      <p className={styles.storyLabel}>{node.label}</p>
      {extraEntries.length === 0 ? (
        <p className={styles.storyExtra}>un nœud simple, posé là.</p>
      ) : (
        extraEntries.map(([k, v]) => (
          <p className={styles.storyExtra} key={k}>
            {k} · {String(v)}
          </p>
        ))
      )}
    </aside>
  );
}
