/**
 * Suite-side provisioning of throwaway database servers for the Connections
 * pane tests. Invoked from globalSetup (fixtures/sandbox-setup.ts);
 * globalTeardown stops whatever this started.
 *
 * Per target the outcome is one of:
 *   - override:   PW_DB_<ID> is set, so an external server is in charge
 *   - external:   something is already listening on the target port (reuse)
 *   - provisioned: our script started a throwaway server inside the sandbox
 *   - file:       an embedded engine with no server at all (SQLite); the
 *                 driver opens the database file directly
 *   - failed:     no script for this platform, or the script errored
 *
 * A failure is deliberately not fatal to the run: suites unrelated to the
 * Connections pane must not die because a runner lacks a database toolchain.
 * The outcome (with the script's output) lands in <sandbox>/db/status.json,
 * and the connections specs read it to skip with a reason that names the
 * actual problem. A timeout in a spec still fails: only a recorded
 * provisioning failure downgrades to a skip.
 */

import { spawnSync, SpawnSyncReturns } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import { ALL_DB_TARGETS, effectiveTarget } from './db-targets';
import { isTcpReachable } from './network';

/** See the module doc above for what each outcome means. */
export type DbStatusOutcome = 'override' | 'external' | 'provisioned' | 'file' | 'failed';

export interface DbStatus {
  ok: boolean;
  outcome: DbStatusOutcome;
  /** Human-readable detail; for failures, includes the script output. */
  detail: string;
  /** Set only when outcome is 'provisioned': what teardown must stop. */
  script?: string;
  dataDir?: string;
}

export type DbStatusFile = Record<string, DbStatus>;

const SCRIPT_PLATFORMS: Partial<Record<NodeJS.Platform, string>> = {
  darwin: 'macos.sh',
  win32: 'windows.ps1',
  linux: 'linux.sh',
};

/**
 * Run one action of a provisioning script.
 *
 * The interpreter follows the script's extension rather than the platform, so
 * a .sh and a .ps1 can coexist and a future platform needs no change here.
 * Windows ships no bash, and PowerShell needs its own launcher:
 *
 *   -ExecutionPolicy Bypass  a developer machine may still be Restricted,
 *                            which would refuse to run the file at all
 *   powershell.exe not pwsh  matches fixtures/desktop.fixture.ts, so the
 *                            suite depends on one Windows shell, not two
 */
function runDbScript(
  script: string,
  action: string,
  dataDir: string,
  env: NodeJS.ProcessEnv,
  timeout: number,
): SpawnSyncReturns<string> {
  const options = { encoding: 'utf8' as const, timeout, env };
  return script.endsWith('.ps1')
    ? spawnSync(
        'powershell.exe',
        ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script, action, dataDir],
        options,
      )
    : spawnSync('bash', [script, action, dataDir], options);
}

function statusPath(sandbox: string): string {
  return path.join(sandbox, 'db', 'status.json');
}

export function readDbStatus(sandbox: string): DbStatusFile {
  const p = statusPath(sandbox);
  if (!fs.existsSync(p)) return {};
  return JSON.parse(fs.readFileSync(p, 'utf8')) as DbStatusFile;
}

/** Start (or adopt) a database server for every known target. */
export async function provisionDatabases(sandbox: string): Promise<DbStatusFile> {
  const status: DbStatusFile = {};

  for (const base of ALL_DB_TARGETS) {
    const target = effectiveTarget(base);

    // A file engine has no server: its driver opens the database file itself,
    // creating it on first connect. All that is owed here is the directory to
    // put it in, since the driver will not create parent directories. Nothing
    // is started, so nothing is stopped at teardown either.
    if (target.kind === 'file') {
      if (!target.database) {
        status[target.id] = {
          ok: false,
          outcome: 'failed',
          detail: `no database file path resolved for ${target.id} (PW_SANDBOX not set?)`,
        };
        continue;
      }
      fs.mkdirSync(path.dirname(target.database), { recursive: true });
      status[target.id] = {
        ok: true,
        outcome: target.overridden ? 'override' : 'file',
        detail: `${target.id} uses the database file ${target.database} (no server)`,
      };
      console.log(`[db] ${target.id}: file-backed at ${target.database}`);
      continue;
    }

    if (target.overridden) {
      status[target.id] = {
        ok: true,
        outcome: 'override',
        detail: `PW_DB_${target.id.toUpperCase()} points at ${target.host}:${target.port}`,
      };
      continue;
    }

    // Reuse anything already listening on the target port: a developer's
    // still-running cluster from an inspected sandbox, or a CI service. If
    // the listener is not actually ours, the probe spec fails with the
    // driver's own auth/connect error, which names the port.
    if (await isTcpReachable(target.host, target.port)) {
      status[target.id] = {
        ok: true,
        outcome: 'external',
        detail: `${target.host}:${target.port} already listening; reusing`,
      };
      continue;
    }

    const scriptName = SCRIPT_PLATFORMS[process.platform];
    const script = scriptName
      ? path.join(__dirname, '..', 'scripts', 'db', target.id, scriptName)
      : null;
    if (!script || !fs.existsSync(script)) {
      status[target.id] = {
        ok: false,
        outcome: 'failed',
        detail: `no ${target.id} provisioning for platform "${process.platform}"`,
      };
      continue;
    }

    const dataDir = path.join(sandbox, 'db', target.id);
    const t0 = Date.now();
    const run = runDbScript(script, 'start', dataDir, {
      ...process.env,
      PW_DBP_PORT: String(target.port),
      PW_DBP_DATABASE: target.database,
      PW_DBP_USER: target.user,
      PW_DBP_PASSWORD: target.password,
    }, 120_000);
    const output = `${run.stdout ?? ''}${run.stderr ?? ''}`.trim();

    if (run.status === 0) {
      status[target.id] = {
        ok: true,
        outcome: 'provisioned',
        detail: `throwaway server started in ${Date.now() - t0}ms`,
        script,
        dataDir,
      };
      console.log(`[db] ${target.id}: provisioned on ${target.host}:${target.port} (${Date.now() - t0}ms)`);
    } else {
      status[target.id] = {
        ok: false,
        outcome: 'failed',
        detail: `provisioning script exited ${run.status}:\n${output}`,
      };
      console.warn(`[db] ${target.id}: provisioning FAILED (specs will skip):\n${output}`);
    }
  }

  fs.mkdirSync(path.dirname(statusPath(sandbox)), { recursive: true });
  fs.writeFileSync(statusPath(sandbox), JSON.stringify(status, null, 2));
  return status;
}

