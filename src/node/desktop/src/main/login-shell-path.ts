/*
 * login-shell-path.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { spawn } from 'child_process';
import ElectronStore from 'electron-store';
import { getenv } from '../core/environment';
import { logger } from '../core/logger';

// On macOS an app launched from the Finder or Dock inherits launchd's minimal
// PATH, so rsession asks the user's login shell for the real one at startup
// (modules::path::initializePath in SessionPath.cpp). Depending on the
// profile that takes anywhere from tens of milliseconds to over a second,
// all of it on the session's critical path, on every launch.
//
// The desktop asks the shell instead, as early as it can, and hands the
// answer to rsession through its environment (marked so the session skips
// its own query). Which answer depends on how quickly the shell replies:
//
//   - if it has replied by the time the session is launched, its answer;
//   - otherwise the previous launch's answer, remembered in the state store,
//     with the fresh reply kept for next time (a changed profile therefore
//     applies from the following launch on machines with a slow profile);
//   - on a first launch with nothing remembered, the launch waits.

/** Marker telling rsession that PATH already came from a login shell. */
export const kSessionPathInitializedEnvVar = 'RSTUDIO_SESSION_PATH_INITIALIZED';

// a profile that hangs must not hold up a first launch indefinitely
const kQueryTimeoutMs = 10000;

interface RememberedLoginShellPath {
  shell: string;
  path: string;
}

// Workaround for electron-store CommonJS/ESM type mismatch
interface StoreInterface {
  get(key: string, defaultValue?: unknown): unknown;
  set(key: string, value: unknown): void;
}

let store: StoreInterface | undefined;
let pending: Promise<string | null> | undefined;

// the previous launch's answer, if any
let remembered: string | null = null;

// this launch's answer: undefined until the shell replies, null if it failed
let fresh: string | null | undefined;

function openStore(cwd?: string): StoreInterface {
  const options: Record<string, unknown> = { name: 'startup-state' };
  if (cwd) {
    options.cwd = cwd;
  }
  return new ElectronStore(options) as unknown as StoreInterface;
}

function readRememberedPath(shell: string): string | null {
  const entry = store?.get('loginShellPath') as RememberedLoginShellPath | undefined;
  if (entry && entry.shell === shell && entry.path.length > 0) {
    return entry.path;
  }
  return null;
}

async function queryLoginShellPath(shell: string): Promise<string | null> {
  return new Promise((resolve) => {
    // don't inherit PATH, so the shell computes it from scratch
    const env = { ...process.env };
    delete env.PATH;

    let stdout = '';
    const child = spawn(shell, ['-l', '-c', 'printf "%s" "$PATH"'], { env, stdio: ['ignore', 'pipe', 'ignore'] });
    const timer = setTimeout(() => {
      logger().logWarning(`Login shell ${shell} did not report PATH within ${kQueryTimeoutMs}ms; killing it`);
      child.kill();
    }, kQueryTimeoutMs);

    child.stdout.on('data', (data) => (stdout += data));
    child.on('error', (error) => {
      clearTimeout(timer);
      logger().logError(error);
      resolve(null);
    });
    child.on('exit', (code) => {
      clearTimeout(timer);
      if (code !== 0) {
        logger().logDebug(`Login shell ${shell} exited with code ${code}; not using its PATH`);
        resolve(null);
        return;
      }

      // profiles may print; the PATH is whatever came last
      const lines = stdout.trim().split('\n');
      const path = lines[lines.length - 1].trim();
      resolve(path.length > 0 ? path : null);
    });
  });
}

/**
 * Starts asking the login shell for PATH. Call as early as possible; does
 * nothing except on macOS.
 *
 * @param storeCwd Directory for the state file (tests only; defaults to the
 *   app's user data directory).
 */
export function startLoginShellPathQuery(storeCwd?: string): void {
  if (pending !== undefined || process.platform !== 'darwin') {
    return;
  }

  const shell = getenv('RSTUDIO_SESSION_SHELL') || getenv('SHELL');
  if (shell.length === 0) {
    return;
  }

  try {
    store = openStore(storeCwd);
    remembered = readRememberedPath(shell);
  } catch (error: unknown) {
    logger().logError(error);
  }

  pending = queryLoginShellPath(shell).then((path) => {
    fresh = path;
    if (path !== null && path !== remembered) {
      logger().logDebug(`Login shell PATH: ${path}`);
      try {
        store?.set('loginShellPath', { shell, path } as RememberedLoginShellPath);
      } catch (error: unknown) {
        logger().logError(error);
      }
    }
    return path;
  });
}

/**
 * The PATH to launch a session with, without waiting: the shell's answer if
 * it has arrived, otherwise the previous launch's, otherwise null.
 */
export function cachedLoginShellPath(): string | null {
  return fresh ?? remembered;
}

/**
 * The PATH to launch a session with. Resolves immediately when the shell
 * has answered or a previous launch's answer is available; on a first
 * launch it waits for the shell (bounded by the query timeout).
 */
export async function loginShellPath(): Promise<string | null> {
  const known = cachedLoginShellPath();
  if (known !== null || pending === undefined) {
    return known;
  }
  return pending;
}

/** Forgets everything; tests only. */
export function resetLoginShellPath(): void {
  store = undefined;
  pending = undefined;
  remembered = null;
  fresh = undefined;
}
