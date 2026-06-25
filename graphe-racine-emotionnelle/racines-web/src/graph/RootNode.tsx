/**
 * Un nœud = une racine. Au repos il « respire » (scale en boucle lente, déphasé
 * par index) — la carte n'est jamais figée. À la naissance : scale 0→1 avec un
 * léger overshoot (spring). Atteignable au clavier, focusable, clic = son histoire.
 * Tout mouvement est coupé en reduced-motion.
 */
import type { PointerEvent } from 'react';
import { motion } from 'motion/react';
import type { NodeDto } from '../api/types';
import { NODE_LABELS } from '../api/types';
import { nodeColor, nodeRadius } from './colors';
import { SPRING_DOUX } from '../theme/motion';

interface Props {
  node: NodeDto;
  x: number;
  y: number;
  index: number;
  fresh: boolean;
  dimmed: boolean;
  selected: boolean;
  reduced: boolean;
  onSelect: (id: string) => void;
  onNodeDown: (id: string, e: PointerEvent) => void;
  onNodeMove: (e: PointerEvent) => void;
  onNodeUp: (e: PointerEvent) => void;
}

export function RootNode({
  node,
  x,
  y,
  index,
  fresh,
  dimmed,
  selected,
  reduced,
  onSelect,
  onNodeDown,
  onNodeMove,
  onNodeUp,
}: Props) {
  const color = nodeColor(node);
  const r = nodeRadius(node);

  const breathe =
    reduced
      ? {}
      : {
          scale: [1, 1.03, 1],
          transition: {
            duration: 5.5,
            repeat: Infinity,
            ease: 'easeInOut' as const,
            delay: (index % 7) * 0.4, // déphasage
          },
        };

  return (
    <g transform={`translate(${x}, ${y})`} style={{ opacity: dimmed ? 0.28 : 1 }}>
      {/* naissance (overshoot) sur la couche externe */}
      <motion.g
        initial={fresh && !reduced ? { scale: 0 } : false}
        animate={{ scale: 1 }}
        transition={SPRING_DOUX}
      >
        {/* respiration sur la couche interne */}
        <motion.g animate={breathe}>
          <circle
            r={r}
            fill={color}
            stroke={selected ? 'var(--encre)' : 'rgba(43,51,38,0.18)'}
            strokeWidth={selected ? 2.5 : 1}
            tabIndex={0}
            role="button"
            aria-label={`${NODE_LABELS[node.type]} : ${node.label}`}
            style={{ cursor: 'grab', outline: 'none', touchAction: 'none' }}
            onPointerDown={(e) => onNodeDown(node.id, e)}
            onPointerMove={onNodeMove}
            onPointerUp={onNodeUp}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(node.id);
              }
            }}
          />
        </motion.g>
      </motion.g>

      <text
        y={r + 16}
        textAnchor="middle"
        fontFamily="var(--corps)"
        fontSize={12}
        fill="var(--encre)"
        style={{ pointerEvents: 'none', userSelect: 'none' }}
      >
        {node.label.length > 26 ? `${node.label.slice(0, 25)}…` : node.label}
      </text>
    </g>
  );
}
