/**
 * Helpers for the Connections pane tests.
 *
 * Node-side (used by globalSetup): prepareOdbcSandbox builds a sandbox-local
 * ODBC configuration directory that ODBCSYSINI points the session at, so the
 * drivers the tests see are exactly the ones declared here and the machine's
 * real ODBC configuration is never read or written.
 *
 * Test-side: in-session probes and selector derivation. Probes run in the
 * rsession (through the R console), not in the test runner: what matters is
 * what the session under test can see, which differs from the runner's
 * machine in remote Server mode.
 */

import { spawnSync } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import type { Page } from '@playwright/test';
import { ConsolePaneActions } from '../actions/console_pane.actions';
import { executeInConsole } from '../pages/console_pane.page';
import {
  ALL_DB_TARGETS,
  DbTarget,
  connectionHostId,
  effectiveTarget,
  resolveDriverLibrary,
  wizardSnippet,
} from './db-targets';
import { readDbStatus } from './db-provision';
import { drainClientExceptions } from './commands';
import { remotePathExists, writeRemoteText } from './remote-provision';
import { rStringLiteral } from './r';

/**
 * The filename the session derives a driver's snippet from. Mirrors
 * .rs.connectionStripRStudioDriver + tolower (SessionConnectionsInstaller.R):
 * the gsub pattern there removes the " with Posit Driver" suffix AND every
 * space, so "PostgreSQL Unicode" looks up "postgresqlunicode.R".
 */
export function snippetFileName(driverName: string): string {
  return `${driverName.replace(/ with Posit Driver/g, '').replace(/ /g, '').toLowerCase()}.R`;
}

export interface OdbcSandbox {
  /**
   * Value for ODBCSYSINI, or null when there is nothing to point it at.
   *
   * Always null on Windows: there is no ODBCSYSINI equivalent, so drivers are
   * registered in the machine-wide registry instead and `registered` is the
   * only signal that anything is available. Callers gating on "did the suite
   * make a driver available" must therefore check `registered`, not this.
   */
  odbcDir: string | null;
  /** Ids of the targets whose drivers were found and registered. */
  registered: string[];
  /**
   * Windows only: directories holding the drivers' dependent DLLs, to be
   * prepended to the session's PATH. Empty elsewhere. See the note in
   * prepareOdbcSandboxWindows about why the DLL is copied alone.
   */
  driverPaths: string[];
}

/** Windows directories we must never copy out of, nor add to PATH. */
function isWindowsSystemDir(dir: string): boolean {
  const windir = process.env.SystemRoot ?? process.env.windir ?? 'C:\\Windows';
  const normalized = path.resolve(dir).toLowerCase();
  return normalized.startsWith(path.resolve(windir).toLowerCase());
}

/** HKLM key holding one subkey per ODBC driver, plus the enumeration list. */
const WIN_ODBCINST = 'HKLM\\SOFTWARE\\ODBC\\ODBCINST.INI';
/** Value list the driver manager enumerates; a driver absent here is invisible. */
const WIN_ODBC_DRIVERS = `${WIN_ODBCINST}\\ODBC Drivers`;
/** Names this run registered, so teardown removes only what it created. */
const WIN_MANIFEST = 'registered-windows-drivers.json';

/**
 * Run reg.exe. Always /reg:64, because the 32-bit view is a separate registry
 * branch (WOW6432Node) and a 64-bit RStudio reads only the 64-bit one; letting
 * the process bitness decide would silently register where nothing looks.
 */
function reg(args: string[]): { status: number; stdout: string } {
  const run = spawnSync('reg.exe', [...args, '/reg:64'], { encoding: 'utf8' });
  return { status: run.status ?? 1, stdout: run.stdout ?? '' };
}

/** Driver names currently in the enumeration list. */
function winRegisteredDriverNames(): string[] {
  const { status, stdout } = reg(['query', WIN_ODBC_DRIVERS]);
  if (status !== 0) return [];
  // Value lines look like: "    <name>    REG_SZ    Installed". Names may
  // contain spaces, so split on the type token rather than on whitespace.
  return stdout
    .split(/\r?\n/)
    .map((line) => /^\s+(.+?)\s+REG_(?:SZ|EXPAND_SZ)\s+/.exec(line))
    .filter((m): m is RegExpExecArray => m !== null)
    .map((m) => m[1]);
}