/**
 * Ask the engine how many client connections to the test database are still
 * open, or null when the probe could not answer (no engine support, server
 * already gone, unparseable output). Called with the IDE shut down, so a
 * non-zero answer means the tests left a session behind on the server.
 *
 * A nonzero exit is warned about, not just folded into `null`: unlike "this
 * engine doesn't support the probe" (a normal, silent case for some
 * targets), a script that used to work and now exits nonzero means the
 * leak-detection safety net this function exists for has gone dark, and
 * that should show up in the log rather than look identical to "nothing to
 * report."
 */
function countOpenSessions(
  id: string,
  script: string,
  dataDir: string,
  env: NodeJS.ProcessEnv,
): number | null {
  const run = runDbScript(script, 'sessions', dataDir, env, 30_000);
  if (run.status !== 0) {
    console.warn(
      `[db] ${id}: sessions probe exited ${run.status}, could not check for leaked connections:\n` +
        `${`${run.stdout ?? ''}${run.stderr ?? ''}`.trim()}`,
    );
    return null;
  }
  const count = Number((run.stdout ?? '').trim());
  return Number.isInteger(count) ? count : null;
}

/**
 * Stop every server the suite provisioned. Runs unconditionally in
 * globalTeardown, before (and independent of) sandbox file removal: a
 * preserved sandbox keeps its data directory for inspection, but never a
 * running server process.
 *
 * Each server is asked for its open-connection count first. Nothing should
 * be attached by now: the specs disconnect what they connect, and
 * resetConnectionState closes every DBI connection before restarting R. A
 * leftover session means one of those paths let a connection escape --
 * invisible on a throwaway server about to be deleted, but the same mistake
 * against a shared database leaves hung sessions behind. Warn rather than
 * fail: teardown must still stop the server.
 */
export function stopProvisionedDatabases(sandbox: string): void {
  const status = readDbStatus(sandbox);
  for (const [id, s] of Object.entries(status)) {
    if (s.outcome !== 'provisioned' || !s.script || !s.dataDir) continue;
    // Some engines need the connection parameters after start too (MySQL
    // shuts down over TCP, and both engines' session probes connect), so
    // pass the same environment as start.
    const base = ALL_DB_TARGETS.find((t) => t.id === id);
    const found = base ? effectiveTarget(base) : null;
    // Only a server target ever reaches here (a file target is never
    // 'provisioned'), but narrow explicitly rather than leaning on that:
    // the connection parameters below exist only on the server variant.
    const target = found && found.kind === 'server' ? found : null;
    const env: NodeJS.ProcessEnv = target
      ? {
          ...process.env,
          PW_DBP_PORT: String(target.port),
          PW_DBP_DATABASE: target.database,
          PW_DBP_USER: target.user,
          PW_DBP_PASSWORD: target.password,
        }
      : process.env;

    const open = countOpenSessions(id, s.script, s.dataDir, env);
    if (open !== null && open > 0) {
      console.warn(
        `[db] ${id}: ${open} connection(s) to ${target?.database ?? id} still open at teardown; ` +
          'a test left a database session behind (close connections before restarting R)',
      );
    }

    const run = runDbScript(s.script, 'stop', s.dataDir, env, 30_000);
    if (run.status === 0) {
      console.log(`[db] ${id}: stopped${open === null ? '' : ` (${open} session(s) left open)`}`);
    } else {
      console.warn(
        `[db] ${id}: stop script exited ${run.status}; a server process may remain:\n${`${run.stdout ?? ''}${run.stderr ?? ''}`.trim()}`,
      );
    }
  }
}
