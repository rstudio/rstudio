/*
 * path.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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



export function expandPaths(rootPath: string, paths: string[]): string[] {
  return paths.map(path => joinPaths(rootPath, path));
}

export function joinPaths(root: string, path: string) {
  const mergedPath = `${root}/${path}`;

  // Clean out duplicate paths
  return mergedPath.replace(/\/\//g, '/');
}

export function getExtension(path: string) {

  // Get the file out of the path
  const fileName = path.split(/[\\/]/).pop();
  if (fileName) {
    const lastDot = fileName.lastIndexOf(".");
    if (lastDot > 0) {
      return fileName.slice(lastDot + 1);
    }
  }
  return "";
}

export function changeExtension(path: string, extension: string) {
  const lastDot = path.lastIndexOf(".");
  const pathNoExtension = path.substr(0, lastDot + 1);
  return pathNoExtension + extension;
}

