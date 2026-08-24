import { chromium } from 'playwright';
import type { Browser, Page } from 'playwright';
import { expect } from '@playwright/test';
import { randomBytes } from 'crypto';
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
 * What the harness does hold, once logged in, is a session running as that
 * account -- so the sandbox credential stores are delivered through it into the
 * remote home.
 *
 * The credential bytes travel over HTTP, through the same /upload endpoint the
 * Files pane posts to (see uploadRemoteFile). The R console is used only to
 * move the uploaded file into place and to probe what is already there, so no
 * console command here ever carries a file's contents -- only paths, and the
 * fixed patterns the probes match on. That distinction is the whole design:
 * RStudio records each submitted console command in the account's history
 * database, so a command carrying a store's contents would leave a recoverable
 * copy of a live token there -- one this run could not clean up if the history
 * file predated it. Keep it that way; never put file contents into a console
 * command in this file.
 *
 * Everything here is driven from the auth.setup project (provisioning) and
 * the auth-teardown project (scrubbing); see tests/auth.setup.ts and
 * tests/auth.teardown.ts. Both projects run with Playwright artifacts off,
 * because they drive a real login form -- a trace, video, or screenshot would
 * capture PW_RSTUDIO_SERVER_PASSWORD into the report.
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

/**
 * The provisioning record the setup step writes into the sandbox
 * (<sandbox>/remote-provision-manifest.json) for the teardown to scrub from.
 * Only paths this run created are listed -- the teardown must never delete
 * credentials it didn't write.
 */
export interface RemoteProvisionManifest {
  serverUrl: string;
  /**
   * Remote paths created by this run: the "~/" store paths, plus the absolute
   * server-side temp file each upload lands in before the console moves it
   * into place. Both kinds are scrubbed, since a run that dies mid-push can
   * leave either behind.
   */
  createdPaths: string[];
}

export const REMOTE_PROVISION_MANIFEST = 'remote-provision-manifest.json';

/**
 * The manifest for `sandbox`, or null when there isn't one. A missing file is
 * the only quiet null: an unreadable or malformed manifest throws. The
 * teardown reads null as "nothing was provisioned, so nothing to scrub", so
 * degrading a damaged manifest to null would quietly abandon whatever real
 * tokens it names on the remote host -- the one outcome this file exists to
 * prevent.
 */
export function readManifest(sandbox: string): RemoteProvisionManifest | null {
  const file = `${sandbox}/${REMOTE_PROVISION_MANIFEST}`;
  let raw: string;
  try {
    raw = fs.readFileSync(file, 'utf-8');
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code === 'ENOENT') return null;
    throw new Error(`[remote-provision] could not read ${file}: ${(err as Error).message}`);
  }
  let parsed: Partial<RemoteProvisionManifest>;
  try {
    parsed = JSON.parse(raw) as Partial<RemoteProvisionManifest>;
  } catch (err) {
    throw new Error(`[remote-provision] malformed JSON in ${file}: ${(err as Error).message}`);
  }
  if (typeof parsed.serverUrl !== 'string' || !Array.isArray(parsed.createdPaths)) {
    throw new Error(`[remote-provision] unrecognized manifest shape in ${file}`);
  }
  // Every entry must be a string. Filtering the bad ones out instead would
  // silently drop a scrub target -- a path this run created, now unnamed and
  // left on the remote host, which is precisely what this manifest exists to
  // prevent. A damaged manifest is a stop-and-look, not something to salvage.
  if (!parsed.createdPaths.every((p): p is string => typeof p === 'string')) {
    throw new Error(
      `[remote-provision] ${file} lists a non-string path; it may name credentials this run left behind, `
        + 'so check the remote host by hand rather than trusting the scrub',
    );
  }
  return { serverUrl: parsed.serverUrl, createdPaths: parsed.createdPaths };
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
 * Returns true, false, or null when the result couldn't be read back.
 *
 * Deliberately not ConsolePaneActions.evalRLogical, which clears the console
 * and then matches the first `[1] TRUE/FALSE` anywhere in the pane. The clear
 * is a Ctrl+L keystroke followed by a fixed sleep, with no confirmation that
 * it landed -- and every probe here runs right after a write command that
 * prints `[1] TRUE` itself (Sys.chmod, file.copy). A clear that silently fails
 * would let a verification probe match the write's own output and report
 * success for a push that never happened. That is the one direction these
 * probes must never fail in: a false negative wastes a run, a false positive
 * vouches for credentials that aren't there.
 *
 * So the answer carries a per-call nonce and is matched only in that form. A
 * stale line cannot satisfy it, because the nonce did not exist when that line
 * was printed. Output is emitted through a single cat() rather than R's
 * auto-printing, so the value and its tag cannot be split across renders.
 */