/** The `Driver` value (DLL path) of one registered driver, or null. */
function winDriverPath(driverName: string): string | null {
  const { status, stdout } = reg(['query', `${WIN_ODBCINST}\\${driverName}`, '/v', 'Driver']);
  if (status !== 0) return null;
  const m = /^\s+Driver\s+REG_(?:SZ|EXPAND_SZ)\s+(.+?)\s*$/m.exec(stdout);
  return m ? m[1] : null;
}

/**
 * Locate the DLL the vendor's own installer put on this machine, by matching
 * the target's pattern against the registered driver names. Returns null when
 * the driver is not installed, which is not an error: the target simply goes
 * unregistered and its specs skip.
 */
function winInstalledDriverLibrary(target: DbTarget): string | null {
  if (!target.windowsInstalledDriverPattern) return null;
  for (const name of winRegisteredDriverNames()) {
    if (name === target.driverName) continue; // our own registration
    if (!target.windowsInstalledDriverPattern.test(name)) continue;
    const dll = winDriverPath(name);
    if (dll && fs.existsSync(dll)) return dll;
  }
  return null;
}

/**
 * Register one driver under the suite's own name, pointing at the sandbox copy.
 *
 * Refuses to overwrite a live registration someone else owns: if the name is
 * already taken and its DLL exists, this returns a reason instead. A dangling
 * entry (name present, DLL gone) is treated as a leftover from a run that was
 * killed before teardown and is reclaimed, so one hard-killed run cannot wedge
 * every later one.
 */
function winRegisterDriver(driverName: string, dllPath: string): string | null {
  const existing = winRegisteredDriverNames().includes(driverName);
  if (existing) {
    const current = winDriverPath(driverName);
    if (current && fs.existsSync(current)) {
      return `"${driverName}" is already registered on this machine at ${current}; refusing to overwrite a real driver`;
    }
  }

  const key = `${WIN_ODBCINST}\\${driverName}`;

  // The full conventional attribute set, not just Driver and Setup.
  //
  // This is load-bearing, and the reason is not obvious. On Windows
  // odbcListDrivers() reports none of Driver/Setup/Description -- only the
  // attributes below (verified on windows-2025: APILevel, ConnectFunctions,
  // CPTimeout, DriverODBCVer, FileUsage, SQLLevel, UsageCount). A driver
  // carrying none of them therefore yields *no* rows from odbcListDrivers,
  // and when SessionConnections.R rbinds that with its own registry read the
  // result gains a row whose attribute is NA. That NA row matches
  // `attribute == "Driver"`, so currentDriver comes out length 2, and
  // `if (dir.exists(snippetsDir))` in .rs.connectionReadOdbcEntry throws "the
  // condition has length > 1". The tryCatch there turns the error into NULL,
  // and the driver silently vanishes from the New Connection wizard.
  //
  // Writing the standard attributes avoids that, and is what a correctly
  // registered driver should carry anyway -- every real driver on the machine
  // has them. Values follow psqlODBC's and sqliteodbc's own registrations.
  const values: Array<[name: string, type: string, data: string]> = [
    ['Driver', 'REG_SZ', dllPath],
    ['Setup', 'REG_SZ', dllPath],
    ['APILevel', 'REG_SZ', '1'],
    ['ConnectFunctions', 'REG_SZ', 'YYY'],
    ['DriverODBCVer', 'REG_SZ', '03.51'],
    ['FileUsage', 'REG_SZ', '0'],
    ['SQLLevel', 'REG_SZ', '1'],
    ['UsageCount', 'REG_DWORD', '1'],
  ];
  for (const [name, type, data] of values) {
    const add = reg(['add', key, '/v', name, '/t', type, '/d', data, '/f']);
    if (add.status !== 0) {
      return `reg add ${key} /v ${name} failed (exit ${add.status}); an HKLM write needs an elevated shell`;
    }
  }
  const list = reg(['add', WIN_ODBC_DRIVERS, '/v', driverName, '/t', 'REG_SZ', '/d', 'Installed', '/f']);
  if (list.status !== 0) {
    return `reg add "${WIN_ODBC_DRIVERS}" /v ${driverName} failed (exit ${list.status})`;
  }
  return null;
}

