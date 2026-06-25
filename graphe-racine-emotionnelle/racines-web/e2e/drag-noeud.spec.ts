/**
 * Tests E2E – Drag d'un nœud dans le jardin
 *
 * Le drag est implémenté via onPointerDown/onPointerMove/onPointerUp sur les
 * <circle> (voir RootNode.tsx). setPointerCapture est utilisé, ce qui signifie
 * que pointermove doit être reçu sur l'élément capturé.
 *
 * Playwright page.mouse.move/down/up déclenche les pointer events en Chromium.
 * On vérifie que la position du nœud (boundingBox) a changé après le drag.
 * Si d3 n'a pas tické (positions non mises à jour), on inspecte l'attribut
 * transform du <g> parent du cercle.
 */
import { test, expect } from '@playwright/test';

test.describe('Drag d\'un nœud – carte vivante', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/jardin');
  });

  test('glisser un nœud déplace sa position dans le SVG', async ({ page }) => {
    // Attendre qu'au moins un nœud soit présent et que le layout d3 soit stable
    const firstCircle = page.locator('circle[role="button"]').first();
    await expect(firstCircle).toBeVisible({ timeout: 12_000 });

    // Attendre que la simulation se stabilise (alphaDecay=0.026, ~2-3s)
    await page.waitForTimeout(3000);

    // Position du transform AVANT le drag
    // Le <g> ayant translate(x,y) est le parent direct du motion.g → circle
    // Récupérer via evaluate pour lire l'attribut SVG
    const transformBefore = await firstCircle.evaluate((el) => {
      // Remonter jusqu'au <g transform="translate(...)"> le plus proche
      let node: Element | null = el.parentElement;
      while (node && !node.getAttribute('transform')?.startsWith('translate')) {
        node = node.parentElement;
      }
      return node?.getAttribute('transform') ?? '';
    });

    // Extraire les coordonnées du transform
    const parseTr = (tr: string): { x: number; y: number } | null => {
      const m = tr.match(/translate\(\s*([\d.-]+)[,\s]+([\d.-]+)\s*\)/);
      if (!m) return null;
      return { x: parseFloat(m[1]), y: parseFloat(m[2]) };
    };

    const posBefore = parseTr(transformBefore);
    expect(posBefore).not.toBeNull();

    // Bounding box du cercle dans le viewport pour placer le pointeur
    const box = await firstCircle.boundingBox();
    expect(box).not.toBeNull();
    const cx = box!.x + box!.width / 2;
    const cy = box!.y + box!.height / 2;

    // Drag via pointer events : on utilise dispatchEvent pour être certain
    // que les pointer events atterrissent sur le bon élément avec capture
    await page.evaluate(
      ({ cx, cy, dx, dy }: { cx: number; cy: number; dx: number; dy: number }) => {
        const circle = document.querySelector('circle[role="button"]') as SVGCircleElement | null;
        if (!circle) return;

        const makePointerEvent = (type: string, x: number, y: number) =>
          new PointerEvent(type, {
            bubbles: true,
            cancelable: true,
            pointerId: 1,
            clientX: x,
            clientY: y,
            pointerType: 'mouse',
            isPrimary: true,
          });

        // pointerdown sur le circle
        circle.dispatchEvent(makePointerEvent('pointerdown', cx, cy));

        // pointermove progressif
        const steps = 15;
        for (let i = 1; i <= steps; i++) {
          const ratio = i / steps;
          circle.dispatchEvent(
            makePointerEvent('pointermove', cx + dx * ratio, cy + dy * ratio),
          );
        }

        // pointerup
        circle.dispatchEvent(makePointerEvent('pointerup', cx + dx, cy + dy));
      },
      { cx, cy, dx: 100, dy: 80 },
    );

    // Attendre que d3 tick propage la position
    await page.waitForTimeout(800);

    // Lire le transform après le drag
    const transformAfter = await firstCircle.evaluate((el) => {
      let node: Element | null = el.parentElement;
      while (node && !node.getAttribute('transform')?.startsWith('translate')) {
        node = node.parentElement;
      }
      return node?.getAttribute('transform') ?? '';
    });

    const posAfter = parseTr(transformAfter);
    expect(posAfter).not.toBeNull();

    // La position doit avoir bougé d'au moins 5 unités SVG
    const dist = Math.hypot(posAfter!.x - posBefore!.x, posAfter!.y - posBefore!.y);
    expect(dist).toBeGreaterThan(5);
  });

  test('un clic simple (sans glisser) ouvre l\'histoire du nœud', async ({ page }) => {
    const firstCircle = page.locator('circle[role="button"]').first();
    await expect(firstCircle).toBeVisible({ timeout: 12_000 });

    // Clic simple → moved = false → selectNode(id)
    await firstCircle.click({ force: true });

    const aside = page.getByRole('complementary', { name: "l'histoire du nœud" });
    await expect(aside).toBeVisible({ timeout: 5_000 });
  });
});
