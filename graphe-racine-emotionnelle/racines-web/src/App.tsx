/**
 * Routeur d'écrans. La carte vivante (Jardin) est toujours le fond ; les autres
 * écrans s'ouvrent en panneau doux par-dessus. « Demander » s'ouvre en panneau
 * latéral pour laisser voir le surlignage du sous-graphe dans la carte.
 */
import { useEffect } from 'react';
import styles from './App.module.css';
import { SkyProvider } from './sky/SkyContext';
import { useGraphStore } from './store/useGraphStore';
import type { Screen } from './store/useGraphStore';
import { Garden } from './graph/Garden';
import { Deposer } from './screens/Deposer';
import { Demander } from './screens/Demander';
import { Valider } from './screens/Valider';
import { Fil } from './screens/Fil';

const NAV: { id: Screen; label: string }[] = [
  { id: 'jardin', label: 'le jardin' },
  { id: 'deposer', label: 'déposer' },
  { id: 'demander', label: 'demander' },
  { id: 'valider', label: 'valider' },
  { id: 'fil', label: 'le fil' },
];

function ScreenPanel() {
  const screen = useGraphStore((s) => s.screen);
  const goTo = useGraphStore((s) => s.goTo);
  if (screen === 'jardin') return null;
  if (screen === 'demander') return <Demander />;

  const panel =
    screen === 'deposer' ? <Deposer /> : screen === 'valider' ? <Valider /> : <Fil />;
  return (
    <div className={styles.panel}>
      <div className={styles.panelInner}>
        <button
          className={styles.closeBtn}
          aria-label="fermer cette carte"
          title="fermer"
          onClick={() => goTo('jardin')}
        >
          ✕
        </button>
        {panel}
      </div>
    </div>
  );
}

function AppInner() {
  const screen = useGraphStore((s) => s.screen);
  const goTo = useGraphStore((s) => s.goTo);
  const loadGraph = useGraphStore((s) => s.loadGraph);
  const status = useGraphStore((s) => s.status);

  useEffect(() => {
    void loadGraph();
  }, [loadGraph]);

  const refreshing = status === 'loading';

  return (
    <div className={styles.app}>
      <div className={styles.backdrop}>
        <Garden />
      </div>

      {screen === 'jardin' && (
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

      <ScreenPanel />

      <nav className={styles.nav} aria-label="navigation principale">
        {NAV.map((n) => (
          <button
            key={n.id}
            className={`${styles.navBtn} ${screen === n.id ? styles.active : ''}`}
            aria-current={screen === n.id ? 'page' : undefined}
            onClick={() => goTo(n.id)}
          >
            {n.label}
          </button>
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
