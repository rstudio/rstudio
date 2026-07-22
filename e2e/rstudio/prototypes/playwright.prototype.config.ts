import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

// Standalone config for the Copilot sign-in smoke test
// (copilot-agent-signin.spec.ts). It deliberately does NOT reuse the main
// playwright.config.ts: the spec drives the copilot-language-server and a
// plain browser against GitHub, with no RStudio involved, so it must not pull
// in the sandbox globalSetup, the `setup` auth project, or the RStudio
// fixture. Run it from e2e/rstudio with:
//
//   npx playwright test --config=prototypes/playwright.prototype.config.ts
//
// Credentials come from e2e/rstudio/.env.local (the same file the main suite
// uses); dotenv is loaded here because the main config never runs on this path.
dotenv.config({ path: path.resolve(__dirname, '..', '.env.local') });

export default defineConfig({
  testDir: __dirname,
  testMatch: /copilot-agent-signin\.spec\.ts$/,
  fullyParallel: false,
  workers: 1,
  // The flow spawns the agent, drives a live GitHub sign-in (including the
  // authorize-button delay, up to 120s), and waits for the agent's own token
  // poll; 5 minutes covers all of that with headroom.
  timeout: 300_000,
  retries: 0,
  reporter: [['list']],
  use: {
    // Artifacts stay off, matching the main config's `setup` project: real
    // credentials are typed into the pages this spec drives, and a trace
    // records fill() values (the password itself). Use PW_DEBUG_AUTH_STEPS
    // and PW_DEBUG_AUTH_CAPTURE for diagnostics instead (see
    // utils/auth-debug.ts).
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
});
