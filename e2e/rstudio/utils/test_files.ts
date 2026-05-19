import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { typeInConsole } from '../pages/console_pane.page';
import { SourcePane } from '../pages/source_pane.page';
import { TIMEOUTS } from './constants';

/** Normalize a path for use inside an R string literal: forward slashes, no escaping issues. */
function rPath(p: string): string {
  return p.replace(/\\/g, '/');
}

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
  const rFullPath = rPath(fullPath);
  if (fs.existsSync(sandboxDir)) {
    fs.writeFileSync(fullPath, content);
  } else {
    const escaped = content
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\n/g, '\\n');
    await typeInConsole(page, `writeLines("${escaped}", "${rFullPath}")`);
  }
  await typeInConsole(page, `file.edit('${rFullPath}')`);
  // The source tab shows the basename, not the full path.
  const tab = page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']");
  await expect(tab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
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
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
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
    const vec = fileNames.map((f) => `"${rPath(path.join(sandboxDir, f))}"`).join(', ');
    await typeInConsole(page, `unlink(c(${vec}))`);
  }
}
