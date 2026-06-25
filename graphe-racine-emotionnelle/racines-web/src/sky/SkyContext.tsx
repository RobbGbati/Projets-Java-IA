/**
 * Le « ciel » : l'humeur du jour, qui colore le fond (PRD US2, SPEC §3).
 * Changement de ciel = cross-fade du fond (1.2 s), pas de coupure.
 * Honoré par reduced-motion (le fondu devient instantané via tokens.css).
 */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useGraphStore } from '../store/useGraphStore';

export interface Sky {
  id: string;
  label: string;
  /** Dégradé de fond (du haut, ciel, vers le bas, sol). */
  gradient: string;
}

export const SKIES: Sky[] = [
  { id: 'ciel-paix', label: 'ciel paisible', gradient: 'linear-gradient(180deg, #cfeaf1 0%, #e9e0cf 60%, #3a2c1e 100%)' },
  { id: 'ciel-gris', label: 'ciel gris', gradient: 'linear-gradient(180deg, #c3c9cc 0%, #ada392 60%, #3a2c1e 100%)' },
  { id: 'ciel-aube', label: "ciel d'aube", gradient: 'linear-gradient(180deg, #f4d7c2 0%, #e8c79f 60%, #3a2c1e 100%)' },
  { id: 'ciel-orage', label: "ciel d'orage", gradient: 'linear-gradient(180deg, #8f96a3 0%, #6a6256 60%, #2b211a 100%)' },
  { id: 'ciel-nuit', label: 'ciel de nuit', gradient: 'linear-gradient(180deg, #2c3a52 0%, #3a3328 60%, #1f1812 100%)' },
];

interface SkyCtx {
  sky: Sky;
  setSky: (id: string) => void;
  skies: Sky[];
}

const Ctx = createContext<SkyCtx | null>(null);

export function SkyProvider({ children }: { children: ReactNode }) {
  const skyId = useGraphStore((s) => s.sky);
  const setSkyId = useGraphStore((s) => s.setSky);
  const [, setTick] = useState(0);

  const sky = useMemo(() => SKIES.find((s) => s.id === skyId) ?? SKIES[0], [skyId]);

  // Applique le fond au document : deux couches superposées qui se relaient
  // donneraient un vrai cross-fade ; ici on profite de la transition CSS sur
  // background-image (tokens.css coupe la transition en reduced-motion).
  useEffect(() => {
    document.body.style.transition = 'background 1.2s var(--ease-organique)';
    document.body.style.background = sky.gradient;
    setTick((t) => t + 1);
  }, [sky]);

  const value = useMemo<SkyCtx>(
    () => ({ sky, setSky: setSkyId, skies: SKIES }),
    [sky, setSkyId],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSky(): SkyCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useSky doit être utilisé dans <SkyProvider>');
  return ctx;
}
