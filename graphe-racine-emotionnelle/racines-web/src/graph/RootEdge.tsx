/**
 * Une arête = une racine qui relie. À la naissance, elle se *dessine*
 * (pathLength 0→1 via Motion). Quand un sous-graphe est surligné, les arêtes
 * hors-contexte se désaturent (le sens reste lisible).
 */
import { motion } from 'motion/react';
import { DUREE, EASE_ORGANIQUE } from '../theme/motion';

interface Props {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  fresh: boolean;
  dimmed: boolean;
  reduced: boolean;
}

export function RootEdge({ x1, y1, x2, y2, fresh, dimmed, reduced }: Props) {
  // Courbe douce : un léger ventre, comme une racine, pas une ligne droite.
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2;
  const dx = x2 - x1;
  const dy = y2 - y1;
  const norm = Math.hypot(dx, dy) || 1;
  const bend = 14;
  const cx = mx + (-dy / norm) * bend;
  const cy = my + (dx / norm) * bend;
  const d = `M ${x1} ${y1} Q ${cx} ${cy} ${x2} ${y2}`;

  return (
    <motion.path
      d={d}
      fill="none"
      stroke="var(--racine-claire)"
      strokeWidth={2}
      strokeLinecap="round"
      initial={fresh && !reduced ? { pathLength: 0, opacity: 0 } : false}
      animate={{ pathLength: 1, opacity: dimmed ? 0.12 : 0.6 }}
      transition={{
        pathLength: { duration: reduced ? 0 : DUREE.longue, ease: EASE_ORGANIQUE },
        opacity: { duration: reduced ? 0 : DUREE.moyenne, ease: EASE_ORGANIQUE },
      }}
    />
  );
}
