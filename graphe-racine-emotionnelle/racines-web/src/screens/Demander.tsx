/**
 * Demander (SPEC §3, écran 4 · PRD US6/US7). Hero centré + champ unique + cartes
 * de questions inspirantes (cf. maquette). La réponse arrive douce ; le
 * sous-graphe est mémorisé et s'illumine au retour sur le Jardin. « Racine
 * commune » → la séquence signature (Revelation), jouée sur la carte.
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import s from './screens.module.css';
import { SoftButton } from '../ui/primitives';
import { useGraphStore } from '../store/useGraphStore';
import { api, ApiError } from '../api/client';
import { revelationPhrase } from '../api/types';

const EXEMPLES = [
  "qu'est-ce qui m'a déjà apaisé quand la tristesse est montée ?",
  'quelle croyance limitante revient le plus souvent sous mes tensions ?',
  'mes tensions au bureau ont-elles un lien avec ma famille ?',
  'quels sont mes besoins insatisfaits les plus fréquents ?',
];

export function Demander() {
  const highlightSubgraph = useGraphStore((st) => st.highlightSubgraph);
  const addFil = useGraphStore((st) => st.addFil);
  const setRevelation = useGraphStore((st) => st.setRevelation);
  const navigate = useNavigate();

  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<string | null>(null);
  const [hasSubgraph, setHasSubgraph] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const ask = async (q: string) => {
    const text = q.trim();
    if (!text || busy) return;
    setQuestion(text);
    setBusy(true);
    setErr(null);
    try {
      const res = await api.ask(text);
      setAnswer(res.answer);
      highlightSubgraph(res.subgraph);
      setHasSubgraph(res.subgraph.nodes.length > 0);
      addFil({ kind: 'question', text });
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'la réponse n’a pas pu venir');
    } finally {
      setBusy(false);
    }
  };

  const reveal = async () => {
    setBusy(true);
    setErr(null);
    try {
      const roots = await api.commonRoots();
      if (roots.length === 0) {
        setAnswer('aucune racine commune ne se dessine encore. laisse la carte grandir.');
        return;
      }
      const root = roots[0];
      setRevelation(root);
      addFil({ kind: 'revelation', text: revelationPhrase(root) });
      navigate('/jardin'); // la révélation se joue sur la carte
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'la révélation n’a pas pu venir');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={s.ask}>
      <h2 className={s.askTitle}>Interroger ses Racines</h2>
      <p className={s.askLede}>
        pose une question en langage naturel. le GraphRAG traversera tes écrits passés
        pour révéler les fils invisibles et ce qui t'apaise.
      </p>

      <div className={s.search}>
        <input
          className={s.searchInput}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && ask(question)}
          placeholder="ex : qu'est-ce qui nourrit ma colère en ce moment ?"
          aria-label="ta question"
          disabled={busy}
        />
        <button
          className={s.sendBtn}
          onClick={() => ask(question)}
          disabled={busy || !question.trim()}
          aria-label="demander"
          title="demander"
        >
          ➤
        </button>
      </div>

      {err && <p style={{ color: 'var(--corail)', textAlign: 'center' }}>{err}</p>}

      {answer ? (
        <>
          <div className={s.answer}>{answer}</div>
          <div className={s.row} style={{ justifyContent: 'center' }}>
            {hasSubgraph && (
              <SoftButton variant="ghost" onClick={() => navigate('/jardin')}>
                voir le sous-graphe dans le jardin
              </SoftButton>
            )}
            <SoftButton
              variant="ghost"
              onClick={() => {
                setAnswer(null);
                setQuestion('');
              }}
            >
              poser une autre question
            </SoftButton>
          </div>
        </>
      ) : (
        <>
          <p className="eyebrow" style={{ textAlign: 'center', marginTop: '2rem' }}>
            ✧ exemples de questions inspirantes
          </p>
          <div className={s.examples}>
            {EXEMPLES.map((q) => (
              <button key={q} className={s.exampleCard} onClick={() => ask(q)} disabled={busy}>
                {q}
              </button>
            ))}
          </div>
          <div className={s.row} style={{ justifyContent: 'center', marginTop: '1.4rem' }}>
            <SoftButton variant="ghost" onClick={reveal} disabled={busy}>
              ou révèle une racine commune
            </SoftButton>
          </div>
        </>
      )}
    </div>
  );
}
