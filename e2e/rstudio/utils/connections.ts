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
  /** Value for ODBCSYSINI, or null when nothing could be registered. */
  odbcDir: string | null;
  /** Ids of the targets whose drivers were found and registered. */
  registered: string[];
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
  // Windows has its own registry-based driver manager and no ODBCSYSINI;
  // handled in the CI-enablement phase.
  if (process.platform === 'win32') return { odbcDir: null, registered: [] };

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

  if (registered.length === 0) return { odbcDir: null, registered };

  fs.writeFileSync(path.join(odbcDir, 'odbcinst.ini'), stanzas.join('\n'));
  fs.writeFileSync(path.join(odbcDir, 'odbc.ini'), '');
  return { odbcDir, registered };
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

/** Whether the session under test can open a TCP connection to the database. */
export async function dbReachableFromSession(page: Page, target: DbTarget): Promise<boolean> {
  const t = effectiveTarget(target);
  const console_ = new ConsolePaneActions(page);
  const expr =
    `local({ s <- try(suppressWarnings(socketConnection(${rStringLiteral(t.host)}, ` +
    `${t.port}, open = "r+", timeout = 5)), silent = TRUE); ` +
    `ok <- !inherits(s, "try-error"); if (ok) close(s); ok })`;
  const result = await console_.evalRLogical(expr);
  return result === true;
}
