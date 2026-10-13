import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';

/**
 * Accessibility scan of a new Server session with default preferences.
 * Future states follow the same shape: drive the UI from the fresh session,
 * then scan. A state test that changes prefs or layout must restore them --
 * on CI they persist into later tests (see README, Accessibility Tests).
 */
test('initial state has no WCAG 2.2 AA violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage }, testInfo) => {
    const results = await scanA11y(a11yPage, testInfo);
    expect(violationSummary(results)).toEqual([]);
  });
