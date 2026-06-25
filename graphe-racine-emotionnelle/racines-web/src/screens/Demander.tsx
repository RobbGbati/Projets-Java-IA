/**
 * Demander (SPEC §3, écran 4 · PRD US6/US7). Un champ unique. La réponse arrive
 * douce ; le sous-graphe concerné s'illumine dans le Jardin (le reste se désature).
 * Panneau latéral : la carte reste visible pendant la lecture.
 * Bouton « racine commune » → la séquence signature (Revelation).
 */
import { useState } from 'react';
import app from '../App.module.css';
import s from './screens.module.css';
import { Field, Invitation, SoftButton } from '../ui/primitives';
import { useGraphStore } from '../store/useGraphStore';
import { api, ApiError } from '../api/client';
import { revelationPhrase } from '../api/types';

export function Demander() {
  const highlightSubgraph = useGraphStore((st) => st.highlightSubgraph);
  const clearHighlight = useGraphStore((st) => st.clearHighlight);
  const addFil = useGraphStore((st) => st.addFil);
  const setRevelation = useGraphStore((st) => st.setRevelation);
  const goTo = useGraphStore((st) => st.goTo);

  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const ask = async () => {
    if (!question.trim()) return;
    setBusy(true);
    setErr(null);
    try {
      const res = await api.ask(question.trim());
      setAnswer(res.answer);
      highlightSubgraph(res.subgraph);
      addFil({ kind: 'question', text: question.trim() });
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
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'la révélation n’a pas pu venir');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={`${app.panel} ${app.side}`}>
      <div className={app.panelInner}>
        <button
          className={app.closeBtn}
          aria-label="fermer cette carte"
          title="fermer"
          onClick={() => {
            clearHighlight();
            goTo('jardin');
          }}
        >
          ✕
        </button>
        <h2 className={s.title}>demander</h2>
        <Invitation>pose ta question à tes racines — tu recevras une lumière, pas un verdict.</Invitation>

        <div style={{ marginTop: '1.2rem' }}>
          <Field
            label="ta question"
            textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="qu'est-ce qui m'a apaisé quand j'étais en colère ?"
          />
          <div className={s.row}>
            <SoftButton variant="accent" onClick={ask} disabled={busy}>
              {busy ? 'la carte écoute…' : 'demander'}
            </SoftButton>
            <SoftButton variant="ghost" onClick={reveal} disabled={busy}>
              une racine commune ?
            </SoftButton>
          </div>
        </div>

        {err && <p style={{ color: 'var(--corail)' }}>{err}</p>}

        {answer && (
          <>
            <div className={s.answer}>{answer}</div>
            <button
              className={s.iconBtn}
              style={{ marginTop: '0.6rem' }}
              onClick={() => {
                setAnswer(null);
                clearHighlight();
              }}
            >
              effacer le surlignage
            </button>
          </>
        )}
      </div>
    </div>
  );
}
