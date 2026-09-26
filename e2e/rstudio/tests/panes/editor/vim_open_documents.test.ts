// Vim ex commands and mappings that open documents (#18948).
//
// These are JSNI callbacks in SourceVimCommands.java that call into
// SourceColumnManager. A stale method reference there still compiles, but
// throws once the command runs; the fixture fails the test on that recorded
// client exception, and the document assertions catch a command that does
// nothing.

import * as fs from 'fs';
import * as path from 'path';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { CONFIRM_BTN } from '@pages/modals.page';
import { SourcePane } from '@pages/source_pane.page';
import { clearPref, documentOpen, executeCommand, setPref, waitForActiveDocument } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { seedSandboxFile } from '@utils/files';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

type ActiveDocument = { id: string; path: string | null };

const ERROR_DIALOG = 'div.gwt-DialogBox[aria-label="Error while opening file"]';

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

// The vim module loads lazily, and RStudio registers its ex commands and
// mappings only once it has. Wait until the active editor uses it and the
// last of those registrations ([f) is in place; typing before then would
// insert the command into the document as text.
async function waitForVim(page: Page): Promise<void> {
  await page.waitForFunction(
    () => {
      type VimHandler = { defaultKeymap: Array<{ keys?: string }> };
      const w = window as unknown as {
        require(id: string): { handler: VimHandler } | undefined;
      };
      const vim = w.require('ace/keyboard/vim');
      if (!vim?.handler.defaultKeymap.some((mapping) => mapping.keys === '[f'))
        return false;

      // RStudio stacks other keyboard handlers on top of vim's, so look for
      // it among the editor's handlers (the typings don't declare keyBinding)
      const editor = window.rstudio?.documents.activeEditor() as unknown as {
        keyBinding: { $handlers: unknown[] };
      } | null;
      return editor?.keyBinding.$handlers.includes(vim.handler) ?? false;
    },
    undefined,
    { timeout: TIMEOUTS.fileOpen, polling: 50 },
  );
}

// In Vim normal mode Ace parks its hidden textarea offscreen, so click the
// visible editor content to focus it, then make sure we're in normal mode.
async function focusVimEditor(page: Page): Promise<void> {
  await waitForVim(page);
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

async function typeVimKeys(page: Page, keys: string): Promise<void> {
  await focusVimEditor(page);
  await page.keyboard.type(keys, { delay: 20 });
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

  test(':e creates a missing file, and takes escaped spaces and e!', async ({ rstudioPage: page }) => {
    const existing = await seedSandboxFile(page, sandbox.dir, 'vim_bang_target.R', '# bang target\n');
    await openNewScript(page);

    // Vim escapes a space in a file name with a backslash
    const created = path.join(sandbox.dir, 'vim new file.R');
    await runExCommand(page, 'e vim\\ new\\ file.R');
    await waitForActiveDocument(page, created, TIMEOUTS.fileOpen);
    expect(fs.existsSync(created)).toBe(true);

    // with a file name, e! opens that file (rather than reverting)
    await runExCommand(page, 'e! vim_bang_target.R');
    await waitForActiveDocument(page, existing, TIMEOUTS.fileOpen);
  });

  test(':e opens a symlink under its own path', async ({ rstudioPage: page }) => {
    test.skip(process.platform === 'win32', 'creating symlinks needs extra privileges on Windows');

    // opened under the target's path instead, a file that's also open from
    // the Files pane would get a second, independent editor
    const target = await seedSandboxFile(
      page,
      sandbox.dir,
      path.join('vim_link_target', 'target.R'),
      '# target\n',
    );
    // link from R: on Linux Server the sandbox belongs to the rsession user,
    // so a symlink created by the test runner fails with EACCES
    const link = path.join(sandbox.dir, 'vim_link.R');
    await executeInConsole(page, `stopifnot(file.symlink(${rPathLiteral(target)}, ${rPathLiteral(link)}))`);
    await openNewScript(page);

    await runExCommand(page, 'e vim_link.R');
    await waitForActiveDocument(page, link, TIMEOUTS.fileOpen);
  });

  test(':e reports a file it cannot open instead of doing nothing', async ({ rstudioPage: page }) => {
    await seedSandboxFile(page, sandbox.dir, path.join('vim_folder', 'keep.R'), '');
    const untitled = await openNewScript(page);

    const cases = [
      // Vim's current and alternate file names, which RStudio doesn't expand
      { command: 'e#', message: '(alternate file)' },
      { command: 'e %', message: '(alternate file)' },
      { command: 'e vim_missing_folder/new.R', message: 'could not be created' },
      { command: 'e vim_folder', message: 'is a folder' },
    ];

    for (const { command, message } of cases) {
      await test.step(`:${command}`, async () => {
        await runExCommand(page, command);
        const dialog = page.locator(ERROR_DIALOG);
        await expect(dialog).toContainText(message, { timeout: TIMEOUTS.fileOpen });
        await page.locator(CONFIRM_BTN).click();
        await expect(dialog).toBeHidden();
      });
    }

    expect(await activeDocumentId(page)).toBe(untitled.id);
    for (const name of ['#', '%', 'vim_missing_folder'])
      expect(fs.existsSync(path.join(sandbox.dir, name))).toBe(false);
  });

  test(':e with no file opens a new R script', async ({ rstudioPage: page }) => {
    const untitled = await openNewScript(page);

    await runExCommand(page, 'e');
    const created = await waitForNewActiveDocument(page, untitled.id);
    expect(created.path).toBeNull();
  });

  test(']f and [f open the adjacent files in the directory', async ({ rstudioPage: page }) => {
    // a directory of their own, so these are the whole listing. The listing
    // isn't sorted, so rather than expect particular files: ]f from the first
    // file reaches some other file, [f must come back, and [f again must reach
    // the third file. That fails if either key moves the wrong way relative
    // to the other.
    const files: Record<string, string> = {};
    for (const name of ['vim_a.R', 'vim_b.R', 'vim_c.R']) {
      files[name] = await seedSandboxFile(
        page,
        sandbox.dir,
        path.join('adjacent', name),
        `# ${name}\n`,
      );
    }

    const start = files['vim_a.R'];
    await documentOpen(page, start);
    await waitForActiveDocument(page, start, TIMEOUTS.fileOpen);
    const startId = await activeDocumentId(page);

    await typeVimKeys(page, ']f');
    const next = await waitForNewActiveDocument(page, startId);
    const nextName = path.basename(next.path ?? '');
    expect(['vim_b.R', 'vim_c.R']).toContain(nextName);

    await typeVimKeys(page, '[f');
    await waitForActiveDocument(page, start, TIMEOUTS.fileOpen);

    const thirdName = nextName === 'vim_b.R' ? 'vim_c.R' : 'vim_b.R';
    await typeVimKeys(page, '[f');
    await waitForActiveDocument(page, files[thirdName], TIMEOUTS.fileOpen);
  });
});
