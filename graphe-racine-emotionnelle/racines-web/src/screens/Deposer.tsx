/**
 * Déposer (SPEC §3, écran 2 · PRD US1/US2). Saisie structurée (phase 1) +
 * écriture libre + sélecteur de ciel. Voix d'invitation : « déposer », jamais
 * « créer une entrée ». Au dépôt, la carte grandit (POST /api/entries).
 */
import { useState } from 'react';
import s from './screens.module.css';
import { Field, Invitation, SoftButton } from '../ui/primitives';
import { useSky } from '../sky/SkyContext';
import { useGraphStore } from '../store/useGraphStore';
import { api, ApiError } from '../api/client';
import type { DepositRequest } from '../api/types';

const FIELDS: { key: keyof DepositRequest; label: string }[] = [
  { key: 'emotion', label: "l'émotion" },
  { key: 'situation', label: 'la situation' },
  { key: 'belief', label: 'la croyance / pensée' },
  { key: 'sensation', label: 'la sensation' },
  { key: 'need', label: 'le besoin' },
  { key: 'resource', label: 'ce qui apaise' },
  { key: 'person', label: 'une personne (anonyme)' },
];

export function Deposer() {
  const { sky, setSky, skies } = useSky();
  const setGraph = useGraphStore((st) => st.setGraph);
  const addFil = useGraphStore((st) => st.addFil);
  const goTo = useGraphStore((st) => st.goTo);

  const [form, setForm] = useState<DepositRequest>({});
  const [rawText, setRawText] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const update = (key: keyof DepositRequest, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));

  const submit = async () => {
    setBusy(true);
    setErr(null);
    try {
      const graph = await api.deposit({ ...form, rawText, sky: sky.label });
      setGraph(graph);
      addFil({ kind: 'depot', text: rawText.trim() || 'un dépôt structuré' });
      goTo('jardin');
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'le dépôt n’a pas pu être enregistré');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h2 className={s.title}>déposer</h2>
      <Invitation>confie ce que tu traverses — quelques mots suffisent.</Invitation>

      <div style={{ marginTop: '1.4rem' }}>
        <p className={s.muted}>ton ciel aujourd'hui</p>
        <div className={s.skies}>
          {skies.map((k) => (
            <button
              key={k.id}
              className={`${s.skyChip} ${k.id === sky.id ? s.on : ''}`}
              aria-pressed={k.id === sky.id}
              onClick={() => setSky(k.id)}
            >
              {k.label}
            </button>
          ))}
        </div>
      </div>

      <Field
        label="quelques mots libres"
        textarea
        value={rawText}
        onChange={(e) => setRawText(e.target.value)}
        placeholder="ce qui pèse, ce qui passe…"
      />

      <p className={s.muted} style={{ marginBottom: '0.6rem' }}>
        et si tu veux, nomme les racines toi-même :
      </p>
      <div className={s.grid}>
        {FIELDS.map((f) => (
          <Field
            key={f.key}
            label={f.label}
            value={(form[f.key] as string) ?? ''}
            onChange={(e) => update(f.key, e.target.value)}
          />
        ))}
      </div>

      {err && <p style={{ color: 'var(--corail)' }}>{err}</p>}

      <div className={s.row}>
        <SoftButton variant="accent" onClick={submit} disabled={busy}>
          {busy ? 'la racine pousse…' : 'déposer'}
        </SoftButton>
        <a href={api.exportUrl()} download>
          <SoftButton variant="ghost" type="button">
            exporter mon cheminement
          </SoftButton>
        </a>
      </div>
    </div>
  );
}
