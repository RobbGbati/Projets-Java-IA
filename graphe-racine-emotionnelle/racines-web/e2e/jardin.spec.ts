/**
 * Tests E2E – /jardin (la carte vivante)
 *
 * Vérifie :
 *  1. Des nœuds (circle[role="button"]) sont chargés depuis GET /api/graph
 *  2. La légende "code couleur des racines" est visible une fois les nœuds présents
 *  3. Le bouton "réactualiser" est présent et actif
 *  4. Cliquer un nœud ouvre le panneau aside "l'histoire du nœud"
 */
import { test, expect } from '@playwright/test';

test.describe('Jardin – la carte vivante', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/jardin');
  });

  test('des nœuds sont présents après chargement du graphe', async ({ page }) => {
    // Attendre que des cercles soient dessinés (le backend seed ~28 nœuds)
    const circles = page.locator('circle[role="button"]');
    await expect(circles.first()).toBeVisible({ timeout: 12_000 });
    const count = await circles.count();
    expect(count).toBeGreaterThan(0);
  });

  test('la légende "code couleur des racines" est visible', async ({ page }) => {
    // La légende n'apparaît que si nodes.length > 0 (cf. Legend.tsx)
    const legend = page.getByRole('list', { name: 'code couleur des racines' });
    await expect(legend).toBeVisible({ timeout: 12_000 });
    // Au moins un item dans la légende
    const items = legend.locator('li');
    await expect(items.first()).toBeVisible();
  });

  test('le bouton réactualiser est présent', async ({ page }) => {
    const btn = page.getByRole('button', { name: /réactualiser/i });
    await expect(btn).toBeVisible();
    await expect(btn).toBeEnabled();
  });

  test('cliquer un nœud ouvre "l\'histoire du nœud"', async ({ page }) => {
    // Attendre qu'un nœud soit là
    const firstNode = page.locator('circle[role="button"]').first();
    await expect(firstNode).toBeVisible({ timeout: 12_000 });

    // Clic simple sur le premier nœud
    await firstNode.click({ force: true });

    // Le panneau latéral doit apparaître
    const aside = page.getByRole('complementary', { name: "l'histoire du nœud" });
    await expect(aside).toBeVisible({ timeout: 5_000 });

    // Fermer le panneau
    const closeAside = aside.getByRole('button', { name: 'fermer' });
    await closeAside.click();
    await expect(aside).not.toBeVisible({ timeout: 3_000 });
  });
});
