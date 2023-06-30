/*
 * user.ts
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

import os from 'os';

import { getenv } from './environment';
import { FilePath } from './file-path';

import desktop from '../native/desktop.node';
import { removeTrailingSlashes } from '../main/utils';

export function userHomePath(): FilePath {
  const pathsToCheck = [() => getenv('HOME')];
  if (process.platform === 'win32') {
    // On Windows, check the R_USER environment variable first, then the
    // HOME environment variable, to match the behaviour of R itself.
    // See R_ExpandFileName in src/gnuwin32/sys-win32.c in R source code.
    pathsToCheck.unshift(() => getenv('R_USER'));

    pathsToCheck.push(
      () => desktop.currentCSIDLPersonalHomePath(),
      () => desktop.defaultCSIDLPersonalHomePath(),
    );
  }
  for (const pathToCheck of pathsToCheck) {
    const path = verifiedPath(pathToCheck());
    if (path) return path;
  }
  return new FilePath(os.homedir());
}

export function userHomePathString(): string {
  return userHomePath().getAbsolutePath();
}

export function username(): string {
  try {
    return os.userInfo().username;
  } catch (err: unknown) {
    return '';
  }
}

/**
 * Removes trailing slashes from a path string and verifies that the path exists
 * @param path The path to verify
 * @returns A FilePath with trailing slashes removed if the path exists, otherwise undefined
 */
function verifiedPath(path: string): FilePath | undefined {
  const trimmedPath = removeTrailingSlashes(path);
  if (trimmedPath.length > 0) {
    const fp = new FilePath(trimmedPath);
    if (fp.existsSync()) return fp;
  }
  return undefined;
}
