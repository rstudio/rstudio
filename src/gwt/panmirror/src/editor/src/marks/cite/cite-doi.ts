/*
 * cite-doi.ts
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

import { EditorView } from "prosemirror-view";
import { Slice } from "prosemirror-inputrules/node_modules/@types/prosemirror-model";
import { EditorState, Transaction } from "prosemirror-state";

import { parseCitation, ParsedCitation } from "./cite";
import { parseCrossRefDOI, CrossrefWork, formatForPreview } from "../../api/crossref";
import { EditorUI, InsertCiteProps, InsertCiteUI } from "../../api/ui";
import { performReplacementPreventingCompletions } from "../../behaviors/completion/completion";
import { BibliographyManager, bibliographyPaths } from "../../api/bibliography";
import { suggestIdForEntry, formatAuthors, formatIssuedDate } from "./cite-bibliography_entry";


// Parses the transation or state to determine whether the current position
// represents a citation containing a DOI
export function parseDOI(context: EditorState | Transaction): ParsedCitation | undefined {
  const parsedCitation = parseCitation(context);
  if (parsedCitation) {
    const doi = parseCrossRefDOI(parsedCitation.token);
    if (doi) {
      return parsedCitation;
    }
    return undefined;
  }
}

// Parses a slice to determine whether the slice contains
// a single DOI
export function doiFromSlice(context: EditorState | Transaction, slice: Slice): ParsedCitation | undefined {
  const parsedCitation = parseCitation(context);
  if (parsedCitation) {
    // Concatenate all the text and search for a DOI
    let text: string | null = null;
    slice.content.forEach(node => (text = text + node.textContent));
    if (text !== null) {
      const doi = parseCrossRefDOI(text);
      if (doi) {
        return { ...parsedCitation, token: doi };
      }
    }
    return undefined;
  }
}

// Determines whether a a given string may be a DOI
// Note that this will validate the form of the string, but not
// whether it is actually a registered DOI
export function isDOI(token: string): boolean {
  return parseCrossRefDOI(token) !== undefined;
}


