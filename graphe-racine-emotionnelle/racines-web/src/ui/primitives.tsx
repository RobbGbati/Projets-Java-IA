/**
 * Primitives douces (SPEC §6). Les mots sont du matériau de design (SPEC §8) :
 * ces composants portent la voix calme et invitante de l'app.
 */
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  TextareaHTMLAttributes,
} from 'react';
import { useId } from 'react';
import styles from './ui.module.css';

interface FieldProps {
  label: string;
  children?: ReactNode;
}

/** Un champ texte simple, labellisé (accessibilité). */
export function Field({
  label,
  textarea,
  ...rest
}: FieldProps & { textarea?: boolean } & InputHTMLAttributes<HTMLInputElement> &
  TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      {textarea ? (
        <textarea id={id} className={styles.textarea} {...rest} />
      ) : (
        <input id={id} className={styles.input} {...rest} />
      )}
    </div>
  );
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'solid' | 'ghost' | 'accent';
};

export function SoftButton({ variant = 'solid', className, ...rest }: ButtonProps) {
  const variantClass =
    variant === 'ghost' ? styles.ghost : variant === 'accent' ? styles.accent : '';
  return (
    <button
      className={`${styles.button} ${variantClass} ${className ?? ''}`}
      {...rest}
    />
  );
}

/** Une phrase-invitation, ton « ami sage », italique Fraunces. */
export function Invitation({ children }: { children: ReactNode }) {
  return <p className={styles.invitation}>{children}</p>;
}
