// Reporter configuration for `npx playwright merge-reports`, shared by the CI
// merge jobs (each platform's e2e-merge and the PR run's e2e-merge-all) so the
// merged-report reporter set is defined once. Not auto-discovered by
// `playwright test` (only playwright.config.* is); always passed via --config.
import { defineConfig } from '@playwright/test';

export default defineConfig({
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'playwright-report/test-results.json' }],
  ],
});
