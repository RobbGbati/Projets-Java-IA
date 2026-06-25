/**
 * Déposer (SPEC §3, écran 2 · PRD US1/US2). Choix du ciel (cartes), écriture
 * libre, et saisie guidée (structurée) en option. Voix d'invitation : « déposer »,
 * jamais « créer une entrée ». Au dépôt, la carte grandit (POST /api/entries).
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const navigate = useNavigate();

  const [form, setForm] = useState<DepositRequest>({});
  const [rawText, setRawText] = useState('');
  const [guided, setGuided] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const update = (key: keyof DepositRequest, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));

  const submit = async () => {
    setBusy(true);
    setErr(null);
    try {
      const graph = await api.deposit({ ...(guided ? form : {}), rawText, sky: sky.name });
      setGraph(graph);
      addFil({ kind: 'depot', text: rawText.trim() || 'un dépôt structuré' });
      navigate('/jardin');
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

      <p className="eyebrow" style={{ marginTop: '1.6rem' }}>
        choisis la couleur de ton ciel (humeur)
      </p>
      <div className={s.skyCards}>
        {skies.map((k) => (
          <button
            key={k.id}
            className={`${s.skyCard} ${k.id === sky.id ? s.skyCardOn : ''}`}
            aria-pressed={k.id === sky.id}
            onClick={() => setSky(k.id)}
          >
            <span className={s.skySwatch} style={{ background: k.swatch }} aria-hidden="true" />
            <span className={s.skyName}>{k.name}</span>
            <span className={s.skyDesc}>{k.description}</span>
          </button>
        ))}
      </div>

      <div className={s.writeCard}>
        <div className={s.writeHead}>
          <div>
            <h3 className={s.writeTitle}>déposer son poids</h3>
            <p className={s.muted}>écris sans fard, tes mots resteront secrets et locaux.</p>
          </div>
          <SoftButton
            variant="ghost"
            type="button"
            onClick={() => setGuided((g) => !g)}
            aria-pressed={guided}
          >
            {guided ? 'écriture libre' : 'être guidé'}
          </SoftButton>
        </div>

        <Field
          label="quelques mots libres"
          textarea
          value={rawText}
          onChange={(e) => setRawText(e.target.value)}
          placeholder="commence à taper ici… ex : « aujourd'hui, j'ai ressenti un poids au bureau quand mon responsable m'a interpelé… »"
        />

        {guided && (
          <>
            <p className={s.muted} style={{ marginBottom: '0.6rem' }}>
              nomme les racines toi-même :
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
          </>
        )}
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
