import { chromium } from 'playwright';
import type { Browser, Page } from 'playwright';
import * as fs from 'fs';
import { ConsolePaneActions } from '../actions/console_pane.actions';
import { executeInConsole } from '../pages/console_pane.page';
import { externalServerUrl, signInToServer } from '../fixtures/server.fixture';
import { AUTH_STORAGE_KEY } from './auth';
import { rStringLiteral } from './r';

/**
 * Through-the-session credential provisioning for external RStudio Servers
 * (PW_RSTUDIO_SERVER_URL), part 2 of #18348.
 *
 * A spawned in-tree rserver delivers the sandbox credentials to its rsessions
 * via the --rsession-path wrapper (fixtures/server.fixture.ts). An external
 * server is outside the harness's reach: its rsessions read the logged-in
 * account's real home directory, whose environment the harness cannot set.
 * What the harness does hold, once logged in, is a live R console running as
 * that account -- so the sandbox credential stores are pushed through it into
 * the remote home. The writers below execute R code with the rsession's uid,
 * the same trick utils/files.ts uses for cross-uid sandbox writes.
 *
 * Everything here is driven from the auth.setup project (provisioning) and
 * the auth-teardown project (scrubbing); see tests/auth.setup.ts and
 * tests/auth.teardown.ts. Both projects run with Playwright artifacts off,
 * because the console commands carry real token material -- a trace or video
 * would capture it into the report.
 *
 * Known limitation, documented in README.md: commands submitted through the
 * console land in the remote account's RStudio console history, so the token
 * bytes also end up in the history database. Provisioning records whether the
 * history files existed beforehand; ones it effectively created are scrubbed
 * with the credential stores, but a pre-existing history database is never
 * deleted (it holds the account's own history), so on such an account token
 * material can persist there -- use a dedicated test account.
 */

/** A logged-in browser session against the external server. */
export interface ExternalSession {
  browser: Browser;
  page: Page;
}

/**
 * Remote (server-side) paths, all expressed with a "~/" prefix so R expands
 * them against the rsession's own home. Node's path helpers are never applied
 * to these -- the remote host's layout is not this machine's.
 */
export const REMOTE_POSITAI_STORE = '~/.posit/ai/auth/data.json';
export const REMOTE_COPILOT_DIR = '~/.config/github-copilot';
// RStudio appends every submitted console command to these; see the module
// comment for how provisioning handles them.
export const REMOTE_HISTORY_FILES = [
  '~/.local/share/rstudio/history_database',
  '~/.Rhistory',
] as const;

/**
 * The provisioning record the setup step writes into the sandbox
 * (<sandbox>/remote-provision-manifest.json) for the teardown to scrub from.
 * Only paths this run created are listed -- the teardown must never delete
 * credentials it didn't write.
 */
export interface RemoteProvisionManifest {
  serverUrl: string;
  /** Remote paths (with "~/" prefixes) created by this run. */
  createdPaths: string[];
}

export const REMOTE_PROVISION_MANIFEST = 'remote-provision-manifest.json';

export function readManifest(sandbox: string): RemoteProvisionManifest | null {
  const file = `${sandbox}/${REMOTE_PROVISION_MANIFEST}`;
  let raw: string;
  try {
    raw = fs.readFileSync(file, 'utf-8');
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'ENOENT') {
      console.warn(`[remote-provision] WARNING: could not read ${file}: ${(err as Error).message}`);
    }
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<RemoteProvisionManifest>;
    if (typeof parsed.serverUrl !== 'string' || !Array.isArray(parsed.createdPaths)) {
      console.warn(`[remote-provision] WARNING: unrecognized manifest shape in ${file}; ignoring it`);
      return null;
    }
    return { serverUrl: parsed.serverUrl, createdPaths: parsed.createdPaths.filter((p): p is string => typeof p === 'string') };
  } catch (err) {
    console.warn(`[remote-provision] WARNING: malformed JSON in ${file}: ${(err as Error).message}`);
    return null;
  }
}

export function writeManifest(sandbox: string, manifest: RemoteProvisionManifest): void {
  fs.writeFileSync(`${sandbox}/${REMOTE_PROVISION_MANIFEST}`, JSON.stringify(manifest, null, 2));
}

export function removeManifest(sandbox: string): void {
  fs.rmSync(`${sandbox}/${REMOTE_PROVISION_MANIFEST}`, { force: true });
}

/**
 * Open a browser against the external server, log in, and wait for the R
 * console -- the shared login flow from fixtures/server.fixture.ts, headless
 * because no test drives this UI. Throws when PW_RSTUDIO_SERVER_URL is unset.
 */
