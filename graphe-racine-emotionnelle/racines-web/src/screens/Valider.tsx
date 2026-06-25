/**
 * Valider (SPEC §3, écran 3 · PRD US3, principe §3.4 consentement). Texte libre
 * → propositions du LLM (POST /extract, NON persistées) → l'utilisateur accepte,
 * corrige ou refuse, sans pression → fusion dans la carte (POST /confirm).
 * On ne met jamais de mots dans sa bouche.
 */
import { useState } from 'react';
import s from './screens.module.css';
import { useNavigate } from 'react-router-dom';
import { Field, Invitation, SoftButton } from '../ui/primitives';
import { useGraphStore } from '../store/useGraphStore';
import { api, ApiError } from '../api/client';
import { NODE_LABELS } from '../api/types';
import type { GraphDto } from '../api/types';

interface NodeState {
  label: string;
  refused: boolean;
}

export function Valider() {
  const setGraph = useGraphStore((st) => st.setGraph);
  const addFil = useGraphStore((st) => st.addFil);
  const navigate = useNavigate();

  const [text, setText] = useState('');
  const [proposal, setProposal] = useState<GraphDto | null>(null);
  const [nodeStates, setNodeStates] = useState<Record<string, NodeState>>({});
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const propose = async () => {
    if (!text.trim()) return;
    setBusy(true);
    setErr(null);
    try {
      const p = await api.extract(text.trim());
      setProposal(p);
      setNodeStates(
        Object.fromEntries(p.nodes.map((n) => [n.id, { label: n.label, refused: false }])),
      );
      if (p.nodes.length === 0) {
        setErr('aucune racine n’a été repérée dans ces mots. essaie d’en dire un peu plus.');
      }
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'l’extraction n’a pas pu se faire');
    } finally {
      setBusy(false);
    }
  };

  const confirm = async () => {
    if (!proposal) return;
    setBusy(true);
    setErr(null);
    try {
      const keptNodes = proposal.nodes
        .filter((n) => !nodeStates[n.id]?.refused)
        .map((n) => ({ ...n, label: nodeStates[n.id]?.label ?? n.label }));
      const keptIds = new Set(keptNodes.map((n) => n.id));
      const keptEdges = proposal.edges.filter(
        (e) => keptIds.has(e.source) && keptIds.has(e.target),
      );
      const validated: GraphDto = { nodes: keptNodes, edges: keptEdges };
      const graph = await api.confirm(validated);
      setGraph(graph);
      addFil({ kind: 'depot', text: `${keptNodes.length} racines validées` });
      navigate('/jardin');
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'la fusion n’a pas pu se faire');
    } finally {
      setBusy(false);
    }
  };

  const toggleRefuse = (id: string) =>
    setNodeStates((st) => ({ ...st, [id]: { ...st[id], refused: !st[id].refused } }));

  const editLabel = (id: string, label: string) =>
    setNodeStates((st) => ({ ...st, [id]: { ...st[id], label } }));

  return (
    <div>
      <h2 className={s.title}>valider</h2>
      <Invitation>écris librement. je te proposerai des racines — tu gardes le dernier mot.</Invitation>

      <div style={{ marginTop: '1.2rem' }}>
        <Field
          label="ton texte"
          textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="raconte un moment, à ta façon…"
        />
        <SoftButton variant="accent" onClick={propose} disabled={busy}>
          {busy && !proposal ? 'je relis tes mots…' : 'proposer des racines'}
        </SoftButton>
      </div>

      {err && <p style={{ color: 'var(--corail)', marginTop: '0.8rem' }}>{err}</p>}

      {proposal && proposal.nodes.length > 0 && (
        <div style={{ marginTop: '1.4rem' }}>
          <p className={s.muted}>accepte, corrige ou refuse — une par une :</p>
          {proposal.nodes.map((n) => {
            const st = nodeStates[n.id];
            if (!st) return null;
            return (
              <div key={n.id} className={`${s.proposal} ${st.refused ? s.refused : ''}`}>
                <span className={s.propType}>{NODE_LABELS[n.type]}</span>
                <input
                  className={s.propLabel}
                  value={st.label}
                  disabled={st.refused}
                  onChange={(e) => editLabel(n.id, e.target.value)}
                  aria-label={`corriger ${NODE_LABELS[n.type]}`}
                />
                <div className={s.propActions}>
                  <button
                    className={s.iconBtn}
                    onClick={() => toggleRefuse(n.id)}
                    aria-label={st.refused ? 'reprendre' : 'refuser'}
                    title={st.refused ? 'reprendre' : 'refuser'}
                  >
                    {st.refused ? '↩' : '✕'}
                  </button>
                </div>
              </div>
            );
          })}

          <div className={s.row}>
            <SoftButton variant="accent" onClick={confirm} disabled={busy}>
              {busy ? 'la carte se tisse…' : 'tisser ces racines'}
            </SoftButton>
          </div>
        </div>
      )}
    </div>
  );
}
