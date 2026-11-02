/*
 * relaunch-project.ts
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

import { app } from 'electron';
import { readFileSync, rmSync, writeFileSync } from 'fs';
import path from 'path';
import { safeError } from '../core/err';
import { logger } from '../core/logger';

// An in-app update relaunches RStudio from the installer (Windows) or from
// Squirrel (macOS), neither of which knows which project was open, and
// "Restart RStudio" preserves the project. So the project is written down
// before quitting and read back, once, by the next launch.

/** Long enough for the installer to finish; short enough not to hijack a later launch. */
export const kRelaunchProjectTtlMs = 15 * 60 * 1000;

/** The remembered project, or null when the marker is malformed or expired. */
export function parseRelaunchProject(text: string, now: number): string | null {
  try {
    const doc = JSON.parse(text) as { project?: unknown; expires?: unknown };
    if (typeof doc.project !== 'string' || doc.project.length === 0) {
      return null;
    }
    if (typeof doc.expires !== 'number' || now > doc.expires) {
      return null;
    }
    return doc.project;
  } catch {
    return null;
  }
}

function markerPath(): string {
  return path.join(app.getPath('userData'), 'relaunch-project.json');
}

/** Remember the project (or kProjectNone) the relaunch after an update should open. */
export function rememberRelaunchProject(project: string): void {
  try {
    const marker = { project, expires: Date.now() + kRelaunchProjectTtlMs };
    writeFileSync(markerPath(), JSON.stringify(marker));
  } catch (error: unknown) {
    logger().logDebug(`could not remember the project to reopen: ${safeError(error).message}`);
  }
}

export function forgetRelaunchProject(): void {
  rmSync(markerPath(), { force: true });
}

/** Consume the remembered project; null when there is none or it has expired. */
export function takeRelaunchProject(now = Date.now()): string | null {
  let text: string;
  try {
    text = readFileSync(markerPath(), 'utf8');
  } catch {
    return null;
  }
  forgetRelaunchProject();
  return parseRelaunchProject(text, now);
}
