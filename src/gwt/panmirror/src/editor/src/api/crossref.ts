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

// https://github.com/CrossRef/rest-api-doc
export interface CrossrefServer {
  works: (query: string) => Promise<CrossrefMessage<CrossrefWork>>;
  doi: (doi: string) => Promise<CrossrefWork>;
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
  publisher: string;
  title: string[];
  DOI: string;
  url: string;
  type: string;
  author: CrossrefContributor[];
  issued: CrossrefDate;
}

export interface CrossrefContributor {
  family: string;
  given: string;
}

export interface CrossrefDate {
  'date-parts': [number, number?, number?];
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

export function parseDOI(token: string): string | undefined {
  let match;
  kCrossrefMatches.find(regex => {
    match = token.match(regex);
    return match && match[0].length > 0;
  });
  return match;
}
