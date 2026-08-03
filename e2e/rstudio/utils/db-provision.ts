/**
 * Suite-side provisioning of throwaway database servers for the Connections
 * pane tests. Invoked from globalSetup (fixtures/sandbox-setup.ts);
 * globalTeardown stops whatever this started.
 *
 * Per target the outcome is one of:
 *   - override:   PW_DB_<ID> is set, so an external server is in charge
 *   - external:   something is already listening on the target port (reuse)
 *   - provisioned: our script started a throwaway server inside the sandbox
 *   - failed:     no script for this platform, or the script errored
 *
 * A failure is deliberately not fatal to the run: suites unrelated to the
 * Connections pane must not die because a runner lacks a database toolchain.
 * The outcome (with the script's output) lands in <sandbox>/db/status.json,
 * and the connections specs read it to skip with a reason that names the
 * actual problem. A timeout in a spec still fails: only a recorded
 * provisioning failure downgrades to a skip.
 */

import { spawnSync } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import { ALL_DB_TARGETS, effectiveTarget } from './db-targets';
import { isTcpReachable } from './network';

export interface DbStatus {
  ok: boolean;
  /** 'override' | 'external' | 'provisioned' | 'failed' */
  outcome: string;
  /** Human-readable detail; for failures, includes the script output. */
  detail: string;
  /** Set only when outcome is 'provisioned': what teardown must stop. */
  script?: string;
  dataDir?: string;
}

export type DbStatusFile = Record<string, DbStatus>;

const SCRIPT_PLATFORMS: Partial<Record<NodeJS.Platform, string>> = {
  darwin: 'macos.sh',
  // linux: 'linux.sh' arrives with the CI-enablement phase.
};

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
    const run = spawnSync('bash', [script, 'start', dataDir], {
      encoding: 'utf8',
      timeout: 120_000,
      env: {
        ...process.env,
        PW_DBP_PORT: String(target.port),
        PW_DBP_DATABASE: target.database,
        PW_DBP_USER: target.user,
        PW_DBP_PASSWORD: target.password,
      },
    });
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
 * Stop every server the suite provisioned. Runs unconditionally in
 * globalTeardown, before (and independent of) sandbox file removal: a
 * preserved sandbox keeps its data directory for inspection, but never a
 * running server process.
 */
export function stopProvisionedDatabases(sandbox: string): void {
  const status = readDbStatus(sandbox);
  for (const [id, s] of Object.entries(status)) {
    if (s.outcome !== 'provisioned' || !s.script || !s.dataDir) continue;
    // Some engines' stop paths need the connection parameters too (MySQL
    // shuts down over TCP), so pass the same environment as start.
    const base = ALL_DB_TARGETS.find((t) => t.id === id);
    const target = base ? effectiveTarget(base) : null;
    const run = spawnSync('bash', [s.script, 'stop', s.dataDir], {
      encoding: 'utf8',
      timeout: 30_000,
      env: target
        ? {
            ...process.env,
            PW_DBP_PORT: String(target.port),
            PW_DBP_DATABASE: target.database,
            PW_DBP_USER: target.user,
            PW_DBP_PASSWORD: target.password,
          }
        : process.env,
    });
    if (run.status === 0) {
      console.log(`[db] ${id}: stopped`);
    } else {
      console.warn(
        `[db] ${id}: stop script exited ${run.status}; a server process may remain:\n${`${run.stdout ?? ''}${run.stderr ?? ''}`.trim()}`,
      );
    }
  }
}
