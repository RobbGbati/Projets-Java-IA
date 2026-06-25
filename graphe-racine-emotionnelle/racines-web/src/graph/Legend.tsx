/**
 * Légende du code couleur : on relie chaque teinte à un type de nœud.
 * N'affiche que les types présents dans la carte (pas de clutter), dans un
 * ordre stable qui met les 4 types-clés en tête.
 */
import styles from './Legend.module.css';
import { TYPE_COLOR } from './colors';
import { NODE_LABELS } from '../api/types';
import type { NodeType } from '../api/types';
import { useGraphStore } from '../store/useGraphStore';

const ORDER: NodeType[] = [
  'EMOTION',
  'SITUATION',
  'BELIEF',
  'NEED',
  'SENSATION',
  'RESOURCE',
  'PERSON',
  'ENTRY',
];

export function Legend() {
  const nodes = useGraphStore((s) => s.graph.nodes);
  if (nodes.length === 0) return null;

  const present = new Set(nodes.map((n) => n.type));
  const types = ORDER.filter((t) => present.has(t));

  return (
    <ul className={styles.legend} aria-label="code couleur des racines">
      {types.map((t) => (
        <li key={t} className={styles.item}>
          <span className={styles.swatch} style={{ background: TYPE_COLOR[t] }} aria-hidden="true" />
          {NODE_LABELS[t]}
        </li>
      ))}
    </ul>
  );
}
