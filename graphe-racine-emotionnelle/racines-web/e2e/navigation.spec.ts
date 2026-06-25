/**
 * Tests E2E – Navigation principale
 *
 * Vérifie :
 *  1. / redirige vers /jardin
 *  2. La nav du bas route vers /deposer, /demander, /valider, /fil
 *  3. Le bouton ✕ "fermer cette carte" ramène au /jardin
 */
import { test, expect } from '@playwright/test';

test.describe('Navigation principale', () => {
  test('/ redirige automatiquement vers /jardin', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/jardin/);
  });

  test('lien "le jardin" dans la nav est actif sur /jardin', async ({ page }) => {
    await page.goto('/jardin');
    const nav = page.getByRole('navigation', { name: 'navigation principale' });
    await expect(nav).toBeVisible();
    const jardinLink = nav.getByRole('link', { name: 'le jardin' });
    await expect(jardinLink).toBeVisible();
  });

  test('nav → /deposer change l\'URL', async ({ page }) => {
    await page.goto('/jardin');
    const nav = page.getByRole('navigation', { name: 'navigation principale' });
    await nav.getByRole('link', { name: 'déposer' }).click();
    await expect(page).toHaveURL(/\/deposer/);
  });

  test('nav → /demander change l\'URL', async ({ page }) => {
    await page.goto('/jardin');
    const nav = page.getByRole('navigation', { name: 'navigation principale' });
    await nav.getByRole('link', { name: 'demander' }).click();
    await expect(page).toHaveURL(/\/demander/);
  });

  test('nav → /valider change l\'URL', async ({ page }) => {
    await page.goto('/jardin');
    const nav = page.getByRole('navigation', { name: 'navigation principale' });
    await nav.getByRole('link', { name: 'valider' }).click();
    await expect(page).toHaveURL(/\/valider/);
  });

  test('nav → /fil change l\'URL', async ({ page }) => {
    await page.goto('/jardin');
    const nav = page.getByRole('navigation', { name: 'navigation principale' });
    await nav.getByRole('link', { name: 'le fil' }).click();
    await expect(page).toHaveURL(/\/fil/);
  });

  test('le bouton ✕ "fermer cette carte" ramène au jardin depuis /deposer', async ({ page }) => {
    await page.goto('/deposer');
    const closeBtn = page.getByRole('button', { name: 'fermer cette carte' });
    await expect(closeBtn).toBeVisible();
    await closeBtn.click();
    await expect(page).toHaveURL(/\/jardin/);
  });

  test('le bouton ✕ "fermer cette carte" ramène au jardin depuis /demander', async ({ page }) => {
    await page.goto('/demander');
    const closeBtn = page.getByRole('button', { name: 'fermer cette carte' });
    await expect(closeBtn).toBeVisible();
    await closeBtn.click();
    await expect(page).toHaveURL(/\/jardin/);
  });

  test('le bouton ✕ "fermer cette carte" ramène au jardin depuis /valider', async ({ page }) => {
    await page.goto('/valider');
    const closeBtn = page.getByRole('button', { name: 'fermer cette carte' });
    await expect(closeBtn).toBeVisible();
    await closeBtn.click();
    await expect(page).toHaveURL(/\/jardin/);
  });
});
