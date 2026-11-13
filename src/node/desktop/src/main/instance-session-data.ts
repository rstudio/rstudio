/*
 * instance-session-data.ts
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

import { readdirSync, rmSync } from 'fs';
import path from 'path';
import { safeError } from '../core/err';
import { logger } from '../core/logger';

// Chromium can't share a profile's storage (cookies, localStorage, caches) between processes.
// An instance started while another is running waits on each locked database (about 1.5 s for
// cookies and 4 s for localStorage, blocking the page) before falling back to empty in-memory
// storage. So only the primary instance uses the shared storage; each other instance gets a
// folder of its own, named for its pid, which a later launch removes once that pid has exited.
export const kInstanceSessionDataDir = 'instance-session-data';

/**
 * Whether a process with this pid exists.
 */
export function isProcessRunning(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch (error: unknown) {
    // EPERM: it exists, but belongs to someone else
    return (error as NodeJS.ErrnoException).code === 'EPERM';
  }
}

function removeStaleFolders(root: string, isRunning: (pid: number) => boolean): void {
  let entries;
  try {
    entries = readdirSync(root, { withFileTypes: true });
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
      logger().logError(error);
    }
    return;
  }

  for (const entry of entries) {
    if (!entry.isDirectory() || !/^\d+$/.test(entry.name) || isRunning(Number(entry.name))) {
      continue;
    }
    const dir = path.join(root, entry.name);
    try {
      rmSync(dir, { recursive: true, force: true });
    } catch (error: unknown) {
      // a later launch tries again
      logger().logWarning(`Unable to remove session data of exited instance ${dir}: ${safeError(error).message}`);
    }
  }
}

/**
 * The session data folder this instance should use instead of the shared one, or undefined
 * for the primary instance. Also removes folders left by instances that have exited.
 */
export function instanceSessionDataPath(
  userData: string,
  primary: boolean,
  pid: number = process.pid,
  isRunning: (pid: number) => boolean = isProcessRunning,
): string | undefined {
  const root = path.join(userData, kInstanceSessionDataDir);
  removeStaleFolders(root, isRunning);
  return primary ? undefined : path.join(root, String(pid));
}
