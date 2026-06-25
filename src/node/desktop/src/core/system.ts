/*
 * system.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import { v4 as uuidv4 } from 'uuid';
import crc from 'crc';
import fs from 'fs';
import { createServer } from 'net';

import { FilePath } from './file-path';

export function generateUuid(includeDashes = true): string {
  let uuid = uuidv4();
  if (!includeDashes) {
    uuid = uuid.replace(/-/g, '');
  }
  return uuid;
}

export function generateShortenedUuid(): string {
  return crc.crc32(generateUuid(false)).toString(16);
}

export function generateRandomPort(): number {
  // Create a random-ish port number to avoid collisions between different
  // instances of rdesktop-launched rsessions; not a cryptographically
  // secure technique so don't copy/paste for such purposes.
  const base = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER);
  return (base % 40000) + 8080;
}

/**
 * Check whether a TCP port can currently be bound on the loopback interface.
 *
 * @param port The port to test.
 * @returns A promise resolving to true if the port is free, false otherwise.
 */
async function isPortFree(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const server = createServer();

    server.once('error', () => resolve(false));
    server.once('listening', () => {
      server.close(() => resolve(true));
    });

    server.listen(port, '127.0.0.1');
  });
}

/**
 * Find a TCP port that is currently free on the loopback interface.
 *
 * Picks random candidates with generateRandomPort() and verifies each is
 * bindable before returning it, so two RStudio instances launched at the same
 * time are very unlikely to hand the same port to their rsession backends. A
 * small time-of-check/time-of-use window remains -- the port could be claimed
 * between this probe and rsession's own bind -- but that is far narrower than
 * trusting an unverified random number, which is the dominant collision source
 * when many sessions start concurrently (e.g. parallel E2E workers).
 *
 * Falls back to an unverified random port if no candidate is found free within
 * maxAttempts, so callers always receive a usable port.
 *
 * @param maxAttempts The maximum number of candidate ports to probe.
 * @returns A promise resolving to a port number.
 */
export async function findFreePort(maxAttempts = 20): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const candidate = generateRandomPort();
    if (await isPortFree(candidate)) {
      return candidate;
    }
  }

  return generateRandomPort();
}

export function localPeer(port: number): string {
  // local peer used for named-pipe communication on Windows
  return `\\\\.\\pipe\\${port.toString()}-rsession`;
}

export function isCentOS(): boolean {
  if (process.platform === 'linux') {
    const redhatRelease = new FilePath('/etc/redhat-release');
    if (redhatRelease.existsSync()) {
      try {
        const contents = fs.readFileSync(redhatRelease.getAbsolutePath(), 'utf-8');
        return contents.includes('CentOS') || contents.includes('Red Hat Enterprise Linux');
      } catch (_error: unknown) {
        return false;
      }
    }
  }
  return false;
}

export function fixupExecutablePath(exePath: FilePath): FilePath {
  if (process.platform !== 'win32') {
    return exePath; // do nothing on posix
  }

  if (exePath.getExtension().length === 0) {
    return exePath.getParent().completePath(exePath.getFilename() + '.exe');
  } else {
    return exePath;
  }
}
