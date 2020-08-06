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

export interface ZoteroResult {
  status: "ok" |    // ok (results in 'message')
  "notfound" |      // invalid api key
  "nohost" |        // no internet connectivity
  "error";          // unexpected error (details in 'error')
  message: any | null;
  warning: string;
  error: string;
}

export interface ZoteroCollectionSpec {
  name: string;
  version: number;
  key: string;
  parentKey: string;
}

export interface ZoteroCollection extends ZoteroCollectionSpec {
  items: ZoteroCSL[];
}

export interface ZoteroCSL extends CSL {
  collectionKeys?: string[];
}

export const kZoteroMyLibrary = 1;

// https://github.com/retorquere/zotero-better-bibtex/blob/master/translators/Better%20BibLaTeX.json
export const kZoteroBibLaTeXTranslator = 'f895aa0d-f28e-47fe-b247-2ea77c6ed583';

export interface ZoteroServer {

  validateWebAPIKey: (key: string) => Promise<boolean>;

  getCollections: (
    file: string | null,
    collections: string[] | null,
    cached: ZoteroCollectionSpec[],
    useCache: boolean
  ) => Promise<ZoteroResult>;

  getCollectionSpecs: ()
    => Promise<ZoteroResult>;

  // Return status: nohost w/ warning text if it fails to 
  // communciate w/ Better BibTeX. Otherwise returns 
  // status: ok with exported text in message.
  betterBibtexExport: (
    itemKeys: string[],
    translatorId: string,
    libraryId: number
  ) => Promise<ZoteroResult>;
}