export async function evalRemoteLogical(page: Page, expr: string): Promise<boolean | null> {
  const nonce = `pw${randomBytes(6).toString('hex')}`;
  // Build the tag from parts at run time: a literal in the command would also
  // appear in the console's echo of the command itself, which the poll below
  // reads -- the same self-match trap the tag exists to close.
  const marker = `paste0("[", "${nonce}", ":", if (isTRUE(v)) "T" else if (isFALSE(v)) "F" else "NA", "]")`;
  await executeInConsole(page, `local({ v <- (${expr}); cat(${marker}, "\\n", sep = "") })`);
  const pane = new ConsolePaneActions(page).consolePane.consoleOutput;
  const re = new RegExp(`\\[${nonce}:(T|F|NA)\\]`);
  try {
    await expect.poll(async () => re.test(await pane.innerText()), { timeout: 15_000 }).toBe(true);
  } catch {
    return null;
  }
  const m = (await pane.innerText()).match(re);
  return m === null || m[1] === 'NA' ? null : m[1] === 'T';
}

/**
 * True/false/null: does `remotePath` exist on the remote host?
 */
export async function remotePathExists(page: Page, remotePath: string): Promise<boolean | null> {
  return evalRemoteLogical(page, `file.exists(${rStringLiteral(remotePath)})`);
}

/**
 * Evaluate an R expression yielding a single string on the remote session.
 * Returns the string, or null when the result couldn't be read back.
 *
 * Same nonce-tagged read-back as evalRemoteLogical, and for the same reason:
 * a stale console line from an earlier command (this is typically paired with
 * a dir.create() or similar side-effecting call) must never satisfy a later
 * probe. The delimiter is assembled from separately-quoted fragments, also
 * for the same reason evalRemoteLogical's marker is: written as one literal
 * ("<<nonce>>") it would appear contiguous in the console's echo of this very
 * command, and the regex below would match that instead of the real answer
 * printed underneath -- verified the hard way, as a resolved database
 * directory that came back as the literal text of this function's own
 * paste0() call.
 */
export async function evalRemoteString(page: Page, expr: string): Promise<string | null> {
  const nonce = `pw${randomBytes(6).toString('hex')}`;
  const marker = `paste0("<<", "${nonce}", ">>", v, "<<", "${nonce}", ">>")`;
  await executeInConsole(page, `local({ v <- (${expr}); cat(${marker}, "\\n", sep = "") })`);
  const pane = new ConsolePaneActions(page).consolePane.consoleOutput;
  const re = new RegExp(`<<${nonce}>>(.*?)<<${nonce}>>`);
  try {
    await expect.poll(async () => re.test(await pane.innerText()), { timeout: 15_000 }).toBe(true);
  } catch {
    return null;
  }
  const m = (await pane.innerText()).match(re);
  return m === null ? null : m[1];
}

/**
 * Poll for `remotePath` to exist, up to `timeoutMs`. Returns immediately when
 * it already does. Used to let a remote-side writer with variable timing (the
 * AI client's startup stub, see remotePositAiStoreAuthenticated) finish before
 * this side writes the same path, instead of racing it.
 *
 * Carries the last probe's verdict out on expiry: false when the path is
 * genuinely absent, null when that final probe could not be read. The
 * distinction matters to callers -- collapsing an unreadable probe into
 * "absent" would let provisioning overwrite a store it never managed to look
 * at, which could be the account's own real sign-in.
 */
