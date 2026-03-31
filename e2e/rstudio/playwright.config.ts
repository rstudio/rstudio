import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 300000,
  retries: 0,
  reporter: [['html'], ['list']],
  use: {
    trace: 'on-first-retry',
    actionTimeout: 10000,
  },
});