/**
 * Remove the registrations this run created, named by the sandbox manifest.
 *
 * Called from globalTeardown unconditionally and before any file removal, for
 * the same reason the database servers are stopped there: a preserved sandbox
 * may keep its files for inspection, but it must never leave machine-wide
 * registry state behind. Only names in the manifest are touched, so a real
 * driver that happened to share a name is safe.
 */
export function unregisterWindowsOdbcDrivers(sandbox: string): void {
  if (process.platform !== 'win32') return;
  const manifest = path.join(sandbox, 'odbc', WIN_MANIFEST);
  if (!fs.existsSync(manifest)) return;

  let names: string[] = [];
  try {
    names = JSON.parse(fs.readFileSync(manifest, 'utf8')) as string[];
  } catch (err) {
    console.warn(`[odbc] could not read ${manifest}: ${(err as Error).message}`);
    return;
  }

  for (const name of names) {
    const fromList = reg(['delete', WIN_ODBC_DRIVERS, '/v', name, '/f']);
    const key = reg(['delete', `${WIN_ODBCINST}\\${name}`, '/f']);
    if (fromList.status !== 0 || key.status !== 0) {
      console.warn(
        `[odbc] could not fully unregister "${name}" (list exit ${fromList.status}, key exit ${key.status}); ` +
          'it may be left pointing at a removed sandbox path',
      );
    } else {
      console.log(`[odbc] unregistered "${name}"`);
    }
  }
}

/**
 * Build <sandbox>/odbc: an odbcinst.ini registering each available target's
 * driver, an empty odbc.ini (no DSNs; the wizard's driver path is what we
 * test), and per target a directory holding a symlink to the real driver
 * library plus a snippets/<driver>.R file. The snippet is what gives the
 * wizard labeled individual fields; the session finds it by walking up from
 * the driver path (.rs.connectionReadOdbcEntry), which is why the ini points
 * at the symlink rather than the real library location.
 */
export function prepareOdbcSandbox(sandbox: string): OdbcSandbox {
  return process.platform === 'win32'
    ? prepareOdbcSandboxWindows(sandbox)
    : prepareOdbcSandboxUnix(sandbox);
}

function prepareOdbcSandboxUnix(sandbox: string): OdbcSandbox {
  const odbcDir = path.join(sandbox, 'odbc');
  const stanzas: string[] = [];
  const registered: string[] = [];

  for (const base of ALL_DB_TARGETS) {
    const target = effectiveTarget(base);
    const library = resolveDriverLibrary(target);
    if (!library) continue;

    const driverDir = path.join(odbcDir, 'drivers', target.id);
    const linkPath = path.join(driverDir, path.basename(library));
    fs.mkdirSync(path.join(driverDir, 'snippets'), { recursive: true });
    fs.symlinkSync(library, linkPath);
    fs.writeFileSync(
      path.join(driverDir, 'snippets', snippetFileName(target.driverName)),
      wizardSnippet(target),
    );

    stanzas.push(...odbcinstStanza(target.driverName, target.id, linkPath));
    registered.push(target.id);
  }

  // driverPaths is Windows-only: the unixODBC ini points at a symlink to the
  // real library, and the dynamic loader follows it to resolve dependencies.
  if (registered.length === 0) return { odbcDir: null, registered, driverPaths: [] };

  fs.writeFileSync(path.join(odbcDir, 'odbcinst.ini'), stanzas.join('\n'));
  fs.writeFileSync(path.join(odbcDir, 'odbc.ini'), '');
  makeOdbcSandboxWorldReadable(sandbox, odbcDir);
  return { odbcDir, registered, driverPaths: [] };
}

