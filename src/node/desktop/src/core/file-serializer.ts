/*
 * file-serializer.ts
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

import lineReader from 'line-reader';
import { err, Expected, ok } from './expected';
import { safeError } from './err';

import { FilePath } from './file-path';

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
 * Read lines from a text file into an array of strings.
 *
 * @param filePath File to read
 * @param trimAndIgnoreBlankLines skip blank lines
 * @returns Array of strings containing each line
 */
export async function readStringArrayFromFile(
  filePath: FilePath,
  trimAndIgnoreBlankLines = true,
): Promise<Expected<Array<string>>> {
  const result: string[] = [];
  try {
    await eachLine(filePath.getAbsolutePath(), (line: string) => {
      if (trimAndIgnoreBlankLines) {
        line = line.trim();
      }
      if (line.length > 0) {
        result.push(line);
      }
    });
  } catch (error: unknown) {
    return err(safeError(error));
  }
  return ok(result);
}
