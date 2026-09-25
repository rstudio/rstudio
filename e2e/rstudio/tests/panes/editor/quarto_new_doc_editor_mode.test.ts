import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted, CONFIRM_BTN } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import { sleep } from '@utils/constants';
import type { Page } from 'playwright';

// https://github.com/rstudio/rstudio/issues/11506
//
// With "Use visual editor by default for new documents" on, unchecking "Use
// visual markdown editor" in the New Quarto Document dialog still opened the
// document in visual mode: the dialog wrote no editor field, so the new doc
// fell back to the visual default. It now writes `editor: source`. The
// interactive templates (Shiny, Observable JS) offer the same checkbox.

const VISUAL_DEFAULT_PREF = 'visual_markdown_editing_is_default';

// Format radio to pick under the Interactive template; null keeps the default
// Document template and its default format.
const TEMPLATES: ReadonlyArray<readonly [label: string, interactiveFormat: string | null]> = [
  ['document', null],
  ['Shiny document', 'Shiny'],
  ['Observable JS document', 'Observable JS'],
];

async function createQuartoDoc(
  page: Page,
  interactiveFormat: string | null,
  visual: boolean,
): Promise<void> {
  await executeCommand(page, 'newQuartoDoc');
  await installDepIfPrompted(page, 2500);

  const okBtn = page.locator(CONFIRM_BTN);
  await expect(okBtn).toBeEnabled({ timeout: 20000 });
  if (interactiveFormat !== null) {
    await page.getByText('Interactive', { exact: true }).click();
    await page.getByRole('radio', { name: interactiveFormat, exact: true }).check();
  }
  await page.getByRole('checkbox', { name: 'Use visual markdown editor' }).setChecked(visual);
  await okBtn.click();
}

async function activeDocText(page: Page): Promise<string> {
  return page.evaluate(() => window.rstudio?.documents.activeEditor()?.getValue() ?? '');
}

test.describe('New Quarto document editor mode', () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
    await setPref(page, VISUAL_DEFAULT_PREF, true);
  });

  test.afterEach(async () => {
    await consoleActions.resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, VISUAL_DEFAULT_PREF);
  });

  for (const [label, interactiveFormat] of TEMPLATES) {
    test(`${label} opens in source mode when visual editor is unchecked`, async ({ rstudioPage: page }) => {
      await createQuartoDoc(page, interactiveFormat, false);

      const toggle = sourceActions.sourcePane.visualMdToggle;
      await expect(toggle).toBeVisible({ timeout: 20000 });
      await expect.poll(() => activeDocText(page), { timeout: 20000 }).toContain('title:');
      expect(await activeDocText(page)).toMatch(/^editor: source$/m);

      // Visual mode is applied asynchronously after the editor mounts, so a
      // single "not pressed" read could pass on a broken build. Sample over a
      // window and fail on the first pressed read.
      const deadline = Date.now() + 5000;
      while (Date.now() < deadline) {
        expect(await toggle.getAttribute('aria-pressed')).not.toBe('true');
        await sleep(250);
      }
    });

    test(`${label} opens in visual mode when visual editor is checked`, async ({ rstudioPage: page }) => {
      await createQuartoDoc(page, interactiveFormat, true);

      await expect(sourceActions.sourcePane.visualMdToggle)
        .toHaveAttribute('aria-pressed', 'true', { timeout: 20000 });
    });
  }
});