/**
 * A real installed RStudio Server (CI's systemd-managed service, or a
 * genuinely external server) often runs its rsessions as a different OS user
 * than the one that built this sandbox -- fs.mkdtempSync creates the sandbox
 * root 0700, which would otherwise block that user from reading ODBCSYSINI
 * even after it's set. Grant just enough: +x (traverse only, not +r) on the
 * sandbox root itself, so a user who knows this exact path can reach it but
 * still can't list the root and browse into unrelated sandbox directories
 * (credential stores, etc.); then the whole odbc subtree world-readable.
 * None of this content is a secret, unlike the credential stores.
 */
function makeOdbcSandboxWorldReadable(sandbox: string, odbcDir: string): void {
  fs.chmodSync(sandbox, fs.statSync(sandbox).mode | fs.constants.S_IXOTH);
  chmodWorldReadableRecursive(odbcDir);
}

/**
 * Directories world-readable+traversable, regular files world-readable.
 * Skips symlinks: chmod has no lchmod on Linux and would follow the link,
 * changing the mode of the vendor-installed driver library itself rather
 * than the sandbox's copy -- that file's permissions are the package
 * manager's concern, and it is already world-readable as installed.
 */
function chmodWorldReadableRecursive(dir: string): void {
  fs.chmodSync(dir, 0o755);
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isSymbolicLink()) continue;
    if (entry.isDirectory()) {
      chmodWorldReadableRecursive(full);
    } else {
      fs.chmodSync(full, 0o644);
    }
  }
}

/**
 * The [name]/Description/Driver/blank stanza written into odbcinst.ini.
 * Shared by the local (prepareOdbcSandboxUnix) and remote
 * (provisionRemoteOdbcSandbox) builders so the two text formats cannot drift
 * apart.
 */
function odbcinstStanza(driverName: string, id: string, linkPath: string): string[] {
  return [
    `[${driverName}]`,
    `Description = ${id} driver registered by the Playwright sandbox`,
    `Driver = ${linkPath}`,
    '',
  ];
}

/**
 * Home-relative directory a genuinely external RStudio Server's ODBC
 * sandbox is built under (see provisionRemoteOdbcSandbox). Namespaced like
 * the local machine's `.cache/rstudio-playwright/...` convention
 * (fixtures/r-libs-setup.ts), distinct from the real app-specific paths
 * remote-provision.ts pushes credentials to (`~/.config/github-copilot`,
 * etc.) since this is a harness-owned sandbox, not real application state.
 */
export const REMOTE_ODBC_DIR = '~/.rstudio-playwright/odbc';

/**
 * Unix-only analogue of resolveDriverLibrary (db-targets.ts), but checked on
 * the REMOTE machine via an in-session file.exists() probe
 * (remotePathExists, utils/remote-provision.ts) rather than Node's local
 * fs.existsSync -- there is no SSH access to a genuinely external server,
 * only the session itself. Always tests the `linux` candidates regardless
 * of the test runner's own OS: RStudio Server only ships for Linux, so the
 * runner's platform is irrelevant to what the remote machine has installed.
 *
 * Windows resolves this differently (winInstalledDriverLibrary reads the
 * vendor installer's own HKLM registration) because there is a real
 * registration to read back. Unix ODBC packages have no equivalent: they
 * just drop a .so at a known path and never self-register in odbcinst.ini,
 * so the curated candidate list -- kept in sync with
 * scripts/db/install-deps/linux.sh -- is the only source of truth, local or
 * remote. An operator can run that script directly against the external
 * machine as a prerequisite; it has no CI-specific assumptions.
 */
async function resolveDriverLibraryRemote(page: Page, target: DbTarget): Promise<string | null> {
  for (const candidate of target.driverLibraries.linux ?? []) {
    if ((await remotePathExists(page, candidate)) === true) return candidate;
  }
  return null;
}

/** What provisionRemoteOdbcSandbox found and built. */
export interface RemoteOdbcStatus {
  /**
   * Value to Sys.setenv(ODBCSYSINI = ...) in the actual test session, or
   * null when no target's driver could be found on the remote machine.
   * Left as the "~/"-prefixed REMOTE_ODBC_DIR rather than an expanded
   * absolute path -- the caller wraps it in path.expand() when applying it,
   * so expansion happens in whichever session actually uses the value
   * rather than requiring a round trip back through this one to resolve it.
   */
  odbcSysIni: string | null;
  /** Ids of the targets whose drivers were found and registered. */
  registered: string[];
}

