/**
 * Contrat API consommé (SPEC §7). Aligné sur racines-api/.../dto/Dtos.java.
 * Le reste de l'app ne connaît QUE ces types : un seul endroit parle au backend.
 */

export type NodeType =
  | 'EMOTION'
  | 'SITUATION'
  | 'BELIEF'
  | 'SENSATION'
  | 'NEED'
  | 'RESOURCE'
  | 'PERSON'
  | 'ENTRY';

export type EdgeType =
  | 'TRIGGERS' // Situation → Emotion
  | 'FED_BY' // Emotion → Belief
  | 'EXPRESSED_AS' // Emotion → Sensation
  | 'TOUCHES' // Belief → Need
  | 'SOOTHES' // Resource → Emotion
  | 'MENTIONS'; // Entry → n'importe quel nœud

export interface NodeDto {
  id: string;
  type: NodeType;
  label: string;
  extra?: Record<string, unknown>;
}

export interface EdgeDto {
  id: string;
  type: EdgeType;
  source: string;
  target: string;
}

export interface GraphDto {
  nodes: NodeDto[];
  edges: EdgeDto[];
}

export interface AskResponse {
  answer: string;
  subgraph: GraphDto;
}

export interface CommonRootDto {
  belief: NodeDto;
  situations: NodeDto[];
  /** Phrase-invitation. Null tant que le LLM ne l'a pas formulée (hors-ligne). */
  revelation: string | null;
}

/**
 * Phrase-invitation d'une racine commune. Si le backend l'a formulée (LLM), on
 * la garde ; sinon on en compose une dans la voix de l'app (SPEC §8) : inviter,
 * jamais juger. « un fil semble relier ces deux moments — est-ce que ça te parle ? »
 */
export function revelationPhrase(root: CommonRootDto): string {
  if (root.revelation && root.revelation.trim()) return root.revelation;
  const [a, b] = root.situations;
  if (a && b) {
    return `un fil semble relier « ${a.label} » et « ${b.label} » — peut-être la même racine, « ${root.belief.label} ». est-ce que ça te parle ?`;
  }
  return `une même racine, « ${root.belief.label} », semble traverser plusieurs de tes moments. est-ce que ça te parle ?`;
}

/** Dépôt structuré (phase 1). Tous les champs sont optionnels côté backend. */
export interface DepositRequest {
  rawText?: string;
  sky?: string;
  emotion?: string;
  situation?: string;
  belief?: string;
  sensation?: string;
  need?: string;
  resource?: string;
  person?: string;
}

/** Libellés humains des types de nœuds (sentence case, voix douce). */
export const NODE_LABELS: Record<NodeType, string> = {
  EMOTION: 'émotion',
  SITUATION: 'situation',
  BELIEF: 'croyance',
  SENSATION: 'sensation',
  NEED: 'besoin',
  RESOURCE: 'ressource',
  PERSON: 'personne',
  ENTRY: 'entrée',
};

