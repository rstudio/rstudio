/*
 * xref.ts
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


export interface XRefServer {
  indexForFile: (file: string) => Promise<XRefs>;
  xrefForId: (file: string, id: string) => Promise<XRefs>;
}

export interface XRefs {
  baseDir: string;
  refs: XRef[];
}

export interface XRef {
  file: string;
  type: string;
  id: string;
  title: string;
}

export function xrefKey(xref: XRef) {
  return xref.type.length > 0 ? `${xref.type}:${xref.id}` : xref.id;
}