function remoteOdbcStatusPath(sandbox: string): string {
  return path.join(sandbox, 'remote-odbc-status.json');
}

/**
 * Persist what provisionRemoteOdbcSandbox found. The step that runs it
 * (tests/auth.setup.ts) and the fixture that later applies ODBCSYSINI
 * (fixtures/rstudio.fixture.ts) are separate worker processes -- setting
 * process.env here would not cross that boundary, the same reason AI
 * credential outcomes cross it via a status file (readAuthStatus /
 * writeAuthStatus in utils/auth.ts) rather than an environment variable.
 */
export function writeRemoteOdbcStatus(sandbox: string, status: RemoteOdbcStatus): void {
  fs.writeFileSync(remoteOdbcStatusPath(sandbox), JSON.stringify(status, null, 2));
}

/**
 * Returns null when the file is absent, unreadable, or malformed -- callers
 * treat that the same as "nothing was provisioned", since the file only
 * exists once provisionRemoteOdbcSandbox has actually run.
 */
export function readRemoteOdbcStatus(sandbox: string): RemoteOdbcStatus | null {
  let raw: string;
  try {
    raw = fs.readFileSync(remoteOdbcStatusPath(sandbox), 'utf-8');
  } catch {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<RemoteOdbcStatus>;
    if (!Array.isArray(parsed.registered)) return null;
    return {
      odbcSysIni: typeof parsed.odbcSysIni === 'string' ? parsed.odbcSysIni : null,
      registered: parsed.registered,
    };
  } catch {
    return null;
  }
}

/**
 * Build the same odbcinst.ini / odbc.ini / per-driver snippets.R content
 * prepareOdbcSandboxUnix builds locally, but for a genuinely external
 * RStudio Server: driver discovery and every write happen in-session
 * (resolveDriverLibraryRemote, writeRemoteText, a console-run
 * file.symlink()), never through Node's local filesystem. Only paths ever
 * cross the console -- never file content -- matching the rule
 * utils/remote-provision.ts establishes for credential pushes; lower risk
 * here since none of this content is a secret.
 *
 * `recordPath` is called once, immediately, with REMOTE_ODBC_DIR itself --
 * before anything is created under it -- so a run killed partway through
 * still leaves the caller's manifest naming the whole subtree to scrub
 * (recursively; every file this writes lives under that one directory, so
 * one entry covers all of them, unlike the credential push's scattered
 * store paths). It is also passed as writeRemoteText's temp-file recorder,
 * so a stray upload temp file outside that directory still gets tracked.
 *
 * Never throws on a single target's discovery/write failure -- logs a
 * warning and moves on, so that target's specs skip with an accurate
 * reason exactly like the local-machine path today, and one target's bad
 * luck cannot strand the others.
 */
export async function provisionRemoteOdbcSandbox(
  page: Page,
  recordPath: (remotePath: string) => void,
): Promise<RemoteOdbcStatus> {
  recordPath(REMOTE_ODBC_DIR);

  const stanzas: string[] = [];
  const registered: string[] = [];

  for (const base of ALL_DB_TARGETS) {
    const target = effectiveTarget(base);
    try {
      const library = await resolveDriverLibraryRemote(page, target);
      if (!library) {
        console.log(`[connections] ${target.id}: no driver library found on the remote machine; its specs will skip`);
        continue;
      }

      const driverDir = `${REMOTE_ODBC_DIR}/drivers/${target.id}`;
      const linkPath = `${driverDir}/${library.split('/').pop()}`;
      await executeInConsole(
        page,
        `dir.create(path.expand(${rStringLiteral(driverDir)}), recursive = TRUE, showWarnings = FALSE); `
          + `file.symlink(${rStringLiteral(library)}, path.expand(${rStringLiteral(linkPath)}))`,
      );

      const snippetPath = `${driverDir}/snippets/${snippetFileName(target.driverName)}`;
      await writeRemoteText(page, snippetPath, wizardSnippet(target), '0644', recordPath);

      stanzas.push(...odbcinstStanza(target.driverName, target.id, linkPath));
      registered.push(target.id);
    } catch (err) {
      console.warn(
        `[connections] ${target.id}: remote ODBC provisioning failed, its specs will skip:`,
        err instanceof Error ? err.message : String(err),
      );
    }
  }

  if (registered.length === 0) return { odbcSysIni: null, registered: [] };

  await writeRemoteText(page, `${REMOTE_ODBC_DIR}/odbcinst.ini`, stanzas.join('\n'), '0644', recordPath);
  await writeRemoteText(page, `${REMOTE_ODBC_DIR}/odbc.ini`, '', '0644', recordPath);

  return { odbcSysIni: REMOTE_ODBC_DIR, registered };
}

