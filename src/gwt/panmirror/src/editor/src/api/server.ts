/*
 * server.ts
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

import { PandocServer } from './pandoc';
import { CrossrefServer } from './crossref';
import { ZoteroServer } from './zotero';
import { XRefServer } from './xref';
import { DOIServer } from './doi';
import { PubMedServer } from './pubmed';
import { DataCiteServer } from './datacite';
import { EnvironmentServer } from './environment';

export interface EditorServer {
  readonly pandoc: PandocServer;
  readonly doi: DOIServer;
  readonly crossref: CrossrefServer;
  readonly datacite: DataCiteServer;
  readonly pubmed: PubMedServer;
  readonly zotero: ZoteroServer;
  readonly xref: XRefServer;
  readonly environment: EnvironmentServer;
}
