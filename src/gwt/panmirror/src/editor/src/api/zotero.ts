/*
 * zotero.ts
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

import { CSL } from "./csl";

export interface ZoteroCollectionSpec {
  name: string;
  version: number;
}

export interface ZoteroCollection extends ZoteroCollectionSpec {
  items: CSL[] | null;
}

export interface ZoteroServer {
  getCollections: (file: string | null, collections: ZoteroCollectionSpec[]) => Promise<ZoteroCollection[]>;
}

