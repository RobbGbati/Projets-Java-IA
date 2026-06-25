/**
 * Client REST typé — le SEUL module qui parle au backend (SPEC §6, §7).
 * Aucune logique métier ici : on envoie, on reçoit, on type.
 */
import type { AskResponse, CommonRootDto, DepositRequest, GraphDto } from './types';

const BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    });
  } catch (cause) {
    // Réseau coupé / backend éteint : message doux, jamais d'excuse clinique.
    throw new ApiError(0, "la carte n'a pas pu être jointe pour le moment");
  }
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new ApiError(res.status, body || `erreur ${res.status}`);
  }
  // Certains endpoints peuvent renvoyer un corps vide ; on reste tolérant.
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

export const api = {
  // ---- lecture de la carte (phase 0) ----
  getGraph: () => request<GraphDto>('/api/graph'),

  // ---- dépôt structuré (phase 1) ----
  deposit: (req: DepositRequest) =>
    request<GraphDto>('/api/entries', {
      method: 'POST',
      body: JSON.stringify(req),
    }),

  // ---- interrogation GraphRAG (phase 2) ----
  ask: (question: string) =>
    request<AskResponse>('/api/ask', {
      method: 'POST',
      body: JSON.stringify({ question }),
    }),

  commonRoots: () => request<CommonRootDto[]>('/api/insights/common-roots'),

  // ---- extraction LLM + validation (phase 3) ----
  extract: (text: string) =>
    request<GraphDto>('/api/entries/extract', {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),

  confirm: (validated: GraphDto) =>
    request<GraphDto>('/api/entries/confirm', {
      method: 'POST',
      body: JSON.stringify(validated),
    }),

  // ---- export (US9) ----
  exportUrl: () => `${BASE}/api/export`,
};

export { ApiError };
