/*
 * installer-staging.ts
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
import { createReadStream, createWriteStream } from 'fs';
import { mkdir, readdir, rename, rm, stat } from 'fs/promises';
import path from 'path';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';
import { BuildArtifact, installerVersion, isUpdateAvailable } from './update-feed';

// Staging of the Windows installer the updater downloads. Nothing here
// touches Electron: the fetch is injected, so the download, verification,
// reuse and pruning rules are unit-tested against real files.

/** The part of fetch() these functions use; Electron's net.fetch satisfies it. */
export type FetchLike = (url: string) => Promise<{
  ok: boolean;
  status: number;
  body: ReadableStream<Uint8Array> | null;
  headers: { get(name: string): string | null };
}>;

/** A partial download older than this was abandoned by an instance that died. */
export const kPartialDownloadTtlMs = 24 * 60 * 60 * 1000;

/** True when `file` exists with the size and SHA-256 the API published. */
export async function verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean> {
  try {
    if (entry.size > 0 && (await stat(file)).size !== entry.size) {
      return false;
    }
    const hash = createHash('sha256');
    for await (const chunk of createReadStream(file)) {
      hash.update(chunk as Buffer);
    }
    return hash.digest('hex') === entry.sha256;
  } catch {
    return false;
  }
}

/**
 * Put a verified copy of `entry` in `dir` and return its path. The directory
 * is shared by every window of one installation, so a verified installer
 * already there (left by another window, or by an earlier run whose user
 * chose "Later") is reused rather than replaced, and a fresh download lands
 * under a private name until it has been verified. Rejects, leaving nothing
 * behind, when the download fails or does not match the published checksum
 * and size.
 */
export async function stageInstaller(
  dir: string,
  entry: BuildArtifact,
  fetch: FetchLike,
  onProgress: (fraction: number) => void = () => undefined,
): Promise<string> {
  await mkdir(dir, { recursive: true });
  const target = path.join(dir, path.basename(entry.filename));
  if (await verifyInstaller(target, entry)) {
    return target;
  }

  const partial = `${target}.${process.pid}.part`;
  try {
    await download(entry, partial, fetch, onProgress);
    if (!(await verifyInstaller(partial, entry))) {
      throw new Error('the downloaded installer did not match the checksum and size published for this release');
    }
    await rename(partial, target);
  } finally {
    await rm(partial, { force: true });
  }
  return target;
}

async function download(
  entry: BuildArtifact,
  file: string,
  fetch: FetchLike,
  onProgress: (fraction: number) => void,
): Promise<void> {
  const response = await fetch(entry.url);
  if (!response.ok || !response.body) {
    throw new Error(`download failed with HTTP ${response.status}`);
  }

  const total = Number(response.headers.get('content-length')) || entry.size;
  let received = 0;
  const source = Readable.fromWeb(response.body as import('stream/web').ReadableStream);
  source.on('data', (chunk: Buffer) => {
    received += chunk.length;
    if (total > 0) {
      onProgress(received / total);
    }
  });
  await pipeline(source, createWriteStream(file));
}

/**
 * Drop staged installers that can no longer be used: builds no newer than
 * `currentVersion` (the update was applied, or superseded) and partial
 * downloads abandoned by an instance that died. Newer installers stay, so
 * "Later" followed by a relaunch does not download again. Returns the files
 * removed; problems with individual files are reported, not thrown.
 */
export async function pruneStagedInstallers(
  dir: string,
  currentVersion: string,
  now = Date.now(),
  onError: (file: string, error: unknown) => void = () => undefined,
): Promise<string[]> {
  let names: string[];
  try {
    names = await readdir(dir);
  } catch {
    return [];
  }

  const removed: string[] = [];
  for (const name of names) {
    const file = path.join(dir, name);
    try {
      const version = installerVersion(name);
      let stale: boolean;
      if (version) {
        stale = !isUpdateAvailable(currentVersion, version);
      } else if (/\.part$/i.test(name)) {
        stale = now - (await stat(file)).mtimeMs > kPartialDownloadTtlMs;
      } else {
        stale = false;
      }
      if (stale) {
        await rm(file, { force: true });
        removed.push(file);
      }
    } catch (error: unknown) {
      onError(file, error);
    }
  }
  return removed;
}
