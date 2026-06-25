/**
 * Code couleur par type de nœud — on doit repérer un type d'un coup d'œil.
 * Les 4 types-clés (émotion, situation, croyance, besoin) ont des teintes bien
 * séparées (vert / ambre / bleu ardoise / mauve). Les types secondaires
 * (sensation, ressource, personne, entrée) restent en tons sourds pour ne pas
 * concurrencer les 4. Le corail n'est JAMAIS ici : réservé à la racine commune
 * (Revelation) — c'est ce qui lui donne son poids (SPEC §2).
 */
import type { NodeDto, NodeType } from '../api/types';

export const TYPE_COLOR: Record<NodeType, string> = {
  EMOTION: '#5a8f4e', // sauge — vert
  SITUATION: '#c98a2e', // ambre
  BELIEF: '#5b6fa0', // bleu ardoise (croyance)
  NEED: '#9a6aa6', // mauve (besoin)
  SENSATION: '#6f5638', // brun terre (sourd)
  RESOURCE: '#7fb6c4', // bleu-vert apaisant (ce qui apaise)
  PERSON: '#8a6a44', // racine claire (sourd)
  ENTRY: '#cdbfa6', // crème grisé (discret)
};

export function nodeColor(node: NodeDto): string {
  return TYPE_COLOR[node.type] ?? '#8a6a44';
}

export function nodeRadius(node: NodeDto): number {
  if (node.type === 'BELIEF') return 26; // les croyances sont des racines profondes
  if (node.type === 'ENTRY') return 14;
  return 22;
}
