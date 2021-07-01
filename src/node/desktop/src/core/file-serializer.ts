/**
 * 
 * file-serializer.ts
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

import { Err, Success } from './err';
import { FilePath } from './file-path';
import { jsonLiteralEscape } from './string-utils';

export interface Serializer {
  exportToString<T>(collection: Iterable<T>): string;
  importFromString<T>(str: string): Iterable<T>;
}

export function writeCollectionToFile<K, V>(filePath: FilePath, collection: Map<K, V>, stringifyFunction: ([key, value]: [K, V]) => string): Err {
  const oStream = filePath.openForWrite();
  
  collection.forEach((value: V, key: K, map: Map<K, V>) => {
    oStream.write(stringifyFunction([key, value]));
  });
  
  return Success();
}
  
export function stringifyStringPair([first, second]: [string, string]): string {
  return first + '="' + jsonLiteralEscape(second) + '"';
}

export function writeStringMapToFile(filepath: FilePath, map: Map<string, string>): Err {
  return writeCollectionToFile<string, string>(filepath, map, stringifyStringPair);
}

export function writeStringToFile(filepath: FilePath, str: string, /*lineEnding, */ truncate = true /*maxopenretryseconds */): Err {
  const oStream = filepath.openForWrite(truncate);

  oStream.write(str);

  return Success();
}
