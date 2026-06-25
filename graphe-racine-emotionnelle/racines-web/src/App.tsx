/**
 * Shell + routing. Chaque menu a son URL dédiée (/jardin, /deposer, …). La carte
 * vivante (Jardin) reste le fond persistant — ce qui préserve le surlignage du
 * sous-graphe et la révélation. Les autres menus s'ouvrent en panneau routé
 * par-dessus. Nav en bas (NavLink).
 */
import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import styles from './App.module.css';
import { SkyProvider } from './sky/SkyContext';
import { useGraphStore } from './store/useGraphStore';
import { Garden } from './graph/Garden';
import { Deposer } from './screens/Deposer';
import { Demander } from './screens/Demander';
import { Valider } from './screens/Valider';
import { Fil } from './screens/Fil';

const NAV: { to: string; label: string }[] = [
  { to: '/jardin', label: 'le jardin' },
  { to: '/deposer', label: 'déposer' },
  { to: '/demander', label: 'demander' },
  { to: '/valider', label: 'valider' },
  { to: '/fil', label: 'le fil' },
];

/** Panneau crème centré (Déposer/Valider/Fil), avec fermeture → Jardin. */
function Panel({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  return (
    <div className={styles.panel}>
      <div className={styles.panelInner}>
        <button
          className={styles.closeBtn}
          aria-label="fermer cette carte"
          title="fermer"
          onClick={() => navigate('/jardin')}
        >
          ✕
        </button>
        {children}
      </div>
    </div>
  );
}

function AppInner() {
  const loadGraph = useGraphStore((s) => s.loadGraph);
  const status = useGraphStore((s) => s.status);
  const location = useLocation();

  useEffect(() => {
    void loadGraph();
  }, [loadGraph]);

  const onJardin = location.pathname === '/jardin' || location.pathname === '/';
  const refreshing = status === 'loading';

  return (
    <div className={styles.app}>
      <div className={styles.backdrop}>
        <Garden />
      </div>

      <Routes>
        <Route path="/" element={<Navigate to="/jardin" replace />} />
        <Route path="/jardin" element={null} />
        <Route path="/deposer" element={<Panel><Deposer /></Panel>} />
        <Route path="/valider" element={<Panel><Valider /></Panel>} />
        <Route path="/fil" element={<Panel><Fil /></Panel>} />
        <Route path="/demander" element={<Demander />} />
        <Route path="*" element={<Navigate to="/jardin" replace />} />
      </Routes>

      {onJardin && (
        <button
          className={styles.refresh}
          onClick={() => void loadGraph()}
          disabled={refreshing}
          aria-label="réactualiser la carte"
        >
          <span className={refreshing ? styles.refreshSpin : undefined} aria-hidden="true">
            ↻
          </span>
          {refreshing ? 'la carte se rafraîchit…' : 'réactualiser'}
        </button>
      )}

      <nav className={styles.nav} aria-label="navigation principale">
        {NAV.map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            className={({ isActive }) => `${styles.navBtn} ${isActive ? styles.active : ''}`}
          >
            {n.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export default function App() {
  return (
    <SkyProvider>
      <AppInner />
    </SkyProvider>
  );
}
