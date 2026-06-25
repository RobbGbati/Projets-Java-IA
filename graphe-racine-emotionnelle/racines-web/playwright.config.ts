/**
 * Configuration Playwright pour racines-web.
 *
 * Prérequis : les deux serveurs doivent tourner AVANT de lancer les tests.
 *   – Front : npm run dev  → http://localhost:5173
 *   – Back  : java -jar racines-api.jar (mode in-memory) → http://localhost:8080
 *
 * Navigateur : Brave (Chromium), déjà installé sur macOS.
 * Pas de téléchargement de binaire Playwright nécessaire.
 */
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  /* Timeout global par test */
  timeout: 30_000,
  /* Timeout pour les assertions */
  expect: { timeout: 10_000 },
  /* Parallélisme activé (chaque spec dans son contexte isolé) */
  fullyParallel: true,
  /* Arrêt rapide en CI */
  forbidOnly: !!process.env.CI,
  /* 1 retry en CI pour l'instabilité des animations */
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],

  use: {
    baseURL: 'http://localhost:5173',
    /* Capture d'écran uniquement sur échec */
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'brave',
      use: {
        ...devices['Desktop Chrome'],
        channel: undefined,
        launchOptions: {
          executablePath: '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',
          args: ['--no-sandbox', '--disable-dev-shm-usage'],
        },
      },
    },
  ],

  /* Pas de webServer : les serveurs sont supposés tourner déjà. */
});
