/*
 * script-tools.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { promises } from 'fs';
import path from 'path';
import lineReader from 'line-reader';

/**
 * @returns Folder containing package.json for Electron project
 */
export function getProjectRootDir(): string {
  return path.dirname(__dirname);
}

/**
 * @returns Folder where webpack puts build output
 */
export function getWebpackBuildOutputDir(): string {
  return path.join(getProjectRootDir(), '.webpack');
}

/**
 * @returns Root folder where forge package builds are generated
 */
export function getForgePackageOutputDir(): string {
  return path.join(getProjectRootDir(), 'out');
}

/**
 * @returns Platform-specific folder containing package build
 */
export function getForgePlatformOutputDir(): string {
  return path.join(getForgePackageOutputDir(), `RStudio-${process.platform}-x64`);
}

/**
 * @returns ProgramFiles folder on Windows
 */
export function getProgramFilesWindows(): string {
  return process.env['PROGRAMFILES'];
}

/**
 * @returns make-package build output folder
 */
export function getMakePackageDir(): string {
  let buildDir = '';
  let osFolder = '';
  switch (process.platform) {
    case 'darwin':
      osFolder = 'osx';
      break;
    case 'linux':
      osFolder = 'linux';
      break;
    case 'win32':
      osFolder = 'win32';
      break;
    default:
      console.error(`Unsupported platform: ${process.platform}`);
      process.exit(1);
  }

  // make-package location: this assumes script execution from the src/node/desktop folder
  return path.join('..', '..', '..', 'package', osFolder);
}

// promisify line-reader
const eachLine = async function (filename: string, iteratee: (line: string) => void): Promise<void> {
  return new Promise(function (resolve, reject) {
    lineReader.eachLine(filename, iteratee, function (err) {
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
};

/**
 * Load properties from a CMakeCache.txt-formatted file.
 *
 * Syntax:
 *   - lines starting with '#' or '//' are ignored (comments) as are blank lines
 *   - VAR:TYPE=value
 *       - the TYPE is ignored and everything is returned as a string
 *   - VAR:TYPE=
 *       - variables with no value are ignored
 *
 * @param file full path to CMakeCache.txt-formatted file (including filename)
 * @returns name/values
 */
export async function loadCMakeVars(file: string): Promise<Map<string, string>> {
  const results = new Map<string, string>();

  await eachLine(file, (line: string) => {
    line = line.trim();
    if (line.length > 0) {
      if (!line.startsWith('//') && !line.startsWith('#')) {
        const match = /^(.+):.+=(.+)/.exec(line);
        if (match) {
          results.set(match[1], match[2]);
        }
      }
    }
  });
  return results;
}

/**
 * @param path Path to test
 * @returns true if path is a folder
 */
export async function isDirectory(path): Promise<boolean> {
  try {
    const stats = await promises.stat(path);
    return stats.isDirectory();
  } catch (err) {
    return false;
  }
}
