/*
 * utils.ts
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

/****** NOTE: This code cannot use node.js. It is invoked from the renderer process. ******/

declare global {
  interface Window {
    // the UI language, set by the page's preload (see renderer/language-bridge.ts)
    desktopLanguage: string;
  }
}

/**
 * Removes duplicate separators from a path, given a separator for search.
 *
 * @param {string} path
 * @param {string} [separator='/']
 * @return {*}
 */
export function normalizeSeparators(path: string, separator = '/') {
  // don't mess with leading '\\' or '//' on a UNC path
  let prefix = '';
  if (path.startsWith('\\\\') || path.startsWith('//')) {
    prefix = `${separator}${separator}`;
    path = path.substring(2);
  }
  return `${prefix}${path.replace(/[\\/]+/g, separator)}`;
}

/**
 * Creates a path in which the user home path will be replaced by the ~ alias.
 */
export function createAliasedPath(filePath: string, userHomePath: string): string {
  // first, retrieve and normalize paths
  filePath = normalizeSeparators(filePath);
  let home = normalizeSeparators(userHomePath || '');

  // remove trailing slashes
  if (filePath.endsWith('/')) {
    filePath = filePath.slice(0, -1);
  }
  if (home.endsWith('/')) {
    home = home.slice(0, -1);
  }

  if (filePath === home) {
    return '~';
  }

  if (filePath.startsWith(home)) {
    // watch out for case where homepath is /user/foo and filePath is /user/foobar/
    const remainingPath = filePath.substring(home.length);
    if (remainingPath.startsWith('/')) {
      filePath = '~' + filePath.substring(home.length);
    }
  }
  return normalizeSeparators(filePath);
}

/**
 * Removes duplicated separators from a path based on platform.
 *
 * @export
 * @param {string} path
 * @return {*}
 */
export function normalizeSeparatorsNative(path: string) {
  /* using conditional to set the separator based on platform as `path` is not available here */
  const separator = process.platform === 'win32' ? '\\' : '/';
  return normalizeSeparators(path, separator);
}

// executable to use on Windows when spawning R to query path information
export const kWindowsRExe = 'Rterm.exe';
