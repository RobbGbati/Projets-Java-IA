/**
 * Capture des écrans de l'app dans docs/ (Playwright, Chromium intégré).
 * Prérequis : front sur http://localhost:5173 (et backend :8080 pour le Jardin).
 * Lancer :  npm run screenshots
 */
import { chromium } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT = resolve(__dirname, '..', 'docs');
const BASE = process.env.BASE_URL ?? 'http://localhost:5173';

/** [route, nom de fichier] */
const ROUTES = [
  ['jardin', 'jardin'],
  ['deposer', 'deposer'],
  ['demander', 'demander'],
  ['valider', 'valider'],
  ['fil', 'fil'],
];

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 1366, height: 900 },
  deviceScaleFactor: 2, // images nettes (retina)
});
const page = await context.newPage();

for (const [route, name] of ROUTES) {
  await page.goto(`${BASE}/${route}`, { waitUntil: 'networkidle' });
  if (route === 'jardin') {
    // attendre les nœuds + laisser la disposition d3-force se poser
    await page.waitForSelector('circle[role="button"]', { timeout: 10_000 }).catch(() => {});
    await page.waitForTimeout(3000);
  } else {
    await page.waitForTimeout(700);
  }
  const file = resolve(OUT, `${name}.png`);
  await page.screenshot({ path: file });
  console.log(`✓ ${name}.png`);
}

await browser.close();
console.log(`\nCaptures enregistrées dans ${OUT}`);
