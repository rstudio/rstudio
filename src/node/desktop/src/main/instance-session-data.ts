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

import fs from 'fs';
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

/**
 * Creates an empty session data folder for this (secondary) instance and returns its path, or
 * undefined if it can't, in which case the instance keeps the shared storage.
 */
export function prepareInstanceSessionData(userData: string, pid: number = process.pid): string | undefined {
  const dir = path.join(userData, kInstanceSessionDataDir, String(pid));
  try {
    // an exited instance with the same pid may have left its storage here
    fs.rmSync(dir, { recursive: true, force: true });
    fs.mkdirSync(dir, { recursive: true });
    return dir;
  } catch (error: unknown) {
    logger().logErrorMessage(`Unable to create session data folder ${dir}: ${safeError(error).message}`);
    return undefined;
  }
}

/**
 * Removes the session data folders of instances that have exited. They can hold a whole
 * Chromium profile, so this runs asynchronously, once startup is over.
 */
export async function removeExitedInstanceSessionData(
  userData: string,
  isRunning: (pid: number) => boolean = isProcessRunning,
): Promise<void> {
  const root = path.join(userData, kInstanceSessionDataDir);
  let entries;
  try {
    entries = await fs.promises.readdir(root, { withFileTypes: true });
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
      await fs.promises.rm(dir, { recursive: true, force: true });
    } catch (error: unknown) {
      // a later launch tries again
      logger().logWarning(`Unable to remove session data of exited instance ${dir}: ${safeError(error).message}`);
    }
  }
}