export async function connectExternalSession(): Promise<ExternalSession> {
  const serverUrl = externalServerUrl();
  if (!serverUrl) {
    throw new Error('connectExternalSession called without PW_RSTUDIO_SERVER_URL set');
  }
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto(serverUrl, { waitUntil: 'domcontentloaded' });
    await signInToServer(page);
    return { browser, page };
  } catch (err) {
    await browser.close();
    throw err;
  }
}

/**
 * End the remote R session (so the next login starts a fresh rsession that
 * reads the just-provisioned stores) and close the browser. Ending the
 * session matters: AI backends inherit their credentials at rsession startup,
 * and the provisioning session itself started before the stores existed.
 */
export async function closeExternalSession(session: ExternalSession): Promise<void> {
  try {
    await executeInConsole(session.page, 'quit(save = "no")', { wait: false });
    await session.page
      .locator('[role="alertdialog"]', { hasText: 'R Session Ended' })
      .waitFor({ state: 'visible', timeout: 15_000 });
  } catch {
    // Best effort -- a session that didn't quit cleanly resumes next login,
    // where the fresh-credential guarantee is weaker but not broken (the AI
    // backends there start on demand, after the stores are in place).
    console.warn('[remote-provision] WARNING: could not confirm the remote R session ended; the next login may resume it');
  }
  await session.browser.close();
}

/**
 * Evaluate an R expression yielding a single logical on the remote session.
 * Thin wrapper over ConsolePaneActions.evalRLogical to keep call sites here
 * uniform. Returns null when the result couldn't be read back.
 */
export async function evalRemoteLogical(page: Page, expr: string): Promise<boolean | null> {
  return new ConsolePaneActions(page).evalRLogical(expr);
}

/**
 * True/false/null: does `remotePath` exist on the remote host?
 */
export async function remotePathExists(page: Page, remotePath: string): Promise<boolean | null> {
  return evalRemoteLogical(page, `file.exists(${rStringLiteral(remotePath)})`);
}

/**
 * Poll for `remotePath` to exist, up to `timeoutMs`. Returns immediately when
 * it already does; false when the deadline passes without it appearing. Used
 * to let a remote-side writer with variable timing (the AI client's startup
 * stub, see remotePositAiStoreAuthenticated) finish before this side writes
 * the same path, instead of racing it.
 */
export async function waitForRemotePath(page: Page, remotePath: string, timeoutMs: number): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    if ((await remotePathExists(page, remotePath)) === true) return true;
    if (Date.now() >= deadline) return false;
    await page.waitForTimeout(500);
  }
}

/**
 * Whether the remote Posit AI store holds a real sign-in. The rsession's AI
 * client writes an unauthenticated stub data.json at session startup --
 * including the login this provisioning session just performed -- so bare
 * existence cannot distinguish "this account is signed in" from "a session
 * started here once". Probe the content instead: the auth entry key plus an
 * access-token marker, mirroring what isStoreFileAuthenticated checks locally,
 * minus token expiry (no JSON parser is assumed on the remote host). The
 * probe reads back a single logical; none of the file's content is echoed to
 * the console. Returns null when the result couldn't be read (including a
 * readLines failure), which callers treat as "don't touch the file".
 */
export async function remotePositAiStoreAuthenticated(page: Page): Promise<boolean | null> {
  const p = rStringLiteral(REMOTE_POSITAI_STORE);
  return evalRemoteLogical(
    page,
    `local({ txt <- paste(readLines(${p}, warn = FALSE), collapse = ""); `
      + `grepl(${rStringLiteral(AUTH_STORAGE_KEY)}, txt, fixed = TRUE) && `
      + `grepl(${rStringLiteral('"accessToken"')}, txt, fixed = TRUE) })`,
  );
}

/**
 * Whether the remote Copilot store holds a sign-in. Existence is as
 * misleading as for the Posit AI store: any session that enables Copilot
 * spawns the copilot-language-server, which creates an empty auth.db on
 * startup -- gate-skipped tests included -- so a bare "auth.db exists" can
 * mean nothing more than "Copilot was once switched on here". auth.db is
 * SQLite, which base R can't query, but a real sign-in leaves a GitHub OAuth
 * token (its "gho_" prefix in cleartext, since the store is written
 * unencrypted) somewhere in the database bytes -- the main file or its -wal
 * sidecar, where an uncheckpointed token row lives. grepRaw over both is
 * crude but decisive: an empty shell contains no token marker. As with the
 * Posit AI probe only a logical is read back, and null means "couldn't
 * tell", which callers treat as "don't touch the file".
 */
export async function remoteCopilotStoreAuthenticated(page: Page): Promise<boolean | null> {
  const db = `${REMOTE_COPILOT_DIR}/auth.db`;
  const holdsToken = (f: string) => {
    const p = rStringLiteral(f);
    return `(file.exists(${p}) && length(grepRaw("gho_", readBin(${p}, "raw", file.size(${p})), fixed = TRUE)) > 0)`;
  };
  return evalRemoteLogical(page, `${holdsToken(db)} || ${holdsToken(`${db}-wal`)}`);
}

