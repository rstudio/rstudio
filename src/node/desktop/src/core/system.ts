/*
 * system.ts
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

import { v4 as uuidv4 } from 'uuid';

import crc from 'crc';

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