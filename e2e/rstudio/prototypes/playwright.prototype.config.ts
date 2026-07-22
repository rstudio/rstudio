import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

// Standalone config for the Copilot authorize-step prototype. It deliberately
// does NOT reuse the main playwright.config.ts: this prototype talks only to
// GitHub through a plain browser, so it must not pull in the sandbox
// globalSetup, the Posit AI `setup` project, or the RStudio fixture. Run it
// from e2e/rstudio with:
//
//   npx playwright test --config=prototypes/playwright.prototype.config.ts
//
// Credentials come from e2e/rstudio/.env.local (the same file the main suite
// uses); dotenv is loaded here because the main config never runs on this path.
dotenv.config({ path: path.resolve(__dirname, '..', '.env.local') });

export default defineConfig({
  testDir: __dirname,
  testMatch: /(copilot-authorize|copilot-agent-signin)\.spec\.ts$/,
  fullyParallel: false,
  workers: 1,
  // The flow launches a browser, drives a live GitHub sign-in, waits out the
  // authorize-button delay (up to 120s), and polls for a token; 5 minutes
  // covers all of that with headroom.
  timeout: 300_000,
  retries: 0,
  reporter: [['list']],
  use: {
    // retain-on-failure, not on: a passing run has nothing to inspect, and a
    // trace captures the typed password. A failure keeps one so GitHub's actual
    // page markup (which changes) is visible for the next iteration.
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
});