/**
 * Record one more successfully-registered driver name in the sandbox
 * manifest, read-modify-write so each success is flushed to disk as it
 * happens rather than batched until the whole loop finishes. That matters
 * because `winRegisterDriver` just made a real HKLM write for this target:
 * if a later target in the same loop throws (a locked DLL on
 * `fs.copyFileSync`, say), this target's registration must still be in the
 * manifest so `unregisterWindowsOdbcDrivers` can undo it on the next
 * teardown -- a manifest written only after the full loop completes would
 * otherwise lose every earlier success along with the one that threw.
 */
function appendWindowsManifestEntry(odbcDir: string, name: string): void {
  const manifestPath = path.join(odbcDir, WIN_MANIFEST);
  let names: string[] = [];
  if (fs.existsSync(manifestPath)) {
    try {
      names = JSON.parse(fs.readFileSync(manifestPath, 'utf8')) as string[];
    } catch (err) {
      console.warn(`[odbc] could not read existing ${manifestPath}, starting fresh: ${(err as Error).message}`);
      names = [];
    }
  }
  names.push(name);
  fs.writeFileSync(manifestPath, JSON.stringify(names, null, 2));
}

/**
 * The Windows equivalent, which has to work differently in two ways.
 *
 * There is no ODBCSYSINI, so drivers are registered machine-wide in HKLM (the
 * only hive RStudio reads: SessionConnections.R calls readRegistry without a
 * hive argument, and R defaults to HKEY_LOCAL_MACHINE). That means real
 * machine state, removed again by unregisterWindowsOdbcDrivers at teardown.
 *
 * And the driver DLL is copied into the sandbox. That is needed because the
 * wizard's labeled fields come from a snippets/<driver>.R which RStudio finds
 * by walking up from the *registered* DLL path, so that path has to be inside
 * a directory we own.
 *
 * Only the DLL is copied, never its directory. An earlier version copied the
 * parent directory so that a driver's bundled dependencies came along with it,
 * on the assumption that every driver has a directory of its own. sqliteodbc
 * disproves that: it installs into C:\Windows\system32, so the copy walked
 * into the entire Windows system directory (it died on a locked file in
 * catroot2 after a few seconds, having otherwise been prepared to copy
 * gigabytes). Dependencies are handled instead by putting the DLL's original
 * directory on the session's PATH, which is uniform across drivers and cannot
 * run away: psqlODBC's bundled libpq and OpenSSL resolve from its versioned
 * bin directory, and a driver already living in a system directory needs
 * nothing added at all.
 */
