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

import { EditorUI } from "./ui";

// https://github.com/CrossRef/rest-api-doc
export interface CrossrefServer {
  works: (query: string) => Promise<CrossrefMessage<CrossrefWork>>;
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
  title?: string[];

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

export function imageForCrossrefType(ui: EditorUI, type: string): [string?, string?] {
  switch (type) {
    case 'monograph':
    case 'report':
    case 'journal-article':
    case 'journal-volume':
    case 'journal':
    case 'journal-issue':
    case 'proceedings-article':
    case 'dissertation':
    case 'report-series':
      return [ui.images.citations?.article, ui.images.citations?.article_dark];
    case 'book-section':
    case 'book-part':
    case 'book-series':
    case 'edited-book':
    case 'book-chapter':
    case 'book':
    case 'book-set':
    case 'book-track':
    case 'reference-book':
      return [ui.images.citations?.book, ui.images.citations?.book_dark];
    case 'dataset':
      return [ui.images.citations?.data, ui.images.citations?.data_dark];
    case 'reference-entry':
      return [ui.images.citations?.entry, ui.images.citations?.entry_dark];
    case 'posted-content':
      return [ui.images.citations?.web, ui.images.citations?.web_dark];
    case 'other':
    case 'standard':
    case 'standard-series':
    case 'peer-review':
    case 'component':
    case 'proceedings-series':
    case 'proceedings':
    default:
      return [ui.images.citations?.other, ui.images.citations?.other_dark];
  }
}

