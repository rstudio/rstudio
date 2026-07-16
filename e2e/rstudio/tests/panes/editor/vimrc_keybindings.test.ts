// Loading Vim keybindings from a user vimrc file (#7350).
//
// When the vim_load_vimrc pref is enabled and Vim editor keybindings are in
// use, VimrcLoader.java fetches ~/.rstudio-vimrc (or ~/.vimrc) and replays
// its supported mapping commands through the Ace Vim emulation.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
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
const SETUP_VIMRC = [
  'if (file.exists("~/.rstudio-vimrc")) file.rename("~/.rstudio-vimrc", "~/.rstudio-vimrc.e2e-backup");',
  'writeLines(c("\\" comment", "imap jk <Esc>", "nnoremap <silent> Y y$", "call plug#begin()"), "~/.rstudio-vimrc")',
].join(' ');

const RESTORE_VIMRC = [
  'unlink("~/.rstudio-vimrc");',
  'if (file.exists("~/.rstudio-vimrc.e2e-backup")) file.rename("~/.rstudio-vimrc.e2e-backup", "~/.rstudio-vimrc")',
].join(' ');

// True once the 'imap jk <Esc>' mapping from the vimrc has been registered
// with the Vim emulation (user mappings land in the shared keymap).
function vimrcMappingRegistered(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    const w = window as unknown as {
      require(id: string): { handler: { defaultKeymap: Array<{ keys?: string }> } };
    };
    const handler = w.require('ace/keyboard/vim').handler;
    return handler.defaultKeymap.some((mapping) => mapping.keys === 'jk');
  });
}

test.describe('Vimrc keybindings', () => {
  test('mappings from ~/.rstudio-vimrc apply when the pref is enabled', async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);

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
        .poll(() => vimrcMappingRegistered(page), {
          message: 'expected the vimrc mapping to be registered',
        })
        .toBe(true);

      // exercise the mapping: insert some text, leave insert mode via the
      // mapped 'jk', then delete a character with normal-mode 'x'. The
      // unsupported 'call plug#begin()' vimrc line must not break loading.
      const editor = new AceEditor(page, '');
      await editor.focus();
      await page.keyboard.type('iabc', { delay: 20 });
      await page.keyboard.type('jk', { delay: 20 });
      await page.keyboard.type('x', { delay: 20 });

      await expect.poll(() => editor.getValue()).toBe('ab');
    } finally {
      // note: mappings already registered with the Vim emulation stay in
      // client memory, but are inert once Vim keybindings are disabled
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
