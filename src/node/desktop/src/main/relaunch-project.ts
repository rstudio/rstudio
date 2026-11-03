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
import { mkdirSync, readFileSync, renameSync, rmSync, unlinkSync, writeFileSync } from 'fs';
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

/**
 * A short key for the installation at `execPath`, stable across its updates.
 * Case is folded only where the file system ignores it (Windows); a macOS
 * volume may be case-sensitive, and two installations differing only by case
 * must not share a key.
 */
export function installationKey(execPath: string, platform: NodeJS.Platform = process.platform): string {
  const dir = path.dirname(execPath);
  const folded = platform === 'win32' ? dir.toLowerCase() : dir;
  return createHash('sha1').update(folded).digest('hex').slice(0, 12);
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

/** Where the markers live; a parameter so tests can use a scratch directory. */
export function defaultMarkerDir(): string {
  return path.join(app.getPath('userData'), 'relaunch-project');
}

/** One marker per installation, so installations never overwrite or cancel each other's. */
function markerPath(dir: string, installKey: string): string {
  return path.join(dir, `${installKey}.json`);
}

/**
 * Remember the project the relaunch into `identity` should open. Written to
 * a private name and renamed into place, so a reader never sees a partial
 * marker (and never discards it as malformed).
 */
export function rememberRelaunchProject(project: string, identity: RelaunchIdentity, dir = defaultMarkerDir()): void {
  const file = markerPath(dir, identity.installKey);
  const partial = `${file}.${process.pid}.tmp`;
  try {
    mkdirSync(dir, { recursive: true });
    const marker: RelaunchMarker = { ...identity, project, expires: Date.now() + kRelaunchProjectTtlMs };
    writeFileSync(partial, JSON.stringify(marker));
    renameSync(partial, file);
  } catch (error: unknown) {
    // the relaunch then opens no project; this must not throw into the
    // install request, and whatever stopped the write (an unwritable or
    // missing directory, say) is likely to stop the cleanup too
    logger().logDebug(`could not remember the project to reopen: ${safeError(error).message}`);
    try {
      rmSync(partial, { force: true });
    } catch (cleanupError: unknown) {
      logger().logDebug(`could not remove ${partial}: ${safeError(cleanupError).message}`);
    }
  }
}

/** Drop this installation's marker, if any; idempotent, never throws. */
export function forgetRelaunchProject(installKey: string, dir = defaultMarkerDir()): void {
  try {
    rmSync(markerPath(dir, installKey), { force: true });
  } catch (error: unknown) {
    // startup must go on; an undeletable marker expires on its own
    logger().logDebug(`could not remove the relaunch marker: ${safeError(error).message}`);
  }
}

/**
 * Claim this installation's marker by deleting it. Two launches may both have
 * read the marker, but unlink() succeeds for only one of them, so exactly one
 * reopens the project. False when another launch claimed it first (ENOENT) or
 * it cannot be removed, in which case reopening it on every launch until it
 * expires would be worse than not reopening it once.
 */
export function claimRelaunchMarker(installKey: string, dir = defaultMarkerDir()): boolean {
  try {
    unlinkSync(markerPath(dir, installKey));
    return true;
  } catch (error: unknown) {
    logger().logDebug(`not reopening the remembered project: ${safeError(error).message}`);
    return false;
  }
}

/**
 * Consume the project remembered for this RStudio: null when there is none,
 * it has expired, it is meant for another build of this installation, or
 * another launch claimed it first.
 */
export function takeRelaunchProject(
  identity: RelaunchIdentity,
  now = Date.now(),
  dir = defaultMarkerDir(),
): string | null {
  let text: string;
  try {
    text = readFileSync(markerPath(dir, identity.installKey), 'utf8');
  } catch {
    return null;
  }

  const marker = parseRelaunchMarker(text);
  if (!marker || markerExpired(marker, now)) {
    forgetRelaunchProject(identity.installKey, dir);
    return null;
  }
  if (!markerMatches(marker, identity)) {
    return null;
  }

  return claimRelaunchMarker(identity.installKey, dir) ? marker.project : null;
}
