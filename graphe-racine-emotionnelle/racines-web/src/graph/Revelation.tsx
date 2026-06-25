/**
 * La révélation de racine commune — l'élément signature (SPEC §2, §4).
 * Timeline GSAP : 1) tout s'assombrit autour, 2) les situations s'illuminent,
 * 3) une racine CORAIL pousse de chaque situation vers la croyance partagée,
 * 4) pulsation + apparition de la phrase-invitation.
 *
 * Le corail n'apparaît QUE là : c'est l'accent rare qui donne son poids au moment.
 * reduced-motion : on montre l'état final d'emblée, sans timeline.
 */
import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import styles from './Revelation.module.css';
import { useGraphStore } from '../store/useGraphStore';
import type { Positions } from './ForceLayout';
import { useReducedMotion } from '../theme/motion';
import { revelationPhrase } from '../api/types';

interface Props {
  positions: Positions;
  transform: { x: number; y: number; k: number };
}

export function Revelation({ positions, transform }: Props) {
  const revelation = useGraphStore((s) => s.revelation);
  const setRevelation = useGraphStore((s) => s.setRevelation);
  const reduced = useReducedMotion();

  const veilRef = useRef<SVGRectElement>(null);
  const groupRef = useRef<SVGGElement>(null);
  const textRef = useRef<HTMLParagraphElement>(null);
  const pathRefs = useRef<SVGPathElement[]>([]);
  const beliefRef = useRef<SVGCircleElement>(null);

  // positions monde → écran (mêmes maths que le <g> de Garden)
  const toScreen = (id: string) => {
    const p = positions[id];
    if (!p) return null;
    return { x: transform.x + p.x * transform.k, y: transform.y + p.y * transform.k };
  };

  useEffect(() => {
    if (!revelation) return;
    pathRefs.current = pathRefs.current.filter(Boolean);

    if (reduced) {
      // état final immédiat
      gsap.set(veilRef.current, { opacity: 0.62 });
      gsap.set([groupRef.current, textRef.current], { opacity: 1 });
      pathRefs.current.forEach((p) => gsap.set(p, { strokeDashoffset: 0 }));
      return;
    }

    const ctx = gsap.context(() => {
      const tl = gsap.timeline();
      // 1) tout s'assombrit autour
      tl.fromTo(veilRef.current, { opacity: 0 }, { opacity: 0.62, duration: 0.9, ease: 'power2.out' });
      // 2) les situations + la croyance s'illuminent
      tl.fromTo(groupRef.current, { opacity: 0 }, { opacity: 1, duration: 0.6 }, '-=0.3');
      // 3) la racine corail pousse vers la croyance partagée
      pathRefs.current.forEach((p) => {
        const len = p.getTotalLength();
        gsap.set(p, { strokeDasharray: len, strokeDashoffset: len });
      });
      tl.to(pathRefs.current, {
        strokeDashoffset: 0,
        duration: 1.1,
        ease: 'power1.inOut',
        stagger: 0.25,
      });
      // 4) pulsation de la croyance + apparition de la phrase
      tl.to(beliefRef.current, {
        scale: 1.25,
        transformOrigin: 'center',
        repeat: 3,
        yoyo: true,
        duration: 0.55,
        ease: 'sine.inOut',
      });
      tl.fromTo(
        textRef.current,
        { opacity: 0, y: 10 },
        { opacity: 1, y: 0, duration: 0.8, ease: 'power2.out' },
        '-=1.4',
      );
    });
    return () => ctx.revert();
  }, [revelation, reduced]);

  if (!revelation) return null;

  const belief = toScreen(revelation.belief.id);
  pathRefs.current = [];

  return (
    <div className={styles.overlay} role="dialog" aria-label="une racine commune">
      <svg className={styles.svg}>
        <rect ref={veilRef} x={0} y={0} width="100%" height="100%" fill="#1c130a" opacity={0} />
        <g ref={groupRef} opacity={0}>
          {belief &&
            revelation.situations.map((sit, i) => {
              const s = toScreen(sit.id);
              if (!s) return null;
              const d = `M ${s.x} ${s.y} L ${belief.x} ${belief.y}`;
              return (
                <g key={sit.id}>
                  <circle cx={s.x} cy={s.y} r={20} fill="none" stroke="var(--corail)" strokeWidth={2.5} />
                  <path
                    ref={(el) => {
                      if (el) pathRefs.current[i] = el;
                    }}
                    d={d}
                    fill="none"
                    stroke="var(--corail)"
                    strokeWidth={3}
                    strokeLinecap="round"
                  />
                </g>
              );
            })}
          {belief && (
            <circle
              ref={beliefRef}
              cx={belief.x}
              cy={belief.y}
              r={28}
              fill="none"
              stroke="var(--corail)"
              strokeWidth={3.5}
            />
          )}
        </g>
      </svg>

      <p className={styles.invitation} ref={textRef}>
        {revelationPhrase(revelation)}
      </p>
      <button
        className={styles.dismiss}
        onClick={() => setRevelation(null)}
        style={{ pointerEvents: 'auto' }}
      >
        je laisse infuser
      </button>
    </div>
  );
}
