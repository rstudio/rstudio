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
  doi: string;
  url: string;
  type: string;
}