function prepareOdbcSandboxWindows(sandbox: string): OdbcSandbox {
  const odbcDir = path.join(sandbox, 'odbc');
  const registered: string[] = [];
  const driverPaths: string[] = [];

  for (const base of ALL_DB_TARGETS) {
    const target = effectiveTarget(base);
    const library = winInstalledDriverLibrary(target);
    if (!library) continue;

    const driverDir = path.join(odbcDir, 'drivers', target.id);
    const copiedDll = path.join(driverDir, path.basename(library));
    fs.mkdirSync(path.join(driverDir, 'snippets'), { recursive: true });
    fs.copyFileSync(library, copiedDll);
    fs.writeFileSync(
      path.join(driverDir, 'snippets', snippetFileName(target.driverName)),
      wizardSnippet(target),
    );

    const problem = winRegisterDriver(target.driverName, copiedDll);
    if (problem) {
      console.warn(`[odbc] ${target.id}: not registered -- ${problem}`);
      continue;
    }
    registered.push(target.id);
    appendWindowsManifestEntry(odbcDir, target.driverName);

    // A driver installed in a system directory needs nothing on PATH: that
    // directory is already searched, and adding it would be noise.
    const origin = path.dirname(library);
    if (!isWindowsSystemDir(origin) && !driverPaths.includes(origin)) {
      driverPaths.push(origin);
    }
  }

  // The manifest is flushed incrementally inside the loop
  // (appendWindowsManifestEntry), not batched here, so a run that throws
  // partway through still leaves teardown a manifest naming every target
  // that got as far as a real HKLM registration.

  // odbcDir stays null: nothing on Windows consumes ODBCSYSINI.
  return { odbcDir: null, registered, driverPaths };
}

/**
 * TypeScript port of ElementIds.idSafeString (ElementIds.java): "C++"
 * becomes "CPP", every other non-alphanumeric run collapses to one "_",
 * leading/trailing "_" stripped, lowercased.
 */
