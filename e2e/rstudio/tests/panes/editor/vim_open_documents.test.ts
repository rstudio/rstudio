// Vim ex commands and mappings that open documents (#18948).
//
// These are JSNI callbacks in SourceVimCommands.java that call into
// SourceColumnManager. A stale method reference there still compiles, but
// throws once the command runs; the fixture fails the test on that recorded
// client exception, and the document assertions catch a command that does
// nothing.

import * as path from 'path';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { SourcePane } from '@pages/source_pane.page';
import { clearPref, documentOpen, executeCommand, setPref, waitForActiveDocument } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { seedSandboxFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';

type ActiveDocument = { id: string; path: string | null };

async function activeDocumentId(page: Page): Promise<string | null> {
  return page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
}

// Wait for a document other than `previousId` to become active with a live
// editor, and return it.
async function waitForNewActiveDocument(page: Page, previousId: string | null): Promise<ActiveDocument> {
  const handle = await page.waitForFunction(
    (prevId) => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (doc === null || doc.id === prevId || window.rstudio?.documents.activeEditor() == null)
        return null;
      return doc;
    },
    previousId,
    { timeout: TIMEOUTS.fileOpen, polling: 50 },
  );
  return (await handle.jsonValue()) as ActiveDocument;
}

async function openNewScript(page: Page): Promise<ActiveDocument> {
  const previousId = await activeDocumentId(page);
  await executeCommand(page, 'newSourceDoc');
  return waitForNewActiveDocument(page, previousId);
}

// In Vim normal mode Ace parks its hidden textarea offscreen, so click the
// visible editor content to focus it, then make sure we're in normal mode.
async function focusVimEditor(page: Page): Promise<void> {
  const contentPane = new SourcePane(page).contentPane;
  await expect(contentPane).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await contentPane.click({ force: true });
  await page.keyboard.press('Escape');
}

async function runExCommand(page: Page, command: string): Promise<void> {
  await focusVimEditor(page);
  await page.keyboard.type(`:${command}`, { delay: 20 });
  await page.keyboard.press('Enter');
}

test.describe('Vim commands that open documents', () => {
  const sandbox = useSuiteSandbox();

  test.beforeEach(async ({ rstudioPage: page }) => {
    await setPref(page, 'editor_keybindings', 'vim');
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await clearPref(page, 'editor_keybindings');
  });

  test(':e <file> opens the file relative to the working directory', async ({ rstudioPage: page }) => {
    const fileName = 'vim_edit_target.R';
    const fullPath = await seedSandboxFile(page, sandbox.dir, fileName, '# edit target\n');
    await openNewScript(page);

    // the sandbox is R's working directory; the document path must come back
    // resolved, not as the relative name that was typed
    await runExCommand(page, `e ${fileName}`);
    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
  });

  test(':e with no file opens a new R script', async ({ rstudioPage: page }) => {
    const untitled = await openNewScript(page);

    await runExCommand(page, 'e');
    const created = await waitForNewActiveDocument(page, untitled.id);
    expect(created.path).toBeNull();
  });

  test(']f and [f open the adjacent files in the directory', async ({ rstudioPage: page }) => {
    // a directory of their own, so the pair is the whole listing and the
    // result doesn't depend on its sort order
    const first = await seedSandboxFile(page, sandbox.dir, path.join('adjacent', 'vim_first.R'), '# first\n');
    const second = await seedSandboxFile(page, sandbox.dir, path.join('adjacent', 'vim_second.R'), '# second\n');
    await documentOpen(page, first);

    await focusVimEditor(page);
    await page.keyboard.type(']f', { delay: 20 });
    await waitForActiveDocument(page, second, TIMEOUTS.fileOpen);

    await focusVimEditor(page);
    await page.keyboard.type('[f', { delay: 20 });
    await waitForActiveDocument(page, first, TIMEOUTS.fileOpen);
  });
});
