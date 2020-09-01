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
import { Node as ProsemirrorNode } from 'prosemirror-model';

import { parseYamlNodes } from "./yaml";

export interface CSL {

  // The id. This is technically required, but some providers (like crossref) don't provide
  // one
  id?: string;

  // Enumeration, one of the type ids from https://api.crossref.org/v1/types
  type: string;

  // An item key that may be used to identify this item
  key?: string;

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
  const cslAny: { [key: string]: any } = {
    ...csl
  };
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

  // Strip any raw date representations
  if (csl.issued?.raw) {
    csl.issued.raw = undefined;
  }

  // pandoc-citeproc performance is extremely poor with large abstracts. As a result, purge this property
  cslAny.abstract = undefined;

  cslAny.id = undefined;

  return cslAny as CSL;
}

export function cslFromDoc(doc: ProsemirrorNode): string | undefined {

  // read the Yaml blocks from the document
  const parsedYamlNodes = parseYamlNodes(doc);

  const cslParsedYamls = parsedYamlNodes.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object' && parsedYaml.yaml.csl,
  );

  // Look through any yaml nodes to see whether any contain csl information
  if (cslParsedYamls.length > 0) {

    // Pandoc uses the last csl block (whether or not it shares a yaml block with the
    // bibliographies element that pandoc will ultimately use) so just pick the last csl
    // block.
    const cslParsedYaml = cslParsedYamls[cslParsedYamls.length - 1];
    const cslFile = cslParsedYaml.yaml.csl;
    return cslFile;
  }
  return undefined;
}

// Converts a csl date to an EDTF date.
// See https://www.loc.gov/standards/datetime/
// Currently omits time component so this isn't truly level 0
export function cslDateToEDTFDate(date: CSLDate) {
  if (date["date-parts"]) {
    const paddedParts = date["date-parts"][0].map(part => {
      const partStr = part?.toString();
      if (partStr?.length === 1) {
        return `0${partStr}`;
      }
      return partStr;
    });
    return paddedParts.join('-');
  }
}




