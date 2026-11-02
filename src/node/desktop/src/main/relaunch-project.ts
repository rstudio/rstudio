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

import { createHash } from 'crypto';
import { app } from 'electron';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'fs';
import path from 'path';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { compareVersions } from './update-feed';

// An in-app update relaunches RStudio from the installer (Windows) or from
// Squirrel (macOS), neither of which knows which project was open, and
// "Restart RStudio" preserves the project. So the project is written down
// before quitting and read back, once, by the relaunched RStudio: the build
// that was just installed, running from the same place. Each installation
// has its own marker file, and any other RStudio launched in between (an
// older build, another installation) leaves it alone.

/** Long enough for the installer to finish; short enough not to hijack a later launch. */
export const kRelaunchProjectTtlMs = 15 * 60 * 1000;

/** Which RStudio a marker is meant for: a build, in an install directory. */
export interface RelaunchIdentity {
  version: string;
  installKey: string;
}

export interface RelaunchMarker extends RelaunchIdentity {
  /** the project file to reopen, or kProjectNone */
  project: string;
  expires: number;
}

/** A short key for the installation at `execPath`, stable across its updates. */
export function installationKey(execPath: string): string {
  return createHash('sha1').update(path.dirname(execPath).toLowerCase()).digest('hex').slice(0, 12);
}

/** The marker's contents, or null when it is not one. */
export function parseRelaunchMarker(text: string): RelaunchMarker | null {
  try {
    const doc = JSON.parse(text) as Partial<Record<keyof RelaunchMarker, unknown>>;
    const text_ = (value: unknown): value is string => typeof value === 'string' && value.length > 0;
    if (!text_(doc.project) || !text_(doc.version) || !text_(doc.installKey) || typeof doc.expires !== 'number') {
      return null;
    }
    return { project: doc.project, version: doc.version, installKey: doc.installKey, expires: doc.expires };
  } catch {
    return null;
  }
}

export function markerExpired(marker: RelaunchMarker, now: number): boolean {
  return now > marker.expires;
}

/** Whether `marker` is meant for the RStudio described by `identity`. */
export function markerMatches(marker: RelaunchMarker, identity: RelaunchIdentity): boolean {
  return marker.installKey === identity.installKey && compareVersions(marker.version, identity.version) === 0;
}

/** One marker per installation, so installations never overwrite or cancel each other's. */
function markerPath(installKey: string): string {
  return path.join(app.getPath('userData'), 'relaunch-project', `${installKey}.json`);
}

/** Remember the project the relaunch into `identity` should open. */
export function rememberRelaunchProject(project: string, identity: RelaunchIdentity): void {
  try {
    const file = markerPath(identity.installKey);
    mkdirSync(path.dirname(file), { recursive: true });
    const marker: RelaunchMarker = { ...identity, project, expires: Date.now() + kRelaunchProjectTtlMs };
    writeFileSync(file, JSON.stringify(marker));
  } catch (error: unknown) {
    logger().logDebug(`could not remember the project to reopen: ${safeError(error).message}`);
  }
}

/** Drop this installation's marker; false when it could not be removed. */
export function forgetRelaunchProject(installKey: string): boolean {
  try {
    rmSync(markerPath(installKey), { force: true });
    return true;
  } catch (error: unknown) {
    // startup must go on; an undeletable marker expires on its own
    logger().logDebug(`could not remove the relaunch marker: ${safeError(error).message}`);
    return false;
  }
}

/**
 * Consume the project remembered for this RStudio: null when there is none,
 * it has expired, it is meant for another build of this installation, or it
 * could not be removed (reopening it on every launch until it expires would
 * be worse than not reopening it once).
 */
export function takeRelaunchProject(identity: RelaunchIdentity, now = Date.now()): string | null {
  let text: string;
  try {
    text = readFileSync(markerPath(identity.installKey), 'utf8');
  } catch {
    return null;
  }

  const marker = parseRelaunchMarker(text);
  if (!marker || markerExpired(marker, now)) {
    forgetRelaunchProject(identity.installKey);
    return null;
  }
  if (!markerMatches(marker, identity)) {
    return null;
  }

  return forgetRelaunchProject(identity.installKey) ? marker.project : null;
}
