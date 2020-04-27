
/*
 * diff.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import diff from 'fast-diff';

export enum EditorChangeType {
  Insert = 1,
  Equal = 0,
  Delete = -1,
}

export interface EditorChange {
  type: EditorChangeType;
  value: string;
}

export function diffChars(from: string, to: string) : EditorChange[] {
  return diff(from, to).map(d => ({
    type: d[0],
    value: d[1]
  }));
}



