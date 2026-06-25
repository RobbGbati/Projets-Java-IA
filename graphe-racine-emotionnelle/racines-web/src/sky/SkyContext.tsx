/**
 * Le « ciel » : l'humeur du jour (PRD US2, SPEC §3). Chaque ciel a un nom
 * poétique, une description, une pastille, et un dégradé qui colore le fond du
 * Jardin. Changement de ciel = cross-fade du fond (1.2 s). Honoré par
 * reduced-motion (le fondu devient instantané via tokens.css).
 */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useGraphStore } from '../store/useGraphStore';

export interface Sky {
  id: string;
  name: string;
  description: string;
  swatch: string;
  /** Dégradé de fond (du ciel, en haut, vers le sol, en bas). */
  gradient: string;
}

export const SKIES: Sky[] = [
  {
    id: 'aurore',
    name: 'Aurore',
    description: 'Douceur & Espoir',
    swatch: '#f0d9d0',
    gradient: 'linear-gradient(180deg, #fbe7df 0%, #e9d6c2 58%, #3a2c1e 100%)',
  },
  {
    id: 'zenith',
    name: 'Zénith',
    description: 'Clarté & Présence',
    swatch: '#f5ecc9',
    gradient: 'linear-gradient(180deg, #fbf6dd 0%, #e9e0cf 58%, #3a2c1e 100%)',
  },
  {
    id: 'crepuscule',
    name: 'Crépuscule',
    description: 'Mélancolie douce',
    swatch: '#f1cba6',
    gradient: 'linear-gradient(180deg, #f7dcc0 0%, #e0b48f 58%, #3a2c1e 100%)',
  },
  {
    id: 'nuit',
    name: 'Nuit Étoilée',
    description: 'Introspection profonde',
    swatch: '#1f2d44',
    gradient: 'linear-gradient(180deg, #2c3a52 0%, #2a2e3a 58%, #161210 100%)',
  },
  {
    id: 'orage',
    name: 'Orage Sauge',
    description: 'Tension & Transformation',
    swatch: '#3a4a36',
    gradient: 'linear-gradient(180deg, #4a5a44 0%, #3a3d30 58%, #1f1812 100%)',
  },
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

  const sky = useMemo(() => SKIES.find((s) => s.id === skyId) ?? SKIES[1], [skyId]);

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
