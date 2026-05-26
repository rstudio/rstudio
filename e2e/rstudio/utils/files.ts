import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { executeInConsole } from '../pages/console_pane.page';
import { SourcePane } from '../pages/source_pane.page';
import { TIMEOUTS } from './constants';
import { rPathLiteral, rStringLiteral } from './r';
import { documentCloseAllNoSave } from './commands';

/**
 * Write `content` to `fileName` in the per-spec sandbox workdir and open it
 * via `file.edit()`, bypassing the R-side `writeLines` escape gauntlet.
 *
 * The Node-side write only works when the workdir is local to the runner
 * (Desktop, or Server pointed at the same machine). When the workdir lives
 * on a remote rsession host, falls back to R-side writeLines with escaped
 * content -- callers that hit this path still have to deal with backslash
 * doubling for content that contains backslashes.
 *
 * All R-side operations use the absolute sandbox path, so they work even if
 * R's working directory has drifted away from the suite sandbox (e.g. a
 * test opened a project).
 */
export async function writeAndOpenFile(
  page: Page,
  sandboxDir: string,
  fileName: string,
  content: string,
): Promise<void> {
  const fullPath = path.join(sandboxDir, fileName);
  if (fs.existsSync(sandboxDir)) {
    fs.writeFileSync(fullPath, content);
  } else {
    await executeInConsole(page, `writeLines(${rStringLiteral(content)}, ${rPathLiteral(fullPath)})`);
  }
  await openFile(page, fullPath);
}

/**
 * Open a file via `file.edit()` and wait until the source editor is fully
 * hydrated and ready to drive: the tab is selected, the bridge reports
 * the file as the active document, and a source-pane Ace instance is
 * reachable. The file must already exist on disk -- `file.edit` resolves
 * existing paths only.
 *
 * Use this instead of bare `file.edit(...)` + `selectedTab` wait. The tab
 * can be selected before Ace finishes loading the document body; without
 * the bridge + Ace polls the gap surfaces later as a confusing
 * `Expected: not ""` timeout from a downstream getEditorContent /
 * focusEditor call rather than as "file never finished loading."
 *
 * `pathOrName` may be a basename, a relative path, or an absolute path --
 * `file.edit` resolves relative paths against R's cwd, and the active-doc
 * post-condition compares basenames so the helper does not need to know
 * what RStudio will normalize the path to.
 */
export async function openFile(
  page: Page,
  pathOrName: string,
): Promise<void> {
  await executeInConsole(page, `file.edit(${rPathLiteral(pathOrName)})`);

  // Compute the basename for both the tab and the bridge check. file.edit
  // resolves relative paths against R's cwd, which can differ from Node's,
  // so we don't know the exact absolute path the bridge will report -- but
  // the basename is invariant.
  const slash = Math.max(pathOrName.lastIndexOf('/'), pathOrName.lastIndexOf('\\'));
  const basename = slash >= 0 ? pathOrName.slice(slash + 1) : pathOrName;

  const tab = page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']");
  await expect(tab).toContainText(basename, { timeout: TIMEOUTS.fileOpen });

  await page.waitForFunction(
    (base: string) => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (!doc || !doc.path) return false;
      const docSlash = Math.max(doc.path.lastIndexOf('/'), doc.path.lastIndexOf('\\'));
      const docBase = docSlash >= 0 ? doc.path.slice(docSlash + 1) : doc.path;
      if (docBase.toLowerCase() !== base.toLowerCase()) return false;
      // Bridge confirms the active doc has a live Ace editor instance --
      // the same handle the GWT side already tracks as "the active doc."
      // Returns null when there's no editor or it's not Ace-backed (data
      // viewer, object explorer, ...) so we don't return prematurely.
      return window.rstudio?.documents.activeEditor() != null;
    },
    basename,
    { timeout: TIMEOUTS.fileOpen, polling: 50 },
  );
}

/**
 * Close all open source documents without prompting to save, then delete the
 * named files from the sandbox via Node `fs`. Avoids the four-step
 * `closeSourceAndDeleteFile` per file (saveAll + closeAll + assert empty +
 * unlink) and its built-in sleeps -- only a single console roundtrip and
 * a single polling wait for the editors to detach.
 *
 * R-side `unlink` (used in the remote-workdir fallback) is given the
 * absolute paths so it doesn't depend on R's current working directory.
 */
export async function closeAndDeleteSandboxFiles(
  page: Page,
  sandboxDir: string,
  fileNames: string[],
): Promise<void> {
  await documentCloseAllNoSave(page);
  const sourcePane = new SourcePane(page);
  await expect(sourcePane.aceTextInput).toHaveCount(0, { timeout: 5000 }).catch(() => {});
  if (fs.existsSync(sandboxDir)) {
    for (const fileName of fileNames) {
      try {
        fs.rmSync(path.join(sandboxDir, fileName), { force: true });
      } catch {
        // best effort
      }
    }
  } else {
    // Remote workdir -- fall back to R-side unlink with absolute paths.
    const vec = fileNames.map((f) => rPathLiteral(path.join(sandboxDir, f))).join(', ');
    await executeInConsole(page, `unlink(c(${vec}))`);
  }
}
