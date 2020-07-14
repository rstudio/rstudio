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

export interface CSL {

  // The id. This is technically required, but some providers (like crossref) don't provide
  // one
  id?: string;

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

  // These properties are often not included in CSL entries and are here
  // primarily because they may need to be sanitized
  ISSN?: string;
  ISBN?: string;
  'original-title'?: string;
  'short-title'?: string;
  'subtitle'?: string;
  subject?: string;
  archive?: string;
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

// Crossref sends some items back with invalid data types in the CSL JSON
// This appears to tend to happen the most frequently with fields that CrossRef
// stores as Arrays which are not properly flatted to strings as Pandoc cite-proc expects.
// This will flatten any of these fields to the first element of the array
export function sanitizeForCiteproc(csl: CSL): CSL {

  // This field list was create speculatively, so may contain fields that do not require
  // sanitization (or may omit fields that require it). 
  const sanitizeProperties = ['ISSN', 'ISBN', 'subject', 'archive', 'original-title', 'short-title', 'subtitle', 'container-title', 'short-container-title'];
  const cslAny = csl as { [key: string]: any };
  const keys = Object.keys(cslAny);
  keys
    .filter(key => sanitizeProperties.includes(key))
    .forEach((property) => {
      const value = cslAny[property];
      if (value && Array.isArray(value)) {
        if (value.length > 0) {
          cslAny[property] = value[0];
        } else {
          cslAny[property] = undefined;
        }
      }
      return csl;
    });
  return cslAny as CSL;
}


