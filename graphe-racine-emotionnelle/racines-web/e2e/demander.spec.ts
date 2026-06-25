/**
 * Tests E2E – /demander (interroger ses racines)
 *
 * Vérifie :
 *  1. Le hero "Interroger ses Racines" est présent
 *  2. Le champ aria-label="ta question" est présent
 *  3. Le bouton aria-label="demander" est présent
 *  4. Les 4 boutons d'exemples sont affichés et cliquables
 *  5. Cliquer un exemple déclenche la requête et affiche une réponse
 *     (le backend hors-ligne renvoie un message de repli — on vérifie
 *      qu'une div de réponse apparaît, peu importe le contenu)
 *  6. "ou révèle une racine commune" est présent
 */
import { test, expect } from '@playwright/test';

const EXEMPLES = [
  "qu'est-ce qui m'a déjà apaisé quand la tristesse est montée ?",
  'quelle croyance limitante revient le plus souvent sous mes tensions ?',
  'mes tensions au bureau ont-elles un lien avec ma famille ?',
  'quels sont mes besoins insatisfaits les plus fréquents ?',
];

test.describe('Demander – interroger ses racines', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/demander');
  });

  test('le hero "Interroger ses Racines" est visible', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /Interroger ses Racines/i })).toBeVisible();
  });

  test('le champ "ta question" est présent', async ({ page }) => {
    const input = page.getByLabel('ta question');
    await expect(input).toBeVisible();
    await expect(input).toBeEnabled();
  });

  test('le bouton "demander" est présent', async ({ page }) => {
    const btn = page.getByRole('button', { name: 'demander' });
    await expect(btn).toBeVisible();
  });

  test('les 4 cartes d\'exemples sont affichées', async ({ page }) => {
    for (const exemple of EXEMPLES) {
      const btn = page.getByRole('button', { name: exemple });
      await expect(btn).toBeVisible();
    }
  });

  test('cliquer un exemple déclenche la requête et affiche une réponse', async ({ page }) => {
    // Clic sur le premier exemple
    const firstExemple = EXEMPLES[0];
    const exempleBtn = page.getByRole('button', { name: firstExemple });
    await exempleBtn.click();

    // Attendre qu'une réponse apparaisse (succès ou erreur de repli).
    // La div de réponse a une classe CSS Module hachée contenant "answer".
    // On utilise un sélecteur CSS standard sans préfixe "css=".
    // On cherche soit la div de réponse, soit un paragraphe d'erreur.
    await expect(
      page.locator('[class*="answer"], p[style*="corail"]').first()
    ).toBeVisible({ timeout: 20_000 });
  });

  test('taper une question dans le champ et envoyer avec Entrée', async ({ page }) => {
    const input = page.getByLabel('ta question');
    await input.fill('quelle est ma principale source de stress ?');
    await input.press('Enter');

    // Attente d'une réponse quelconque (div de réponse ou erreur)
    await expect(
      page.locator('[class*="answer"], p[style*="corail"]').first()
    ).toBeVisible({ timeout: 20_000 });
  });

  test('"ou révèle une racine commune" est présent', async ({ page }) => {
    const btn = page.getByRole('button', { name: /révèle une racine commune/i });
    await expect(btn).toBeVisible();
  });
});
