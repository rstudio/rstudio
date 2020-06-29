/*
 * crossref.ts
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

// https://github.com/CrossRef/rest-api-doc
export interface CrossrefServer {
  works: (query: string) => Promise<CrossrefMessage<CrossrefWork>>;
  doi: (doi: string, progressDelay: number) => Promise<CrossrefWork>;
}

export const kCrossrefItemsPerPage = 'items-per-page';
export const kCrossrefStartIndex = 'start-index';
export const kCrossrefSearchTerms = 'search-terms';
export const kCrossrefTotalResults = 'total-results';

export interface CrossrefMessage<T> {
  items: T[];
  [kCrossrefItemsPerPage]: number;
  query: {
    [kCrossrefStartIndex]: number;
    [kCrossrefSearchTerms]: string;
  };
  [kCrossrefTotalResults]: number;
}

// https://github.com/Crossref/rest-api-doc/blob/master/api_format.md#work
export interface CrossrefWork {
  // Name of work's publisher
  publisher: string;

  // Work titles, including translated titles
  title: string[];

  // DOI of the work 
  DOI: string;

  // URL form of the work's DOI
  URL: string;

  // Enumeration, one of the type ids from https://api.crossref.org/v1/types
  type: string;

  // Array of Contributors
  author: CrossrefContributor[];

  // Earliest of published-print and published-online
  issued: CrossrefDate;

  // Full titles of the containing work (usually a book or journal)
  'container-title'?: string;

  // Short titles of the containing work (usually a book or journal)
  'short-container-title'?: string;

  // Issue number of an article's journal
  issue: string;

  // Volume number of an article's journal
  volume: string;

  // Pages numbers of an article within its journal
  page: string;
}

export interface CrossrefFormattedField {
  name: string;
  value: string;
}

export interface CrossrefContributor {
  family: string;
  given?: string;
}

/* (Partial Date) Contains an ordered array of year, month, day of month. Only year is required. 
Note that the field contains a nested array, e.g. [ [ 2006, 5, 19 ] ] to conform 
to citeproc JSON dates */
export interface CrossrefDate {
  'date-parts': Array<[number, number?, number?]>;
}

// ^10.\d{4,9}/[-._;()/:A-Z0-9]+$
// Core regexes come from https://www.crossref.org/blog/dois-and-matching-regular-expressions/
const kModernCrossrefDOIRegex = /10.\d{4,9}\/[-._;()/:A-Z0-9]+$/i;
const kEarlyWileyCrossrefDOIRegex = /10.1002\/[^\s]+$/i;
const kOtherCrossrefDOIRegex1 = /10.\\d{4}\/\d+-\d+X?(\d+)\d+<[\d\w]+:[\d\w]*>\d+.\d+.\w+;\d$/i;
const kOtherCrossrefDOIRegex2 = /10.1021\/\w\w\d+$/i;
const kOtherCrossrefDOIRegex3 = /10.1207\/[\w\d]+\&\d+_\d+$/i;

const kCrossrefMatches = [
  kModernCrossrefDOIRegex,
  kEarlyWileyCrossrefDOIRegex,
  kOtherCrossrefDOIRegex1,
  kOtherCrossrefDOIRegex2,
  kOtherCrossrefDOIRegex3,
];

export function parseCrossRefDOI(token: string): string | undefined {
  let match = null;
  kCrossrefMatches.find(regex => {
    match = token.match(regex);
    return match && match[0].length > 0;
  });

  if (match === null) {
    return undefined;
  } else {
    return match[0];
  }
}

export function formatForPreview(work: CrossrefWork): CrossrefFormattedField[] {

  const pairs = new Array<CrossrefFormattedField>();
  pairs.push({ name: "Title", value: work.title[0] });
  pairs.push({ name: "Authors", value: formatAuthors(work.author, 255) });
  pairs.push({ name: "Issue Date", value: formatIssuedDate(work.issued) });

  const containerTitle = work["container-title"];
  if (containerTitle) {
    pairs.push({ name: "Publication", value: containerTitle });
  }

  const volume = work.volume;
  if (volume) {
    pairs.push({ name: "Volume", value: volume });
  }

  const page = work.page;
  if (volume) {
    pairs.push({ name: "Page(s)", value: page });
  }

  return pairs;
}
