/**
 * Helpers de mouvement partagés (SPEC §4).
 * Le respect de `prefers-reduced-motion` est centralisé ici : un seul endroit
 * décide si l'on anime ou si l'on réduit à un fondu. Non négociable (PRD §3.8).
 */
import { useEffect, useState } from 'react';

export const EASE_ORGANIQUE = [0.2, 0.8, 0.2, 1] as const;

export const DUREE = {
  courte: 0.5,
  moyenne: 0.8,
  longue: 1.2,
} as const;

/** Vrai si l'utilisateur a demandé moins de mouvement. Lu une fois (au chargement). */
export function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** Hook réactif : suit le changement de préférence sans rechargement. */
export function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(prefersReducedMotion);

  useEffect(() => {
    if (!window.matchMedia) return;
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    const onChange = () => setReduced(mq.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  return reduced;
}

/** Spring doux pour Motion (naissance d'un nœud : overshoot léger). */
export const SPRING_DOUX = {
  type: 'spring' as const,
  stiffness: 120,
  damping: 16,
  mass: 0.9,
};
