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

// Pure helpers for the in-app updater: version parsing, channel and index
// URL selection, and parsing of the dailies.rstudio.com JSON API, which
// already lists every published build with its download link and SHA-256
// (https://dailies.rstudio.com/api/v1/openapi.json). Nothing here touches
// Electron, so it is unit-testable.

/**
 * The site whose JSON API the updater reads. Overridable at runtime with the
 * RSTUDIO_UPDATE_API_URL environment variable (useful for testing against a
 * local copy).
 */
export const kDefaultUpdateApiBaseUrl = 'https://dailies.rstudio.com';

/** Release notes link offered when the API has none for a build. */
export const kDefaultReleaseNotesUrl = 'https://docs.posit.co/ide/news/';

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
  /** true for a Pro build (".pro<n>" suffix) */
  pro: boolean;
}

/** One artifact of one build, as the API's products/platforms entries describe it. */
export interface BuildArtifact {
  version: string;
  url: string;
  filename: string;
  sha256: string;
  size: number;
  commit: string;
  /** ISO-8601 build date, or '' when absent */
  date: string;
}

/** The API's `products.electron.platforms` keys this platform needs. */
export interface PlatformKeys {
  /** the user-facing installer (DMG or exe), for "Quit and Download" */
  download: string;
  /** what the updater installs from: the zipped .app on macOS, the exe on Windows */
  install: string;
}

/**
 * Parse an RStudio version string: <YYYY>.<MM>.<patch>[-<tag>]+<build>[.pro<n>].
 * Returns null when the string does not look like one.
 */
export function parseVersion(version: string): ParsedVersion | null {
  const match = /^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z]+))?(?:\+(\d+))?(\.pro\d+)?/i.exec(version.trim());
  if (!match) {
    return null;
  }

  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
    tag: (match[4] as string | undefined)?.toLowerCase() ?? '',
    build: (match[5] as string | undefined) === undefined ? 0 : Number(match[5]),
    pro: (match[6] as string | undefined) !== undefined,
  };
}

/**
 * Compare two version strings numerically: major, minor, patch, then build
 * number. The pre-release tag is ignored: indexes are per channel, so both
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

/**
 * Whether this is a Pro (Workbench) desktop build. Pro builds are not
 * updated in-app: Workbench has its own distribution and will need its own
 * update infrastructure.
 */
export function isProVersion(version: string): boolean {
  return parseVersion(version)?.pro === true;
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
 * The URL slug of a release codename, as the dailies site and
 * docker/jenkins/publish-build.sh form it: "Parrot Balsam" -> "parrot-balsam".
 */
export function branchSlug(releaseName: string): string {
  return releaseName.trim().toLowerCase().replace(/\s+/g, '-');
}

/**
 * The index the updater polls for this channel, or null when the channel
 * never updates. Release builds use the release API, which lists only
 * published builds; a released branch's own index also shows unpublished
 * patch candidates, which users must not be offered.
 */
export function updateIndexUrl(baseUrl: string, channel: UpdateChannel, branch: string): string | null {
  const base = baseUrl.replace(/\/+$/, '');
  switch (channel) {
    case 'release':
      return `${base}/api/v1/release/index.json`;
    case 'preview':
    case 'daily':
      return `${base}/rstudio/${branch}/index.json`;
    case 'hourly':
      return `${base}/rstudio-hourly/${branch}/index.json`;
    default:
      return null;
  }
}

/**
 * The platform entries to read, or null when the platform has no in-app
 * update support (Linux installs come from the distribution's package
 * manager; see #4186).
 */
export function platformKeys(platform: NodeJS.Platform, arch: string): PlatformKeys | null {
  switch (platform) {
    case 'darwin':
      // the macOS build is universal, so one entry serves both architectures
      return { download: 'macos', install: 'macos-zip' };
    case 'win32':
      return arch === 'arm64'
        ? { download: 'windows-arm64', install: 'windows-arm64' }
        : { download: 'windows', install: 'windows' };
    default:
      return null;
  }
}

function asObject(value: unknown, what: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`${what} is not a JSON object`);
  }
  return value as Record<string, unknown>;
}

