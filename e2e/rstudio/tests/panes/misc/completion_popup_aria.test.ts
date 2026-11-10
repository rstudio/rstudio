// Screen reader support for the console/editor completion popup (#6167) and
// the console history popup (#6168). Keyboard focus stays in the Ace text
// input while either popup is open, so the input's aria-activedescendant is
// what tells a screen reader which item is selected.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import {
  COMPLETION_POPUP,
  setAlwaysShowCompletionPopup,
  waitForCompletionPopup,
} from '@actions/autocomplete.actions';
import { CONSOLE_INPUT } from '@pages/console_pane.page';
import { setPref, clearPref } from '@utils/commands';
import { useSuiteSandbox } from '@utils/sandbox';

interface ActiveOption {
  id: string;
  role: string | null;
  selected: string | null;
  text: string;
  listRole: string | null;
  listLabel: string | null;
  selectedCount: number;
}

/**
 * Resolve the focused element's aria-activedescendant to the option it names,
 * or null when the attribute is absent or names no element.
 */
async function activeOption(page: Page): Promise<ActiveOption | null> {
  return await page.evaluate(() => {
    const input = document.activeElement;
    const id = input?.getAttribute('aria-activedescendant');
    if (!id)
      return null;
    const option = document.getElementById(id);
    if (!option)
      return null;
    const list = option.closest('[role="listbox"], [role="menu"]');
    return {
      id,
      role: option.getAttribute('role'),
      selected: option.getAttribute('aria-selected') ?? option.getAttribute('aria-current'),
      text: (option.textContent ?? '').trim(),
      listRole: list?.getAttribute('role') ?? null,
      listLabel: list?.getAttribute('aria-label') ?? null,
      selectedCount: list
        ? list.querySelectorAll('[aria-selected="true"], [aria-current="true"]').length
        : 0,
    };
  });
}

async function expectActiveOption(page: Page, listLabel: string): Promise<ActiveOption> {
  await expect.poll(() => activeOption(page)).not.toBeNull();
  const option = (await activeOption(page))!;
  expect(option.role).toMatch(/^(option|menuitem)$/);
  expect(option.selected).toBe('true');
  expect(option.listRole).toMatch(/^(listbox|menu)$/);
  expect(option.listLabel).toBe(listLabel);
  expect(option.selectedCount).toBe(1);
  return option;
}

/** Arrow Down must move aria-activedescendant to a different selected option. */
async function expectArrowDownMovesOption(page: Page, listLabel: string, before: ActiveOption) {
  await page.keyboard.press('ArrowDown');
  await expect.poll(async () => (await activeOption(page))?.id).not.toBe(before.id);
  const after = await expectActiveOption(page, listLabel);
  expect(after.text).not.toBe(before.text);
}

/**
 * Record, at each aria-activedescendant write on the focused element, whether
 * the named element is visible. Screen readers (VoiceOver in Safari) skip a
 * hidden target and don't re-announce once it becomes visible, and a
 * MutationObserver would only run after the popup is made visible again, so
 * wrap setAttribute. Writes on unfocused elements, such as the one GWT's
 * MenuBar keeps on itself, are not announced and so are not recorded.
 */
async function trackActiveDescendantWrites(page: Page) {
  await page.evaluate(() => {
    const w = window as unknown as { __adWrites: { id: string; visible: boolean }[] };
    w.__adWrites = [];
    const proto = Element.prototype as Element & { __adOriginal?: Element['setAttribute'] };
    proto.__adOriginal ??= proto.setAttribute;
    const original = proto.__adOriginal;
    proto.setAttribute = function (name: string, value: string) {
      original.call(this, name, value);
      if (name === 'aria-activedescendant' && this === document.activeElement) {
        const target = document.getElementById(value);
        w.__adWrites.push({
          id: value,
          visible: !!target && target.checkVisibility({ visibilityProperty: true }),
        });
      }
    };
  });
}

async function untrackActiveDescendantWrites(page: Page) {
  await page.evaluate(() => {
    const proto = Element.prototype as Element & { __adOriginal?: Element['setAttribute'] };
    if (proto.__adOriginal)
      proto.setAttribute = proto.__adOriginal;
  });
}

async function expectWritesNameVisibleItems(page: Page) {
  const writes = await page.evaluate(
    () => (window as unknown as { __adWrites: { id: string; visible: boolean }[] }).__adWrites);
  expect(writes.length).toBeGreaterThan(0);
  expect(writes.filter((w) => !w.visible)).toEqual([]);
}

async function expectNoActiveDescendant(page: Page, inputSelector: string) {
  await expect(page.locator(inputSelector)).not.toHaveAttribute('aria-activedescendant');
}

