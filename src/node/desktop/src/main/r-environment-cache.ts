/*
 * r-environment-cache.ts
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

import { realpathSync, statSync } from 'fs';
import { join } from 'path';
import ElectronStore from 'electron-store';
import { createHash } from 'crypto';

interface Store {
  get(key: string): unknown;
  set(key: string, value: unknown): void;
}

interface Entry {
  version: number;
  executable: string;
  environment: string;
  installation: string;
  stdout: string;
}

// Keep the raw query output: parseRQueryResult still applies the environment
// of this launch (including the user's library search path) on cache hits.
export class REnvironmentCache {
  private store: Store;

  constructor(directory?: string) {
    // conf parses the file here; a damaged one must be a miss on this and
    // every later launch, not a throw that disables the cache until repaired.
    this.store = new ElectronStore({
      name: 'r-environment',
      clearInvalidConfig: true,
      ...(directory ? { cwd: directory } : {}),
    }) as unknown as Store;
  }

  read(executable: string, environment: NodeJS.ProcessEnv): string | null {
    try {
      const entry = this.store.get('query') as Entry | undefined;
      if (
        entry?.version !== 1 ||
        entry.executable !== executable ||
        typeof entry.stdout !== 'string' ||
        entry.environment !== environmentStamp(environment)
      ) {
        return null;
      }
      const home = queryHome(entry.stdout);
      return home && entry.installation === installationStamp(executable, home) ? entry.stdout : null;
    } catch {
      // A missing, damaged, or inaccessible cache must never prevent startup.
      return null;
    }
  }

  write(executable: string, environment: NodeJS.ProcessEnv, stdout: string): void {
    try {
      const home = queryHome(stdout);
      if (!home) {
        return;
      }
      this.store.set('query', {
        version: 1,
        executable,
        environment: environmentStamp(environment),
        installation: installationStamp(executable, home),
        stdout,
      } satisfies Entry);
    } catch {
      // Caching is optional, including when the installation is being changed.
    }
  }
}

function queryHome(stdout: string): string | null {
  const marker = stdout.lastIndexOf('\x1E');
  const fields = stdout.substring(marker + 1).split('\x1F');
  // writeLines appends the separator after the last field as well.
  if (fields.length === 10 && fields[9].trim() === '') {
    fields.pop();
  }
  return marker >= 0 && fields.length === 9 && fields[0].length > 0 && fields[1].length > 0 ? fields[1] : null;
}

function environmentStamp(environment: NodeJS.ProcessEnv): string {
  // --vanilla skips user startup files, but the launcher and site Renviron
  // can still use these variables to select an architecture or library path.
  const names = Object.keys(environment)
    .filter((name) => ['PATH', 'HOME', 'JAVA_HOME', 'LANG'].includes(name) || /^(R_|LC_|LD_|DYLD_)/.test(name))
    .sort();
  return createHash('sha256')
    .update(JSON.stringify(names.map((name) => [name, environment[name] ?? null])))
    .digest('hex');
}

function installationStamp(executable: string, home: string): string {
  const files = [
    executable,
    home,
    join(home, 'bin', 'R'),
    join(home, 'bin', 'exec', 'R'),
    join(home, 'lib', 'libR.dylib'),
    join(home, 'lib', 'libR.so'),
    join(home, 'bin', 'R.dll'),
    join(home, 'bin', 'x64', 'R.dll'),
    join(home, 'bin', 'i386', 'R.dll'),
    join(home, 'etc', 'Renviron'),
    join(home, 'etc', 'ldpaths'),
  ];
  const stamps = files.map((file) => {
    try {
      const target = realpathSync(file);
      const stat = statSync(target);
      return [file, target, stat.size, stat.mtimeMs, stat.ctimeMs];
    } catch (error: unknown) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
        return [file, null];
      }
      throw error;
    }
  });
  // The executable and R home must exist; a cache isn't proof that R is usable.
  if (stamps[0][1] === null || stamps[1][1] === null) {
    throw new Error('R installation is missing');
  }
  return createHash('sha256').update(JSON.stringify(stamps)).digest('hex');
}