/**
 * Write a text file on the remote host at `remotePath` (a "~/" path), creating
 * parent directories, then chmod it to `mode`. Delivered through the chunked
 * binary writer: a single writeLines command carrying the whole content would
 * hit R's console line-length limit (see BINARY_CHUNK_BYTES) once the content
 * grows past a few KiB, and the byte-for-byte result is identical. Verifies
 * the write by size; throws on mismatch.
 */
export async function writeRemoteText(
  page: Page,
  remotePath: string,
  content: string,
  mode = '0600',
): Promise<void> {
  await writeRemoteBinary(page, remotePath, Buffer.from(content, 'utf-8'), mode);
}

// Hex payload per console command. R caps a console input line at about 4095
// bytes -- a longer command is cut off mid-string and nothing lands -- so the
// payload must keep each command safely under that. 1536 file bytes become
// 3072 hex characters, leaving ample room for the .pwWriteHex call around it.
const BINARY_CHUNK_BYTES = 1536;

/**
 * Write a binary file on the remote host at `remotePath` (a "~/" path) from a
 * local Buffer, creating parent directories, then chmod it to `mode`. The
 * bytes travel hex-encoded in chunks, decoded and appended remotely with
 * base-R writeBin -- no remote packages assumed. Verifies the final size;
 * throws on mismatch.
 */
export async function writeRemoteBinary(
  page: Page,
  remotePath: string,
  data: Buffer,
  mode = '0600',
): Promise<void> {
  const p = rStringLiteral(remotePath);
  await executeInConsole(page, `dir.create(dirname(path.expand(${p})), recursive = TRUE, showWarnings = FALSE)`);
  // A temporary decoder in the remote global env keeps the per-chunk commands
  // short; removed below once the file is written.
  await executeInConsole(
    page,
    '.pwWriteHex <- function(path, hex, append) { '
      + 'con <- file(path, if (append) "ab" else "wb"); on.exit(close(con)); '
      + 'n <- nchar(hex); '
      + 'writeBin(as.raw(strtoi(substring(hex, seq(1L, n, 2L), seq(2L, n, 2L)), 16L)), con); '
      + 'invisible(NULL) }',
  );
  try {
    // Two attempts: a concurrent remote-side writer (the AI client's startup
    // stub, a straggler past the waitForRemotePath window) can land on top of
    // a fresh write, and the first chunk's append=FALSE truncates, so a
    // rewrite starts clean.
    for (let attempt = 1; attempt <= 2; attempt++) {
      for (let offset = 0; offset < data.length; offset += BINARY_CHUNK_BYTES) {
        const hex = data.subarray(offset, offset + BINARY_CHUNK_BYTES).toString('hex');
        const append = offset > 0 ? 'TRUE' : 'FALSE';
        await executeInConsole(page, `.pwWriteHex(${p}, "${hex}", ${append})`);
      }
      // A zero-byte source still needs the file created (truncated).
      if (data.length === 0) {
        await executeInConsole(page, `.pwWriteHex(${p}, "", FALSE)`);
      }
      await executeInConsole(page, `Sys.chmod(path.expand(${p}), ${rStringLiteral(mode)})`);
      if ((await evalRemoteLogical(page, `file.size(${p}) == ${data.length}`)) === true) return;
      if (attempt === 1) {
        console.warn(
          `[remote-provision] WARNING: remote write verification failed for ${remotePath}; retrying once`,
        );
      }
    }
    throw new Error(
      `[remote-provision] remote write verification failed for ${remotePath} after retry: expected ${data.length} bytes`,
    );
  } finally {
    await executeInConsole(page, 'rm(.pwWriteHex)');
  }
}

/**
 * Best-effort removal of remote paths. Each is unlink()ed (recursively, so a
 * directory entry removes its contents too), then re-probed; returns the
 * paths that still exist so the caller can warn loudly. R's unlink does not
 * error on missing paths, so scrubbing an already-clean host is quiet.
 */
export async function scrubRemote(page: Page, remotePaths: string[]): Promise<string[]> {
  if (remotePaths.length === 0) return [];
  const vec = remotePaths.map((p) => rStringLiteral(p)).join(', ');
  await executeInConsole(page, `unlink(c(${vec}), recursive = TRUE)`);
  const survivors: string[] = [];
  for (const remotePath of remotePaths) {
    const exists = await remotePathExists(page, remotePath);
    // null (unreadable result) is treated as a survivor: the caller's warning
    // errs toward "check the host" rather than silently assuming clean.
    if (exists !== false) survivors.push(remotePath);
  }
  return survivors;
}
