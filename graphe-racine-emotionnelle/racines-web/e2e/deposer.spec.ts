/**
 * Tests E2E – /deposer (dépôt d'une entrée)
 *
 * Vérifie :
 *  1. Les 5 cartes de ciel sont présentes (Aurore, Zénith, Crépuscule, Nuit Étoilée, Orage Sauge)
 *  2. Sélectionner un ciel met à jour l'état aria-pressed
 *  3. La bascule "être guidé" révèle des champs structurés
 *  4. Le bouton "déposer" est présent
 *  5. (optionnel) Déposer un texte renvoie au jardin
 */
import { test, expect } from '@playwright/test';

const SKIES = ['Aurore', 'Zénith', 'Crépuscule', 'Nuit Étoilée', 'Orage Sauge'];

test.describe('Déposer – dépôt d\'entrée', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/deposer');
  });

  test('les 5 cartes de ciel sont présentes', async ({ page }) => {
    for (const sky of SKIES) {
      // Les cartes sont des boutons contenant le nom du ciel
      const btn = page.getByRole('button', { name: new RegExp(sky, 'i') }).first();
      await expect(btn).toBeVisible();
    }
  });

  test('sélectionner "Aurore" met aria-pressed à true', async ({ page }) => {
    const auroreBtn = page.getByRole('button', { name: /Aurore/i }).first();
    await auroreBtn.click();
    await expect(auroreBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('sélectionner "Nuit Étoilée" met aria-pressed à true', async ({ page }) => {
    const nuitBtn = page.getByRole('button', { name: /Nuit Étoilée/i }).first();
    await nuitBtn.click();
    await expect(nuitBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('le bouton "être guidé" révèle les champs structurés', async ({ page }) => {
    // Avant : les champs guidés ne sont pas là
    const guidedBtn = page.getByRole('button', { name: /être guidé/i });
    await expect(guidedBtn).toBeVisible();

    // Les champs guidés ne sont pas encore affichés
    const emotionField = page.getByLabel("l'émotion");
    await expect(emotionField).not.toBeVisible();

    // Activer le mode guidé
    await guidedBtn.click();

    // Les champs structurés doivent apparaître
    await expect(emotionField).toBeVisible({ timeout: 3_000 });
    await expect(page.getByLabel('la situation')).toBeVisible();
    await expect(page.getByLabel('la croyance / pensée')).toBeVisible();

    // Le bouton change de label
    await expect(page.getByRole('button', { name: /écriture libre/i })).toBeVisible();
  });

  test('le champ "quelques mots libres" accepte du texte', async ({ page }) => {
    const textarea = page.getByLabel('quelques mots libres');
    await expect(textarea).toBeVisible();
    await textarea.fill("j'ai ressenti de l'anxiété ce matin en allant au travail");
    await expect(textarea).toHaveValue(/anxiété/);
  });

  test('le bouton "déposer" est présent et activable', async ({ page }) => {
    const btn = page.getByRole('button', { name: /^déposer$/i });
    await expect(btn).toBeVisible();
    await expect(btn).toBeEnabled();
  });

  test('déposer un texte et revenir au jardin (intégration backend)', async ({ page }) => {
    const textarea = page.getByLabel('quelques mots libres');
    await textarea.fill('test e2e : une légère tristesse au réveil');

    const deposerBtn = page.getByRole('button', { name: /^déposer$/i });
    await deposerBtn.click();

    // Après dépôt réussi → navigate('/jardin')
    await expect(page).toHaveURL(/\/jardin/, { timeout: 15_000 });
  });
});