export async function waitForRemotePath(page: Page, remotePath: string, timeoutMs: number): Promise<boolean | null> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const exists = await remotePathExists(page, remotePath);
    if (exists === true) return true;
    if (Date.now() >= deadline) return exists;
    await page.waitForTimeout(500);
  }
}

/**
 * Whether the remote Posit AI store holds a real sign-in. The rsession's AI
 * client writes an unauthenticated stub data.json at session startup --
 * including the login this provisioning session just performed -- so bare
 * existence cannot distinguish "this account is signed in" from "a session
 * started here once". Probe the content instead, mirroring the markers
 * isStoreFileAuthenticated checks locally: the auth entry key, the
 * "authenticated": true flag that its isAuthEntry guard requires, and an
 * access-token marker.
 *
 * Two ways this is weaker than the local guard, neither fixable from here. No
 * JSON parser is assumed on the remote host, so token expiry goes unchecked
 * and an expired-but-well-formed store reads as authenticated. And the markers
 * are matched against the whole file rather than within one entry, so a store
 * carrying them in unrelated places would pass. Both err toward leaving a
 * remote store alone, which is the safe direction.
 *
 * The probe reads back a single logical; none of the file's content is echoed
 * to the console. Returns null when the result couldn't be read (including a
 * readLines failure), which callers treat as "don't touch the file".
 */
export async function remotePositAiStoreAuthenticated(page: Page): Promise<boolean | null> {
  const p = rStringLiteral(REMOTE_POSITAI_STORE);
  // POSIX character classes rather than \s: the pattern crosses into R as a
  // string literal, where a backslash escape would need doubling to survive.
  const authenticatedFlag = rStringLiteral('"authenticated"[[:space:]]*:[[:space:]]*true');
  return evalRemoteLogical(
    page,
    `local({ txt <- paste(readLines(${p}, warn = FALSE), collapse = ""); `
      + `grepl(${rStringLiteral(AUTH_STORAGE_KEY)}, txt, fixed = TRUE) && `
      + `grepl(${authenticatedFlag}, txt) && `
      + `grepl(${rStringLiteral('"accessToken"')}, txt, fixed = TRUE) })`,
  );
}

/**
 * The marker a signed-in Copilot store leaves in its bytes: one of GitHub's
 * token prefixes. The family matters -- ghp_ is a personal access token, gho_
 * an OAuth-app token, ghu_ user-to-server, ghs_ server-to-server, ghr_ a
 * refresh token. Copilot authenticates as a GitHub *App* (its oauth_client_id
 * is an "Iv1." App id), so real stores hold ghu_; matching gho_ alone finds
 * nothing in any genuine sign-in.
 *
 * One definition, used by both the remote probe below and the caller deciding
 * whether the bytes it just pushed carry a marker worth re-probing for. Those
 * two must agree: a guard armed on evidence the probe never looks at turns a
 * good push into a reported failure.
 *
 * A prefix scan cannot distinguish a live token from an expired one, and it
 * is deliberately broader than the local gate (isCopilotStoreAuthenticated,
 * which counts oauth_tokens rows through real SQLite). Erring toward "this
 * store holds a sign-in" is the safe direction: it leaves a remote store
 * alone rather than overwriting and later scrubbing what may be someone's
 * real credentials.
 */
const COPILOT_TOKEN_MARKER_PATTERN = 'gh[opusr]_';

/** Whether `bytes` carry a GitHub token marker (see COPILOT_TOKEN_MARKER_PATTERN). */
export function bytesHoldCopilotToken(bytes: Buffer): boolean {
  // latin1 maps each byte to one code unit, so a binary SQLite page can be
  // scanned as text without a decoder dropping or merging bytes.
  return new RegExp(COPILOT_TOKEN_MARKER_PATTERN).test(bytes.toString('latin1'));
}

