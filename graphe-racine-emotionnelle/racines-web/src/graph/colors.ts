/**
 * Couleur d'un nœud selon son type (et sa valence pour les émotions).
 * Le corail n'apparaît JAMAIS ici : il est réservé à la racine commune
 * (Revelation). C'est ce qui lui donne son poids (SPEC §2).
 */
import type { NodeDto } from '../api/types';

// Valeurs en dur (équivalent tokens.css) — évite de lire le CSSOM par nœud.
const C = {
  terreDouce: '#5c4630',
  racineClaire: '#8a6a44',
  sauge: '#5a8f4e',
  ocre: '#b98a2e',
  cielPaix: '#cfeaf1',
  creme: '#faf4e8',
};

export function nodeColor(node: NodeDto): string {
  switch (node.type) {
    case 'EMOTION': {
      const valence = String(node.extra?.['valence'] ?? '');
      return valence.startsWith('pos') ? C.sauge : C.ocre;
    }
    case 'SITUATION':
      return C.terreDouce;
    case 'BELIEF':
      return C.racineClaire;
    case 'SENSATION':
      return C.ocre;
    case 'NEED':
      return C.sauge;
    case 'RESOURCE':
      return C.cielPaix;
    case 'PERSON':
      return C.racineClaire;
    case 'ENTRY':
      return C.creme;
    default:
      return C.racineClaire;
  }
}

export function nodeRadius(node: NodeDto): number {
  if (node.type === 'BELIEF') return 26; // les croyances sont des racines profondes
  if (node.type === 'ENTRY') return 14;
  return 22;
}
