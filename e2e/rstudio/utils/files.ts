import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { executeInConsole } from '../pages/console_pane.page';
import { TIMEOUTS } from './constants';
import { rPathLiteral, rStringLiteral } from './r';
import { resetSourcePaneState } from './commands';
import { assertAbsolutePath } from './paths';

/**
 * True when the current process can create/modify entries in `dir`. Detects
 * three distinct cases where the Node-side fast path is unsafe:
 *   - `dir` doesn't exist (ENOENT)
 *   - `dir` lives on a remote rsession host (also ENOENT from here)
 *   - `dir` exists locally but was created by a different uid (Server-on-Linux:
 *     test runner is `runner`, rsession is `pw_*` -- fs.existsSync returns
 *     true via traversal, but writes EACCES)
 * Callers fall back to R-side operations (executed by rsession, same uid as
 * the workdir owner) when this returns false.
 */
function canWriteDir(dir: string): boolean {
  try {
    fs.accessSync(dir, fs.constants.W_OK);
    return true;
  } catch {
    return false;
  }
}

/**
 * Write `content` to `fileName` in the per-spec sandbox workdir and open it
 * via `file.edit()`, bypassing the R-side `writeLines` escape gauntlet.
 *
 * The Node-side write only works when the workdir is writable by the test
 * process. When it isn't -- the workdir lives on a remote rsession host, or
 * lives locally but is owned by a different uid (Server-on-Linux) -- falls
 * back to R-side writeLines with escaped content. Callers that hit the
 * fallback still have to deal with backslash doubling for content that
 * contains backslashes.
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
  assertAbsolutePath(sandboxDir, `writeAndOpenFile(fileName=${JSON.stringify(fileName)}): sandboxDir`);
  const fullPath = path.join(sandboxDir, fileName);
  if (canWriteDir(sandboxDir)) {
    fs.writeFileSync(fullPath, content);
  } else {
    // sep="" + useBytes=TRUE keeps writeLines byte-identical to Node's
    // fs.writeFileSync. Default sep="\n" would append an extra trailing
    // newline that the original Node write doesn't, breaking content-equality
    // assertions (toBe / toContain) in tests that round-trip the file.
    await executeInConsole(page, `writeLines(${rStringLiteral(content)}, ${rPathLiteral(fullPath)}, sep="", useBytes=TRUE)`);
  }
  await openFile(page, fullPath);
}

/**
 * Create `relativePath` (with its parent directories) under `sandboxDir` and
 * write `content` to it. Same write-or-fallback heuristic as
 * `writeAndOpenFile`, but for cases where the test needs to seed a file
 * deeper than the sandbox root (e.g. `tests/testthat/test-foo.R` inside a
 * project skeleton). Does NOT open the file in the IDE -- callers open it
 * separately via `documentOpen` / `openFile` once the test setup is ready.
 */
