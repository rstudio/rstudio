/*
 * system.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
      } catch (error: unknown) {
        return false;
      }
    }
  }
  return false;
}
