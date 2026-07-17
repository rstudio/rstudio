// Loading Vim keybindings from a user vimrc file (#7350).
//
// When the vim_load_vimrc pref is enabled and Vim editor keybindings are in
// use, VimrcLoader.java fetches ~/.rstudio-vimrc (or ~/.vimrc) and replays
// its supported mapping commands through the Ace Vim emulation.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { SourcePane } from '@pages/source_pane.page';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import {
  CODE_TAB,
  CODE_EDITING_PANEL,
  openGlobalOptions,
  closeGlobalOptions,
} from '@pages/global_options.page';

// The vimrc is read from the session user's home directory, so create and
// restore it through the R session rather than the local filesystem -- this
// keeps the test working against both Desktop and Server. ~/.rstudio-vimrc
// takes priority over ~/.vimrc, so a developer's real ~/.vimrc is never read;
// any pre-existing ~/.rstudio-vimrc is backed up and restored.
//
// Besides the mappings under test, the fixture carries two canaries for the
// loader's command allowlist (its security boundary): 'call plug#begin()'
// must be ignored without breaking the load, and 'edit ~/vimrc-canary-e2e.R'
// -- an ex command RStudio itself registers, with an observable effect --
// must NOT execute (it would open the canary file in a source tab).
const CANARY_FILE = 'vimrc-canary-e2e.R';

const SETUP_VIMRC = [
  'if (file.exists("~/.rstudio-vimrc")) file.rename("~/.rstudio-vimrc", "~/.rstudio-vimrc.e2e-backup");',
  `writeLines("# canary", "~/${CANARY_FILE}");`,
  'writeLines(c("\\" comment", "imap jk <Esc>", "nnoremap <silent> Y y$",',
  `  "edit ~/${CANARY_FILE}", "call plug#begin()"), "~/.rstudio-vimrc")`,
].join(' ');

const SETUP_VIMRC_MIDSESSION = [
  'if (file.exists("~/.rstudio-vimrc")) file.rename("~/.rstudio-vimrc", "~/.rstudio-vimrc.e2e-backup");',
  'writeLines("imap zz <Esc>", "~/.rstudio-vimrc")',
].join(' ');

const RESTORE_VIMRC = [
  `unlink(c("~/.rstudio-vimrc", "~/${CANARY_FILE}"));`,
  'if (file.exists("~/.rstudio-vimrc.e2e-backup")) file.rename("~/.rstudio-vimrc.e2e-backup", "~/.rstudio-vimrc")',
].join(' ');

// True once a mapping with the given keys has been registered with the Vim
// emulation (user mappings land in the shared keymap).
function vimMappingRegistered(page: Page, keys: string): Promise<boolean> {
  return page.evaluate((mappedKeys) => {
    const w = window as unknown as {
      require(id: string): { handler: { defaultKeymap: Array<{ keys?: string }> } };
    };
    const handler = w.require('ace/keyboard/vim').handler;
    return handler.defaultKeymap.some((mapping) => mapping.keys === mappedKeys);
  }, keys);
}

test.describe('Vimrc keybindings', () => {
  test('mappings from ~/.rstudio-vimrc apply when the pref is enabled', async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);
    const sourcePane = new SourcePane(page);

    // run all console commands while keybindings are still the default, so
    // console input behavior isn't affected by Vim mode
    await consoleActions.executeInConsole(SETUP_VIMRC);

    try {
      await setPref(page, 'vim_load_vimrc', true);
      await setPref(page, 'editor_keybindings', 'vim');

      // open a new document; attaching the Vim keyboard handler triggers the
      // vimrc load
      await executeCommand(page, 'newSourceDoc');
      await expect
        .poll(() => vimMappingRegistered(page, 'jk'), {
          message: 'expected the vimrc mapping to be registered',
        })
        .toBe(true);

      // exercise the mappings: insert some text, leave insert mode via the
      // mapped 'jk', delete a character with normal-mode 'x', then use the
      // <silent>-stripped 'Y' (y$, yank to end of line: just 'b') and paste.
      // Vim's default linewise Y would instead yield 'ab\nab' here.
      const editor = new AceEditor(page, '');
      // in Vim normal mode Ace parks its hidden textarea offscreen, so click
      // the visible editor content to focus the editor
      await sourcePane.contentPane.click({ force: true });
      await page.keyboard.type('iabc', { delay: 20 });
      await page.keyboard.type('jk', { delay: 20 });
      await page.keyboard.type('x', { delay: 20 });
      await expect.poll(() => editor.getValue()).toBe('ab');

      await page.keyboard.type('Yp', { delay: 20 });
      await expect.poll(() => editor.getValue()).toBe('abb');

      // the blocked 'edit' canary must not have opened a tab
      await expect(
        page.locator('.gwt-TabLayoutPanelTab', { hasText: CANARY_FILE }),
      ).toHaveCount(0);
    } finally {
      // note: mappings already registered with the Vim emulation stay in
      // client memory, but are inert once Vim keybindings are disabled
      await clearPref(page, 'editor_keybindings');
      await clearPref(page, 'vim_load_vimrc');
      await consoleActions.closeAllBuffersWithoutSaving();
      await consoleActions.executeInConsole(RESTORE_VIMRC);
    }
  });

  test('enabling the pref mid-session loads the vimrc immediately', async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);
    const sourcePane = new SourcePane(page);

    await consoleActions.executeInConsole(SETUP_VIMRC_MIDSESSION);

    try {
      // Vim keybindings first, with the load pref still disabled
      await setPref(page, 'editor_keybindings', 'vim');
      await executeCommand(page, 'newSourceDoc');

      // focus the editor so the mid-session load path has a Vim editor to
      // apply against, then flip the pref: the mapping should appear without
      // opening another document
      // in Vim normal mode Ace parks its hidden textarea offscreen, so click
      // the visible editor content to focus the editor
      await sourcePane.contentPane.click({ force: true });
      await setPref(page, 'vim_load_vimrc', true);

      await expect
        .poll(() => vimMappingRegistered(page, 'zz'), {
          message: 'expected the vimrc mapping to be registered after enabling the pref',
        })
        .toBe(true);
    } finally {
      await clearPref(page, 'editor_keybindings');
      await clearPref(page, 'vim_load_vimrc');
      await consoleActions.closeAllBuffersWithoutSaving();
      await consoleActions.executeInConsole(RESTORE_VIMRC);
    }
  });

  test('the vimrc checkbox follows the keybindings selection', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    try {
      await page.locator(CODE_TAB).click();
      await expect(page.locator(CODE_EDITING_PANEL)).toBeVisible();

      const checkbox = page.getByLabel('Load Vim keybindings');
      const keybindings = page.locator(
        "xpath=//label[contains(text(),'Keybindings:')]/following::select[1]",
      );

      await keybindings.selectOption({ label: 'Default' });
      await expect(checkbox).toBeDisabled();

      await keybindings.selectOption({ label: 'Vim' });
      await expect(checkbox).toBeEnabled();
    } finally {
      // cancel so nothing selected here is applied
      await closeGlobalOptions(page);
    }
  });
});
