/*
 * script-tools.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

export function getProjectRootDir(): string {
  return path.dirname(__dirname);
}

export function getBuildOutputDir(): string {
  return path.join(getProjectRootDir(), 'dist');
}

export function getPackageOutputDir(): string {
  return path.join(getProjectRootDir(), 'out');
}

export function getPlatformPackageOutputDir(): string {
  return path.join(getPackageOutputDir(), `RStudio-${process.platform}-x64`);
}

export function section(message: string): string {
  return '\x1b[1m\x1b[36m==>\x1b[39m ' + message + '\x1b[0m';
}

export function info(message: string): string {
  return '\x1b[1m[I]\x1b[0m ' + message;
}

export function warn(message: string): string {
  return '\x1b[1m\x1b[31m[W]\x1b[0m ' + message;
}

export function getProgramFilesWindows(): string {
  return process.env['PROGRAMFILES'];
}

export async function isDirectory(path): Promise<boolean> {
  try {
    const stats = await promises.stat(path);
    return stats.isDirectory();
  } catch (err) {
    return false;
  }
}