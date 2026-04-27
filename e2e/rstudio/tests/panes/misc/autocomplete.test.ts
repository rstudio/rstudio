import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AutocompleteActions } from '@actions/autocomplete.actions';

// Ground truth from: src/cpp/tests/automation/testthat/test-automation-completions.R (BRAT)

const contexts = ['console', 'editor'] as const;
type Context = (typeof contexts)[number];

for (const context of contexts) {
  test.describe(`Autocomplete [${context}]`, () => {
    let consoleActions: ConsolePaneActions;
    let sourceActions: SourcePaneActions;
    let autocomplete: AutocompleteActions;

    test.beforeAll(async ({ rstudioPage: page }) => {
      consoleActions = new ConsolePaneActions(page);
      sourceActions = new SourcePaneActions(page, consoleActions);
      autocomplete = new AutocompleteActions(page, consoleActions, sourceActions);
      await consoleActions.closeAllBuffersWithoutSaving();
      await consoleActions.typeInConsole('rm(list = ls())');
      await sleep(500);
    });

    // Safety: ensure clean editor state before each editor test
    test.beforeEach(async () => {
      if (context === 'editor') {
        await consoleActions.closeAllBuffersWithoutSaving();
      }
    });

    // Safety: dismiss lingering popups or partial input between tests
    test.afterEach(async ({ rstudioPage: page }) => {
      await page.keyboard.press('Escape');
      await sleep(200);
      await page.keyboard.press('Escape');
      await sleep(200);
      if (context === 'editor') {
        await consoleActions.closeAllBuffersWithoutSaving();
      }
    });

    /** Get completions in the current context. */
    async function getCompletions(
      setupCode: string[],
      triggerText: string,
      cursorLine?: number,
      cursorCol?: number,
    ): Promise<string[]> {
      if (context === 'console') {
        return autocomplete.getCompletionsInConsole(setupCode, triggerText);
      } else {
        return autocomplete.getCompletionsInEditor(setupCode, triggerText, cursorLine, cursorCol);
      }
    }

    // -----------------------------------------------------------------------
    // Issue 13196 — Function parameter completions
    // https://github.com/rstudio/rstudio/issues/13196
    // -----------------------------------------------------------------------
    test.describe('Issue 13196 - Function parameter completions', () => {

      test('base function: cat(', async ({ rstudioPage: page }) => {
        const items = await getCompletions([], 'cat(');
        expect(items).toEqual(expect.arrayContaining(['... =', 'file =', 'sep =', 'fill =', 'labels =', 'append =']));
        expect(items).toHaveLength(6);
      });

      test('non-base function: stats::rnorm(', async ({ rstudioPage: page }) => {
        const items = await getCompletions([], 'stats::rnorm(');
        expect(items).toEqual(expect.arrayContaining(['n =', 'mean =', 'sd =']));
        expect(items).toHaveLength(3);
      });

      test('user-defined function', async ({ rstudioPage: page }) => {
        const items = await getCompletions(
          ['a <- function(x, y, z) { print(x + y) }'],
          'a(',
        );
        expect(items).toEqual(expect.arrayContaining(['x =', 'y =', 'z =']));
        expect(items).toHaveLength(3);
      });
    });

    // -----------------------------------------------------------------------
    // Issue 13291 — List $ completions with data preview
    // https://github.com/rstudio/rstudio/issues/13291
    // -----------------------------------------------------------------------
    test.describe('Issue 13291 - List $ completions', () => {

      test('list member names appear after $', async ({ rstudioPage: page }) => {
        const items = await getCompletions(
          [
            'test_df <- data.frame(col1 = rep(1, 3), col2 = rep(2, 3), col3 = rep(3, 3))',
            'test_ls <- list(a = test_df, b = test_df)',
          ],
          'test_ls$',
        );
        expect(items).toEqual(expect.arrayContaining(['a', 'b']));
        expect(items).toHaveLength(2);
      });
    });

    // -----------------------------------------------------------------------
    // Issue 12678 — Completion ordering (local variables first)
    // https://github.com/rstudio/rstudio/issues/12678
    // -----------------------------------------------------------------------
    test.describe('Issue 12678 - Completion ordering', () => {
      let missingPackages: string[] = [];

      test.beforeAll(async () => {
        missingPackages = await consoleActions.ensurePackages(['dplyr']);
      });

      test('local variables show up first', async ({ rstudioPage: page }) => {
        test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
        const items = await getCompletions(
          ['library(dplyr)', 'left_table <- tibble(x = 1)'],
          'lef',
        );
        expect(items).toContain('left_table');
        expect(items).toContain('left_join');
        // Local variable must appear before the package function (regardless of popup direction)
        const localIdx = items.indexOf('left_table');
        const pkgIdx = items.indexOf('left_join');
        expect(Math.abs(localIdx - pkgIdx)).toBe(1);
      });
    });

    // -----------------------------------------------------------------------
    // Issue 12918 — data.table $ completions without bogus error
    // https://github.com/rstudio/rstudio/issues/12918
    // -----------------------------------------------------------------------
    test.describe('Issue 12918 - data.table $ completions', () => {
      let missingPackages: string[] = [];

      test.beforeAll(async () => {
        missingPackages = await consoleActions.ensurePackages(['data.table', 'pillar']);
      });

      test('$ completions appear without error', async ({ rstudioPage: page }) => {
        test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
        const items = await getCompletions(
          [
            'library(data.table)',
            'library(pillar)',
            'StartDateTime <- as.POSIXct("2023-01-01", origin="1970-01-01")',
            'DateTime10 <- seq(StartDateTime, StartDateTime+10, length.out=11)',
            'test.df <- as.data.table(cbind(c(1:11), DateTime10, shift(DateTime10)))',
            'test.df <- cbind(test.df, test.df$V3)',
            'test.df$V2 <- as.POSIXct(test.df$V2, origin = "1960-01-01")',
          ],
          'test.df$',
        );
        // Main assertion: completions appeared (no bogus error killed the popup)
        expect(items.length).toBeGreaterThan(0);
        expect(items).toContain('V1');
        expect(items).toContain('DateTime10');
      });
    });

    // -----------------------------------------------------------------------
    // Issue 14625 — Unicode column names in completions
    // https://github.com/rstudio/rstudio/issues/14625
    // -----------------------------------------------------------------------
    test.describe('Issue 14625 - Unicode column names', () => {
      let missingPackages: string[] = [];

      test.beforeAll(async () => {
        missingPackages = await consoleActions.ensurePackages(['tibble']);
      });

      test('Unicode column names appear in completions', async ({ rstudioPage: page }) => {
        test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
        const items = await getCompletions(
          [
            'library(tibble)',
            'd <- tibble(ちちち = rnorm(10), 面面面 = rnorm(10))',
          ],
          'd$',
        );
        expect(items).toContain('ちちち');
        expect(items).toContain('面面面');
      });
    });
  });
}
