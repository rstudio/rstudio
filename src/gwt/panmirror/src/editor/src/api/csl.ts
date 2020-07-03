/*
 * csl.ts
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

import { formatAuthors, formatIssuedDate } from "../marks/cite/cite-bibliography_entry";


export interface CSL {

  // Enumeration, one of the type ids from https://api.crossref.org/v1/types
  type: string;

  // Name of work's publisher
  publisher?: string;

  // Title
  title?: string;

  // DOI of the work 
  DOI?: string;

  // URL form of the work's DOI
  URL?: string;

  // Array of Contributors
  author?: CSLName[];

  // Earliest of published-print and published-online
  issued?: CSLDate;

  // Full titles of the containing work (usually a book or journal)
  'container-title'?: string;

  // Short titles of the containing work (usually a book or journal)
  'short-container-title'?: string;

  // Issue number of an article's journal
  issue?: string;

  // Volume number of an article's journal
  volume?: string;

  // Pages numbers of an article within its journal
  page?: string;

  ISSN?: string;
}

export interface CSLName {
  family: string;
  given: string;
  literal?: string;
}

export interface CSLDate {
  'date-parts'?: Array<[number, number?, number?]>;
  'raw'?: string;
}
export interface CSLField {
  name: string;
  value: string;
}

export function sanitizeForCiteproc(csl: CSL): CSL {

  // the CSL returned by the server may include some fields with invalid types
  // for example, ISSN may be returned as an array of ISSNs. But Citeproc expects
  // this to be a simple string.
  if (csl.ISSN && Array.isArray(csl.ISSN)) {
    return {
      ...csl,
      ISSN: csl.ISSN[0]
    };
  }

  return csl;
}

export function formatForPreview(csl: CSL): CSLField[] {

  const pairs = new Array<CSLField>();
  if (csl.title) {
    pairs.push({ name: "Title", value: csl.title });
  }
  pairs.push({ name: "Authors", value: formatAuthors(csl.author, 255) });
  if (csl.issued) {
    pairs.push({ name: "Issue Date", value: formatIssuedDate(csl.issued) });
  }

  const containerTitle = csl["container-title"];
  if (containerTitle) {
    pairs.push({ name: "Publication", value: containerTitle });
  }

  const volume = csl.volume;
  if (volume) {
    pairs.push({ name: "Volume", value: volume });
  }

  const page = csl.page;
  if (page) {
    pairs.push({ name: "Page(s)", value: page });
  }

  return pairs;
}
