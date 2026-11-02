/*
 * update-feed.ts
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

// Pure helpers for the in-app updater: version parsing, channel and feed URL
// selection, and parsing of the releases.json feed. Nothing here touches
// Electron, so it is unit-testable and shared with the feed generator's
// conventions (package/update-feed.cmake).

/**
 * Where per-channel update feeds live. Overridable at runtime with the
 * RSTUDIO_UPDATE_FEED_URL environment variable (useful for testing against a
 * local feed).
 */
export const kDefaultUpdateFeedBaseUrl = 'https://cdn.posit.co/rstudio/updates';

/**
 * Build channels, derived from the pre-release tag of the running version.
 * A release build (no tag, e.g. 2026.10.1+239) is on 'release'. Developer
 * builds ('-dev') never auto-update.
 */
export type UpdateChannel = 'release' | 'preview' | 'daily' | 'hourly' | 'dev';

export interface ParsedVersion {
  major: number;
  minor: number;
  patch: number;
  /** pre-release tag without the leading '-', e.g. 'daily'; '' for releases */
  tag: string;
  /** numeric build number after '+'; 0 when absent */
  build: number;
}

/** One entry of a releases.json feed. */
export interface ReleaseFeedEntry {
  version: string;
  name: string;
  notes: string;
  pubDate: string;
  url: string;
  sha256: string;
  size: number;
  commit: string;
}

/**
 * Parse an RStudio version string: <YYYY>.<MM>.<patch>[-<tag>]+<build>[.pro<n>].
 * Returns null when the string does not look like one.
 */
export function parseVersion(version: string): ParsedVersion | null {
  const match = /^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z]+))?(?:\+(\d+))?/.exec(version.trim());
  if (!match) {
    return null;
  }

  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
    tag: (match[4] as string | undefined)?.toLowerCase() ?? '',
    build: (match[5] as string | undefined) === undefined ? 0 : Number(match[5]),
  };
}

/**
 * Compare two version strings numerically: major, minor, patch, then build
 * number. The pre-release tag is ignored: feeds are per channel, so both
 * sides carry the same tag. Unparseable versions compare as lowest.
 */
export function compareVersions(a: string, b: string): number {
  const pa = parseVersion(a);
  const pb = parseVersion(b);
  if (!pa || !pb) {
    return (pa ? 1 : 0) - (pb ? 1 : 0);
  }

  for (const key of ['major', 'minor', 'patch', 'build'] as const) {
    if (pa[key] !== pb[key]) {
      return pa[key] < pb[key] ? -1 : 1;
    }
  }
  return 0;
}

/** The channel a build belongs to, from its version's pre-release tag. */
export function channelForVersion(version: string): UpdateChannel {
  const parsed = parseVersion(version);
  if (!parsed) {
    return 'dev';
  }

  switch (parsed.tag) {
    case '':
      return 'release';
    case 'preview':
    case 'daily':
    case 'hourly':
      return parsed.tag;
    default:
      return 'dev';
  }
}

/**
 * The feed URL for this platform, or null when the platform has no in-app
 * update support (Linux installs come from the distribution's package
 * manager; see #4186).
 */
export function updateFeedUrl(
  baseUrl: string,
  channel: UpdateChannel,
  platform: NodeJS.Platform,
  arch: string,
): string | null {
  let os: string;
  let feedArch: string;
  switch (platform) {
    case 'darwin':
      // the macOS build is a universal binary, so one feed serves both
      os = 'macos';
      feedArch = 'universal';
      break;
    case 'win32':
      os = 'windows';
      feedArch = arch === 'arm64' ? 'arm64' : 'x86_64';
      break;
    default:
      return null;
  }

  return `${baseUrl.replace(/\/+$/, '')}/${channel}/${os}/${feedArch}/releases.json`;
}

/**
 * Parse the text of a releases.json feed. Throws on malformed input so the
 * caller can report (and log) the reason rather than act on a partial entry.
 */
export function parseReleaseFeed(text: string): ReleaseFeedEntry {
  // a SyntaxError from JSON.parse is descriptive enough to log as-is
  const raw: unknown = JSON.parse(text);

  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) {
    throw new Error('update feed is not a JSON object');
  }

  const obj = raw as Record<string, unknown>;
  const requireString = (key: string): string => {
    const value = obj[key];
    if (typeof value !== 'string' || value.length === 0) {
      throw new Error(`update feed is missing "${key}"`);
    }
    return value;
  };
  const optionalString = (key: string): string => (typeof obj[key] === 'string' ? (obj[key] as string) : '');

  const version = requireString('version');
  if (!parseVersion(version)) {
    throw new Error(`update feed has an unrecognized version "${version}"`);
  }

  const url = requireString('url');
  if (!/^https:\/\//i.test(url)) {
    throw new Error('update feed download URL must use https');
  }

  const sha256 = requireString('sha256').toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(sha256)) {
    throw new Error('update feed sha256 is not a 64-digit hex string');
  }

  const size = typeof obj.size === 'number' && Number.isFinite(obj.size) ? obj.size : 0;

  return {
    version,
    name: optionalString('name') || version,
    notes: optionalString('notes'),
    pubDate: optionalString('pub_date'),
    url,
    sha256,
    size,
    commit: optionalString('commit'),
  };
}

/** True when the feed entry is newer than the running version. */
export function isUpdateAvailable(currentVersion: string, entry: ReleaseFeedEntry): boolean {
  return compareVersions(entry.version, currentVersion) > 0;
}
