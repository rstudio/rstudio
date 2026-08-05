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
  for (const value of ['Driver', 'Setup']) {
    const add = reg(['add', key, '/v', value, '/t', 'REG_SZ', '/d', dllPath, '/f']);
    if (add.status !== 0) {
      return `reg add ${key} /v ${value} failed (exit ${add.status}); an HKLM write needs an elevated shell`;
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

    stanzas.push(
      `[${target.driverName}]`,
      `Description = ${target.id} driver registered by the Playwright sandbox`,
      `Driver = ${linkPath}`,
      '',
    );
    registered.push(target.id);
  }

  // driverPaths is Windows-only: the unixODBC ini points at a symlink to the
  // real library, and the dynamic loader follows it to resolve dependencies.
  if (registered.length === 0) return { odbcDir: null, registered, driverPaths: [] };

  fs.writeFileSync(path.join(odbcDir, 'odbcinst.ini'), stanzas.join('\n'));
  fs.writeFileSync(path.join(odbcDir, 'odbc.ini'), '');
  return { odbcDir, registered, driverPaths: [] };
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
  const registeredNames: string[] = [];
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
    registeredNames.push(target.driverName);

    // A driver installed in a system directory needs nothing on PATH: that
    // directory is already searched, and adding it would be noise.
    const origin = path.dirname(library);
    if (!isWindowsSystemDir(origin) && !driverPaths.includes(origin)) {
      driverPaths.push(origin);
    }
  }

  // Written even when empty is pointless, but written before returning so a
  // partially-successful run still records what teardown must undo.
  if (registeredNames.length > 0) {
    fs.mkdirSync(odbcDir, { recursive: true });
    fs.writeFileSync(path.join(odbcDir, WIN_MANIFEST), JSON.stringify(registeredNames, null, 2));
  }

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