/**
 * Read `/api/v1/release/index.json` for the current release's newest
 * published build: `released[0]` is always the current release and
 * `published[0]` its newest build, so no ordering is assumed beyond what
 * the API documents.
 */
export function parseReleaseIndex(text: string): { buildsUrl: string; notesUrl: string } {
  const doc = asObject(JSON.parse(text), 'release index');
  const released = doc.released;
  if (!Array.isArray(released) || released.length === 0) {
    throw new Error('release index lists no released versions');
  }

  const current = asObject(released[0], 'current release');
  const published = current.published;
  if (!Array.isArray(published) || published.length === 0) {
    throw new Error('release index lists no published builds for the current release');
  }

  const build = asObject(published[0], 'published build');
  if (typeof build.builds_url !== 'string' || !/^https:\/\//i.test(build.builds_url)) {
    throw new Error('published build has no https builds_url');
  }

  return {
    buildsUrl: build.builds_url,
    notesUrl: typeof build.notes_url === 'string' ? build.notes_url : '',
  };
}

/**
 * Read one `products.electron.platforms` entry out of a build or branch
 * index. Returns null when the platform is absent (e.g. the zipped .app has
 * not been published for this build); throws when the entry is malformed.
 */
export function parseBuildIndex(text: string, platformKey: string): BuildArtifact | null {
  const doc = asObject(JSON.parse(text), 'build index');
  const products = asObject(doc.products ?? {}, 'products');
  if (products.electron === undefined) {
    return null;
  }
  const electron = asObject(products.electron, 'electron product');
  const platforms = asObject(electron.platforms ?? {}, 'platforms');
  if (platforms[platformKey] === undefined) {
    return null;
  }

  const entry = asObject(platforms[platformKey], `platform "${platformKey}"`);
  const requireString = (key: string): string => {
    const value = entry[key];
    if (typeof value !== 'string' || value.length === 0) {
      throw new Error(`platform "${platformKey}" is missing "${key}"`);
    }
    return value;
  };
  const optionalString = (key: string): string => (typeof entry[key] === 'string' ? (entry[key] as string) : '');

  const version = requireString('version');
  if (!parseVersion(version)) {
    throw new Error(`platform "${platformKey}" has an unrecognized version "${version}"`);
  }

  const url = requireString('link');
  if (!/^https:\/\//i.test(url)) {
    throw new Error(`platform "${platformKey}" download link must use https`);
  }

  const sha256 = requireString('sha256').toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(sha256)) {
    throw new Error(`platform "${platformKey}" sha256 is not a 64-digit hex string`);
  }

  return {
    version,
    url,
    filename: optionalString('filename') || url.substring(url.lastIndexOf('/') + 1),
    sha256,
    size: typeof entry.size === 'number' && Number.isFinite(entry.size) ? entry.size : 0,
    commit: optionalString('commit'),
    date: optionalString('date'),
  };
}

/**
 * The document Squirrel.Mac expects from an update server in its "default"
 * format (url, name, notes, pub_date), with the sha256 and size Electron
 * verifies before unpacking. The updater serves this from a loopback
 * listener, since the dailies API is not in this shape.
 */
export function squirrelFeedDocument(artifact: BuildArtifact, notesUrl: string): string {
  return JSON.stringify(
    {
      url: artifact.url,
      name: artifact.version,
      notes: notesUrl || kDefaultReleaseNotesUrl,
      pub_date: artifact.date || new Date().toISOString(),
      sha256: artifact.sha256,
      size: artifact.size,
    },
    null,
    2,
  );
}

/** True when `candidate` is newer than the running version. */
export function isUpdateAvailable(currentVersion: string, candidate: string): boolean {
  return compareVersions(candidate, currentVersion) > 0;
}
