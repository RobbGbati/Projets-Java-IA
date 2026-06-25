/**
 * Le Fil (SPEC §3, écran 5). Journal chronologique des dépôts, questions et
 * révélations. Lecture, pas métrique : aucune « performance émotionnelle »,
 * aucun streak (PRD §3.1).
 */
import s from './screens.module.css';
import { Invitation } from '../ui/primitives';
import { useGraphStore } from '../store/useGraphStore';
import type { FilItem } from '../store/useGraphStore';

const KIND_LABEL: Record<FilItem['kind'], string> = {
  depot: 'un dépôt',
  question: 'une question',
  revelation: 'une révélation',
};

function when(ts: number): string {
  return new Date(ts).toLocaleString('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function Fil() {
  const fil = useGraphStore((st) => st.fil);

  return (
    <div>
      <h2 className={s.title}>le fil</h2>
      <Invitation>le chemin parcouru, dans l'ordre où il est venu.</Invitation>

      {fil.length === 0 ? (
        <p className={s.muted} style={{ marginTop: '1.4rem' }}>
          rien encore. ce que tu déposeras s'inscrira ici, doucement.
        </p>
      ) : (
        <div style={{ marginTop: '1.2rem' }}>
          {fil.map((item) => (
            <div key={item.id} className={s.filItem}>
              <span className={s.filWhen}>{when(item.at)}</span>
              <p className={s.filText}>
                <span className={s.filKind}>{KIND_LABEL[item.kind]}</span> · {item.text}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