test.describe('Completion popup screen reader support', () => {
  // The editor test creates a file in the working directory.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await untrackActiveDescendantWrites(page);
    await setAlwaysShowCompletionPopup(page, false);
    await clearPref(page, 'code_completion');
    await page.keyboard.press('Escape');
    await page.keyboard.press('Escape');
    await consoleActions.resetSourcePane();
  });

  test('console completion popup announces the selected item', async ({ rstudioPage: page }) => {
    await setAlwaysShowCompletionPopup(page, true);
    await trackActiveDescendantWrites(page);
    await consoleActions.typeInConsole('Sys.get');
    await waitForCompletionPopup(page);
    await expect(page.locator(COMPLETION_POPUP)).toBeVisible();

    const first = await expectActiveOption(page, 'Completions');
    expect(first.text).toContain('Sys.get');
    await expectWritesNameVisibleItems(page);
    await expectArrowDownMovesOption(page, 'Completions', first);

    await page.keyboard.press('Escape');
    await expect(page.locator(COMPLETION_POPUP)).toBeHidden();
    await expectNoActiveDescendant(page, CONSOLE_INPUT);
  });

  test('editor completion popup announces the selected item', async ({ rstudioPage: page }) => {
    const fileName = `completion_aria_${Date.now()}.R`;
    await sourceActions.createAndOpenFile(fileName, '');
    await sourceActions.sourcePane.aceTextInput.click({ force: true });

    await setAlwaysShowCompletionPopup(page, true);
    await trackActiveDescendantWrites(page);
    await page.keyboard.type('Sys.get');
    await waitForCompletionPopup(page);
    await expect(page.locator(COMPLETION_POPUP)).toBeVisible();

    const first = await expectActiveOption(page, 'Completions');
    expect(first.text).toContain('Sys.get');
    await expectWritesNameVisibleItems(page);
    await expectArrowDownMovesOption(page, 'Completions', first);

    await page.keyboard.press('Escape');
    await expect(page.locator(COMPLETION_POPUP)).toBeHidden();
    await expect.poll(() => activeOption(page)).toBeNull();
  });

  test('C++ completion popup announces the selected item', async ({ rstudioPage: page }) => {
    // A standalone .cpp file gets compile arguments, and so completions,
    // only as an Rcpp (or cpp11) source file.
    const canComplete = await consoleActions.evalRLogical(
      '.rs.isClangAvailable() && requireNamespace("Rcpp", quietly = TRUE)');
    test.skip(canComplete !== true, 'C++ completion needs libclang and the Rcpp package');

    // C++ completion asks libclang for the members of `s` once `s.` is typed.
    // The file must exist on disk, since completion compiles the saved file.
    const fileName = `completion_aria_${Date.now()}.cpp`;
    await sourceActions.createAndOpenFile(fileName, [
      '#include <Rcpp.h>',
      '#include <string>',
      '// [[Rcpp::export]]',
      'int probe() {',
      '  std::string s;',
      '  ',
      '  return 0;',
      '}',
    ].join('\n'));
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor() ?? null;
      if (!editor) throw new Error('No active source editor');
      editor.focus();
      editor.gotoLine(6, 2);
    });

    await trackActiveDescendantWrites(page);
    await page.keyboard.type('s.');

    // libclang compiles the translation unit on the first request, which can
    // take several seconds.
    // Located by structure, not ARIA, so a missing label fails on the label.
    const popup = page.locator('.themedPopupPanel:visible .gwt-MenuBar-vertical');
    await expect(popup).toBeVisible({ timeout: 60000 });

    const first = await expectActiveOption(page, 'Completions');
    expect(first.text.length).toBeGreaterThan(0);
    await expectWritesNameVisibleItems(page);
    await expectArrowDownMovesOption(page, 'Completions', first);

    // The type icons are decorative; the item's text names it.
    await expect(popup.locator('img:not([role="presentation"])')).toHaveCount(0);

    await page.keyboard.press('Escape');
    await expect(popup).toBeHidden();
    await expect.poll(() => activeOption(page)).toBeNull();
  });

  test('console history popup announces the selected item', async ({ rstudioPage: page }) => {
    // Typing the prefix would otherwise raise the completion popup too.
    await setPref(page, 'code_completion', 'manual');

    // Two distinct commands sharing a prefix, so the popup lists both.
    await consoleActions.executeInConsole('historyAriaProbe <- 1', { wait: true });
    await consoleActions.executeInConsole('historyAriaProbe <- 2', { wait: true });

    await consoleActions.typeInConsole('historyAriaProbe');
    await trackActiveDescendantWrites(page);
    await page.keyboard.press('Control+ArrowUp');

    // The history popup selects the most recent entry, which is the last row.
    const last = await expectActiveOption(page, 'Console history');
    expect(last.text).toBe('historyAriaProbe <- 2');
    await expectWritesNameVisibleItems(page);

    await page.keyboard.press('ArrowUp');
    await expect.poll(async () => (await activeOption(page))?.text).toBe('historyAriaProbe <- 1');
    await expectActiveOption(page, 'Console history');

    await page.keyboard.press('Escape');
    await expectNoActiveDescendant(page, CONSOLE_INPUT);
  });
});