export function idSafeString(label: string): string {
  return label
    .replace(/C\+\+/g, 'CPP')
    .replace(/[^a-zA-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase();
}

/**
 * The id of a connection type's entry in the New Connection wizard list
 * (NewConnectionNavigationPage assigns it with a raw setId, so there is no
 * uniqueness suffix to allow for).
 */
export function wizardPageId(connectionName: string): string {
  return `#rstudio_label_${idSafeString(connectionName)}_wizard_page`;
}

/**
 * Availability of a target's database for skip decisions, from the status
 * file globalSetup wrote. Skips must name the actual problem; a missing
 * status file means provisioning never ran (e.g. spec executed outside the
 * suite harness).
 */
export function dbAvailability(target: DbTarget): { ok: boolean; reason: string } {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) return { ok: false, reason: 'PW_SANDBOX not set (run through the suite harness)' };
  const status = readDbStatus(sandbox)[target.id];
  if (!status) return { ok: false, reason: `no provisioning status recorded for ${target.id}` };
  return { ok: status.ok, reason: `${target.id}: ${status.outcome}, ${status.detail}` };
}

/**
 * Drain the session's recorded client exceptions, returning any that are NOT
 * the known connections-pane bug: re-exploring a connection that was
 * disconnected earlier in the same session raises "Cannot read properties of
 * null (reading 'object_types')" from ObjectBrowserModel.isLeaf, which
 * null-guards the tree node but not connection_.getObjectTypes(). Specs that
 * connect call this in afterEach and assert the result is empty: the known
 * bug is excused, anything else still fails, and the per-test fixture's own
 * drain then finds nothing to re-raise.
 */
export async function drainKnownExplorerException(page: Page): Promise<string[]> {
  const known = /reading 'object_types'/;
  const exceptions = await drainClientExceptions(page);
  return exceptions.filter((e) => !known.test(e.message)).map((e) => e.message);
}

/** R that yields the names of every DBIConnection bound in globalenv. */
const LIVE_CONNECTIONS_R =
  'Filter(function(nm) isTRUE(try(methods::is(get(nm, envir = globalenv()), ' +
  '"DBIConnection"), silent = TRUE)), ls(envir = globalenv()))';

/**
 * Clear all connection state for a target: close every live connection,
 * drop the `con` binding the wizard's R Console destination creates, and
 * remove the target's entry from the pane's connection history.
 *
 * Specs share one RStudio session, so without this each test inherits the
 * previous one's connections and history entries, making anything that reads
 * the pane's list order-dependent. Driven through the session's own RPCs
 * rather than the UI: the list re-renders on every connection event, which
 * detaches rows mid-click.
 *
 * Callers restart R immediately after this, so closing comes first and is
 * verified: restarting with a connection still open orphans a session on the
 * database server, and rm()ing the binding does not close anything. The
 * sweep covers connections bound under any name, since one session can hold
 * several.
 *
 * Each step is checked separately so a failure says which one broke rather
 * than leaving the caller to guess.
 */
export async function resetConnectionState(page: Page, target: DbTarget): Promise<void> {
  const t = effectiveTarget(target);
  const type = rStringLiteral(t.connectionType);
  const host = rStringLiteral(connectionHostId(t));
  const console_ = new ConsolePaneActions(page);
  const where = `type=${t.connectionType} host=${connectionHostId(t)}`;

  // 1. Close and unbind every live connection, then confirm none survives.
  const closed = await console_.evalRLogical(
    'local({ ' +
      `for (nm in ${LIVE_CONNECTIONS_R}) { ` +
      '  try(DBI::dbDisconnect(get(nm, envir = globalenv())), silent = TRUE); ' +
      '  try(rm(list = nm, envir = globalenv()), silent = TRUE); ' +
      // The semicolon is required: R does not accept a new expression
      // directly after a block's closing brace on the same line.
      '}; ' +
      `length(${LIVE_CONNECTIONS_R}) == 0 })`,
  );
  if (closed !== true) {
    throw new Error(
      `resetConnectionState (${where}): a live DBI connection survived the sweep, so ` +
        'restarting R now would orphan a session on the database server ' +
        `(probe returned ${closed})`,
    );
  }

  // 2. Tell the pane the connection is gone. Expected to fail when nothing
  // is connected (the RPC also wants finder/disconnectCode, which only a
  // live connection has), so this one is best-effort.
  await console_.evalRLogical(
    `!inherits(try(.rs.invokeRpc("connection_disconnect", list(type = ${type}, ` +
      `host = ${host})), silent = TRUE), "try-error")`,
  );

  // 3. Drop the history entry. Identity is (type, host) and history is keyed
  // by it, so one removal clears it outright -- there are never duplicates.
  // A silent failure here leaves the entry in place and every list-reading
  // test order-dependent again, so it is checked.
  const removed = await console_.evalRLogical(
    `!inherits(try(.rs.invokeRpc("remove_connection", list(type = ${type}, ` +
      `host = ${host})), silent = TRUE), "try-error")`,
  );
  if (removed !== true) {
    throw new Error(
      `resetConnectionState (${where}): the remove_connection RPC errored, so the pane ` +
        `still lists this connection (probe returned ${removed})`,
    );
  }
}

/** Whether the session under test sees the target's ODBC driver. */
export async function driverVisibleInSession(page: Page, target: DbTarget): Promise<boolean> {
  const console_ = new ConsolePaneActions(page);
  const result = await console_.evalRLogical(
    `${rStringLiteral(target.driverName)} %in% odbc::odbcListDrivers()$name`,
  );
  return result === true;
}

/**
 * Whether the session under test can actually reach the database.
 *
 * For a server target that means opening a TCP connection. For a file target
 * there is no socket to open, so the equivalent question is whether the
 * session can create the database file: the directory has to exist and be
 * writable by the rsession, which in remote Server mode is a different user
 * on a different filesystem than the test runner. Checked in-session for
 * exactly that reason.
 */
export async function dbReachableFromSession(page: Page, target: DbTarget): Promise<boolean> {
  const t = effectiveTarget(target);
  const console_ = new ConsolePaneActions(page);

  if (t.kind === 'file') {
    if (!t.database) return false;
    // file.access mode 2 is write permission; dirname because the file itself
    // does not exist until the driver creates it on first connect.
    const expr =
      `local({ d <- dirname(${rStringLiteral(t.database)}); ` +
      'dir.exists(d) && file.access(d, 2) == 0 })';
    return (await console_.evalRLogical(expr)) === true;
  }

  const expr =
    `local({ s <- try(suppressWarnings(socketConnection(${rStringLiteral(t.host)}, ` +
    `${t.port}, open = "r+", timeout = 5)), silent = TRUE); ` +
    `ok <- !inherits(s, "try-error"); if (ok) close(s); ok })`;
  const result = await console_.evalRLogical(expr);
  return result === true;
}
