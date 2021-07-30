/*
 * copy-files.ts
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

import fs from "fs";
import path from "path";

function isResourceFile(path: string) {
  return /[.](html|css)$/.test(path);
}

function copyRecursive(dir: string) {

  // list files in folder
  const dirents = fs.readdirSync(dir, {
    encoding: 'utf-8',
    withFileTypes: true,
  });

  for (const dirent of dirents) {
    if (dirent.isDirectory()) {
      copyRecursive(path.join(dir, dirent.name));
    } else if (dirent.isFile() && isResourceFile(dirent.name)) {

      const source = path.join(dir, dirent.name);
      const target = path.join('dist', source);
      const upToDate =
        fs.existsSync(target) &&
        fs.statSync(source).mtime <= fs.statSync(target).mtime;

      if (!upToDate) {
        fs.copyFileSync(source, target);
      }
    }
  }

}

// Copy resource files (e.g. .html files) from the 'src' directory
// to the 'dist' directory, so that development builds work fine.

try {
  process.chdir(path.join(__dirname, '..'));
  copyRecursive('src');
} catch (error) {
  console.log(error);
  process.exit(error);
}