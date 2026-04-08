/*
 * whats-new-utils.ts
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

import { accessSync, constants } from 'fs';
import { join, resolve } from 'path';

const SLUG_PATTERN = /^[a-z0-9-]+$/;

/**
 * Convert a flower name to a filesystem-safe slug.
 *
 * Algorithm:
 * 1. Lowercase
 * 2. Remove apostrophes
 * 3. Replace runs of non-[a-z0-9] with a single hyphen
 * 4. Strip leading/trailing hyphens
 */
export function toReleaseSlug(flowerName: string): string {
  return flowerName
    .toLowerCase()
    .replace(/'/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

/**
 * Validate that a slug matches ^[a-z0-9-]+$ (no path traversal).
 */
export function isValidSlug(slug: string): boolean {
  return slug.length > 0 && SLUG_PATTERN.test(slug);
}

/**
 * Check if a version string represents a release build.
 *
 * Release format: MAJOR.MINOR.PATCH+BUILD (no hyphen before +, + required).
 * Non-release: MAJOR.MINOR.PATCH-PRERELEASE+BUILD (hyphen before +).
 * Malformed (no +): not a release — partially generated or dev strings
 * like "2026.04.0" without build metadata are not valid release versions.
 */
export function isReleaseBuild(version: string): boolean {
  const plusIndex = version.indexOf('+');
  if (plusIndex === -1) {
    // No build metadata delimiter — not a well-formed release version
    return false;
  }
  const beforePlus = version.substring(0, plusIndex);
  return !beforePlus.includes('-');
}

/**
 * Resolve the path to a release's index.html in the bundled assets.
 * Returns the path if the file exists and is readable, null otherwise.
 *
 * In the packaged app, __dirname is .webpack/main/, so assets are at
 * ../renderer/assets/whats-new/<slug>/index.html relative to it.
 */
export function resolveWhatsNewContentPath(slug: string): string | null {
  if (!isValidSlug(slug)) {
    return null;
  }
  const assetsBase = resolve(__dirname, '..', 'renderer', 'assets', 'whats-new');
  const indexPath = join(assetsBase, slug, 'index.html');
  try {
    accessSync(indexPath, constants.R_OK);
    return indexPath;
  } catch {
    return null;
  }
}