/**
 * Whether the remote Copilot store holds a sign-in. Existence is as
 * misleading as for the Posit AI store: any session that enables Copilot
 * spawns the copilot-language-server, which creates an empty auth.db on
 * startup -- gate-skipped tests included -- so a bare "auth.db exists" can
 * mean nothing more than "Copilot was once switched on here". auth.db is
 * SQLite, which base R can't query, but a real sign-in leaves a GitHub token
 * in cleartext (the store is written unencrypted) somewhere in the database
 * bytes -- the main file or its -wal sidecar, where an uncheckpointed token
 * row lives. grepRaw over both is crude but decisive: an empty shell contains
 * no token marker. As with the Posit AI probe only a logical is read back,
 * and null means "couldn't tell", which callers treat as "don't touch the
 * file".
 */
export async function remoteCopilotStoreAuthenticated(page: Page): Promise<boolean | null> {
  const db = `${REMOTE_COPILOT_DIR}/auth.db`;
  const marker = rStringLiteral(COPILOT_TOKEN_MARKER_PATTERN);
  const holdsToken = (f: string) => {
    const p = rStringLiteral(f);
    // fixed = FALSE: the marker is a character class, not a literal.
    return `(file.exists(${p}) && length(grepRaw(${marker}, readBin(${p}, "raw", file.size(${p})), fixed = FALSE)) > 0)`;
  };
  return evalRemoteLogical(page, `${holdsToken(db)} || ${holdsToken(`${db}-wal`)}`);
}

/**
 * Write a text file on the remote host at `remotePath` (a "~/" path), creating
 * parent directories, with final mode `mode`. Delegates to the binary writer so
 * text and binary take one code path; the remote file holds exactly the bytes of
 * `content`, with no trailing newline added. Verifies the result; throws on
 * mismatch.
 */
export async function writeRemoteText(
  page: Page,
  remotePath: string,
  content: string,
  mode = '0600',
  recordTempFile?: (tempPath: string) => void,
): Promise<void> {
  await writeRemoteBinary(page, remotePath, Buffer.from(content, 'utf-8'), mode, recordTempFile);
}

/**
 * Upload `data` to the remote host over HTTP and return the absolute path of the
 * server-side temporary file now holding it.
 *
 * This is how credential bytes reach the remote host without ever appearing in a
 * console command, and therefore without landing in the account's console
 * history. The endpoint is the one the Files pane's upload dialog posts to. It
 * requires no CSRF header: its real client is a hidden iframe form, which cannot
 * set custom headers, so the handler is registered outside the JSON-RPC dispatch
 * path that enforces CSRF (registerUploadHandler in
 * src/cpp/session/modules/SessionFiles.cpp).
 *
 * Two details of the form are load-bearing, both verified against the server's
 * parser:
 *  - The file part must come FIRST and targetDirectory LAST. The server recovers
 *    targetDirectory by searching backwards from the closing multipart boundary,
 *    so the reverse order fails with "Parameter value invalid".
 *  - The file field is named "File".
 *
 * targetDirectory is always "~". The server echoes it back but nothing here uses
 * it, because the caller moves the temp file into place itself -- so pointing it
 * at a directory certain to exist keeps this independent of whether the endpoint
 * would accept a path that doesn't yet.
 */
async function uploadRemoteFile(page: Page, data: Buffer, filename: string): Promise<string> {
  const serverUrl = externalServerUrl();
  if (!serverUrl) {
    throw new Error('uploadRemoteFile called without PW_RSTUDIO_SERVER_URL set');
  }
  // externalServerUrl() has already stripped any trailing slash.
  const response = await page.request.post(`${serverUrl}/upload`, {
    multipart: {
      File: { name: filename, mimeType: 'application/octet-stream', buffer: data },
      targetDirectory: '~',
    },
  });
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    throw new Error(
      `[remote-provision] upload of ${filename} returned a non-JSON response (HTTP ${response.status()})`,
    );
  }
  // Shape: { result: { token: { uploadedTempFile, ... } } } on success,
  // { error: { message } } otherwise -- e.g. a limit-file-upload-size-mb
  // rejection. Neither carries file content, so quoting it back is safe.
  const token = (body as { result?: { token?: { uploadedTempFile?: unknown } } })?.result?.token;
  const tempFile = token?.uploadedTempFile;
  if (typeof tempFile !== 'string' || tempFile.length === 0) {
    const detail = (body as { error?: { message?: string } })?.error?.message ?? JSON.stringify(body);
    throw new Error(
      `[remote-provision] upload of ${filename} did not yield a temp file (HTTP ${response.status()}): ${detail}`,
    );
  }
  return tempFile;
}

