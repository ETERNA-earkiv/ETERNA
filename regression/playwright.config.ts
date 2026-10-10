import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { defineConfig } from '@playwright/test';
import { loadConfig } from './src/config';

// Variabler som redan finns i miljön har företräde framför .env.
const envFile = join(__dirname, '.env');
if (existsSync(envFile)) process.loadEnvFile(envFile);

const cfg = loadConfig();

export default defineConfig({
  testDir: './tests',
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  timeout: 120_000,
  // I CI avbryts körningen efter fem fel, så att en följdfelskaskad inte tar en timme.
  maxFailures: process.env.CI ? 5 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: cfg.url,
    extraHTTPHeaders: { Accept: 'application/json' },
  },
  projects: [
    { name: 'unit', testDir: './tests/unit' },
    { name: 'smoke', testDir: './tests/smoke', retries: 1 },
    { name: 'core', testDir: './tests/core', dependencies: ['smoke'], retries: 0 },
  ],
});