export async function seedSandboxFile(
  page: Page,
  sandboxDir: string,
  relativePath: string,
  content: string,
): Promise<string> {
  assertAbsolutePath(sandboxDir, `seedSandboxFile(relativePath=${JSON.stringify(relativePath)}): sandboxDir`);
  const fullPath = path.join(sandboxDir, relativePath);
  const parentDir = path.dirname(fullPath);
  if (canWriteDir(sandboxDir)) {
    fs.mkdirSync(parentDir, { recursive: true });
    fs.writeFileSync(fullPath, content);
  } else {
    // R-side mkdir + write executes as rsession's uid -- same owner as the
    // sandbox -- so it sidesteps the cross-uid EACCES.
    await executeInConsole(
      page,
      `dir.create(${rPathLiteral(parentDir)}, recursive = TRUE, showWarnings = FALSE)`,
    );
    // sep="" + useBytes=TRUE for byte-identical output -- see writeAndOpenFile
    // above for the full rationale.
    await executeInConsole(page, `writeLines(${rStringLiteral(content)}, ${rPathLiteral(fullPath)}, sep="", useBytes=TRUE)`);
  }
  return fullPath;
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
 * `pathOrName` may be a basename, a relative path, or an absolute path.
 * When absolute, the active-doc post-condition compares full paths
 * (case-insensitive, slashes normalized) so two open files that share a
 * basename can't be confused for one another; a doc.path RStudio reports
 * home-aliased ("~/sub/file.R" -- anything under the rsession's home,
 * e.g. a sandbox under C:\Users\<user>\AppData\...\Temp on Windows) is
 * matched by its home-relative suffix. When relative or a bare
 * basename, falls back to basename match -- R resolves the path against
 * its own cwd, which can differ from Node's, so the absolute form
 * RStudio reports back is not known to the helper.
 */
export async function openFile(
  page: Page,
  pathOrName: string,
): Promise<void> {
  await executeInConsole(page, `file.edit(${rPathLiteral(pathOrName)})`);

  // rPathLiteral normalizes backslashes to forward slashes for R; mirror
  // that on the comparison side so platform-specific separators don't
  // cause spurious mismatches.
  const requestedSlashes = pathOrName.replace(/\\/g, '/');
  const slash = requestedSlashes.lastIndexOf('/');
  const basename = slash >= 0 ? requestedSlashes.slice(slash + 1) : requestedSlashes;
  const expectedFullPath = path.isAbsolute(pathOrName) ? requestedSlashes : null;

  const tab = page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']");
  await expect(tab).toContainText(basename, { timeout: TIMEOUTS.fileOpen });

  await page.waitForFunction(
    (args: { expectedFullPath: string | null; basename: string }) => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (!doc || !doc.path) return false;
      const docPath = doc.path.replace(/\\/g, '/');
      if (args.expectedFullPath !== null) {
        // Full-path match: case-insensitive to tolerate macOS HFS+ /
        // Windows NTFS, where the filesystem itself folds case. RStudio
        // home-aliases paths under the rsession's home directory
        // ("~/sub/file.R"), so a home-aliased doc.path matches when the
        // expected absolute path ends with the aliased path minus the "~".
        // That is still a full home-relative path comparison, so two open
        // files sharing a basename can't be confused.
        const dp = docPath.toLowerCase();
        const expected = args.expectedFullPath.toLowerCase();
        const matches = dp === expected
          || (dp.startsWith('~/') && expected.endsWith(dp.slice(1)));
        if (!matches) return false;
      } else {
        const docSlash = docPath.lastIndexOf('/');
        const docBase = docSlash >= 0 ? docPath.slice(docSlash + 1) : docPath;
        if (docBase.toLowerCase() !== args.basename.toLowerCase()) return false;
      }
      // Bridge confirms the active doc has a live Ace editor instance --
      // the same handle the GWT side already tracks as "the active doc."
      // Returns null when there's no editor or it's not Ace-backed (data
      // viewer, object explorer, ...) so we don't return prematurely.
      return window.rstudio?.documents.activeEditor() != null;
    },
    { expectedFullPath, basename },
    { timeout: TIMEOUTS.fileOpen, polling: 50 },
  );
}

/**
 * Best-effort remove a single file by absolute path. Mirrors the write
 * fallback in seedSandboxFile / writeAndOpenFile: when the parent directory
 * isn't writable from the test process (Server-on-Linux: file owned by
 * rsession's uid), unlink via R so the rsession-side uid does the delete.
 * Errors are swallowed -- callers treat removal as cleanup, not a
 * verification point.
 */
export async function removeSandboxFile(page: Page, fullPath: string): Promise<void> {
  assertAbsolutePath(fullPath, 'removeSandboxFile: fullPath');
  if (canWriteDir(path.dirname(fullPath))) {
    try {
      fs.rmSync(fullPath, { force: true });
    } catch {
      // best effort
    }
  } else {
    await executeInConsole(page, `unlink(${rPathLiteral(fullPath)})`);
  }
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
  assertAbsolutePath(sandboxDir, 'closeAndDeleteSandboxFiles: sandboxDir');
  // Leave a single Untitled placeholder tab rather than draining the source
  // pane to zero tabs. Going through zero triggers the source pane's HIDE
  // animation (#17738) and lets RStudio auto-spawn a fresh Untitled1 in the
  // gap -- which then collides with the next test's file (e.g. two
  // publishBtns visible at the same time, breaking strict-mode locators).
  // resetSourcePaneState waits for its async close chain to drain before
  // returning, so the editors have detached by the time we delete the files
  // below. Deleting while a tab still holds a file open makes RStudio raise a
  // "File Deleted" prompt and a "Save File" (system error 2) error whose glass
  // panels then block the next test's first action.
  await resetSourcePaneState(page);
  if (canWriteDir(sandboxDir)) {
    for (const fileName of fileNames) {
      try {
        fs.rmSync(path.join(sandboxDir, fileName), { force: true });
      } catch {
        // best effort
      }
    }
  } else {
    // Workdir not writable from here (remote rsession host, or local but
    // cross-uid as on Server-on-Linux) -- fall back to R-side unlink.
    const vec = fileNames.map((f) => rPathLiteral(path.join(sandboxDir, f))).join(', ');
    await executeInConsole(page, `unlink(c(${vec}))`);
  }
}