/**
 * Whether a file just written on the remote host has the expected size AND
 * mode. Mode is part of the check because the credential stores must land 0600
 * and Sys.chmod reports failure by returning FALSE rather than erroring, so an
 * unapplied chmod would otherwise be invisible. file.info on a missing path
 * yields NA, hence the is.na guard -- the whole expression is then FALSE, not an
 * error.
 */
async function verifyRemoteFile(
  page: Page,
  remotePath: string,
  expectedBytes: number,
  mode: string,
): Promise<boolean | null> {
  const p = rStringLiteral(remotePath);
  // as.character() on file.info()$mode (an octmode) yields "600", not "0600".
  const expectedMode = rStringLiteral(mode.replace(/^0+(?=\d{3}$)/, ''));
  return evalRemoteLogical(
    page,
    `local({ i <- file.info(path.expand(${p})); `
      + `!is.na(i$size) && i$size == ${expectedBytes} && `
      + `identical(as.character(i$mode), ${expectedMode}) })`,
  );
}

/**
 * Write a binary file on the remote host at `remotePath` (a "~/" path) from a
 * local Buffer, creating parent directories, with final mode `mode`. The bytes
 * are uploaded over HTTP; only the two paths involved cross the R console. When
 * `recordTempFile` is given it is called with the server-side temp path as soon
 * as the upload lands, so a caller tracking what to clean up learns about the
 * file while it still exists. Verifies size and mode; throws on mismatch.
 */
export async function writeRemoteBinary(
  page: Page,
  remotePath: string,
  data: Buffer,
  mode = '0600',
  recordTempFile?: (tempPath: string) => void,
): Promise<void> {
  const p = rStringLiteral(remotePath);
  const m = rStringLiteral(mode);
  await executeInConsole(page, `dir.create(dirname(path.expand(${p})), recursive = TRUE, showWarnings = FALSE)`);

  // A zero-byte source -- an empty SQLite -shm sidecar is the common case -- has
  // no bytes to upload, so create the file directly rather than posting an empty
  // form part and depending on how the server's parser handles one.
  if (data.length === 0) {
    await executeInConsole(
      page,
      `file.create(path.expand(${p})); Sys.chmod(path.expand(${p}), ${m})`,
    );
    if ((await verifyRemoteFile(page, remotePath, 0, mode)) !== true) {
      throw new Error(
        `[remote-provision] remote write verification failed for ${remotePath}: expected an empty file with mode ${mode}`,
      );
    }
    return;
  }

  // Two attempts: a concurrent remote-side writer (the AI client's startup stub,
  // a straggler past the waitForRemotePath window) can land on top of a fresh
  // write. Each attempt re-uploads, since the move below consumes the temp file.
  for (let attempt = 1; attempt <= 2; attempt++) {
    const tempFile = await uploadRemoteFile(page, data, remotePath.split('/').pop() || 'upload.bin');
    recordTempFile?.(tempFile);
    const t = rStringLiteral(tempFile);
    // Paths only -- never the file's contents (see the module comment). The temp
    // file is chmod'ed before the copy, and copy.mode carries that across, so
    // the destination never exists at umask permissions while holding a token.
    await executeInConsole(
      page,
      `Sys.chmod(${t}, ${m}); `
        + `file.copy(${t}, path.expand(${p}), overwrite = TRUE, copy.mode = TRUE); `
        + `Sys.chmod(path.expand(${p}), ${m}); `
        + `unlink(${t})`,
    );
    if ((await verifyRemoteFile(page, remotePath, data.length, mode)) === true) return;
    if (attempt === 1) {
      console.warn(
        `[remote-provision] WARNING: remote write verification failed for ${remotePath}; retrying once`,
      );
    }
  }
  throw new Error(
    `[remote-provision] remote write verification failed for ${remotePath} after retry: `
      + `expected ${data.length} bytes with mode ${mode}`,
  );
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
