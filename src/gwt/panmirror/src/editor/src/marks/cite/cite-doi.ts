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
import { parseCrossRefDOI, CrossrefWork } from "../../api/crossref";
import { EditorUI, InsertCiteProps, InsertCitePreviewPair } from "../../api/ui";
import { performReplacementPreventingCompletions } from "../../behaviors/completion/completion";
import { BibliographyManager, bibliographyPaths } from "../../api/bibliography";
import { suggestId, formatAuthors, formatIssuedDate } from "./cite-bibliography_entry";


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

// Replaces the current selection with a resolved citation id
export function insertCitationForDOI(
  work: CrossrefWork,
  bibManager: BibliographyManager,
  pos: number,
  ui: EditorUI,
  view: EditorView
) {
  bibManager.loadBibliography(ui, view.state.doc).then(bibliography => {

    // Read bibliographies out of the document and pass those alone
    const bibliographies = bibliographyPaths(ui, view.state.doc);

    const citeProps: InsertCiteProps = {
      suggestedId: suggestId(bibliography.sources.map(source => source.id), work.title[0], work.author, work.issued),
      bibliographyFiles: bibliography.project_biblios || bibliographies?.bibliography || [],
      previewPairs: previewPairs(work, ui)
    };

    // Ask the user to provide information that we need in order to populate
    // this citation (id, bibliography)
    const citation = ui.dialogs.insertCite(citeProps).then(result => {
      // If the user provided an id, insert the citation
      if (result && result.id.length) {
        // Use the biblography manager to write an entry to the user specified bibliography
        performReplacementPreventingCompletions(view, pos, result.id);
        view.focus();
      }
    });
  });
}

function previewPairs(work: CrossrefWork, ui: EditorUI): InsertCitePreviewPair[] {

  const pairs = new Array<InsertCitePreviewPair>();
  pairs.push({ name: "Title", value: work.title[0] });
  pairs.push({ name: "Type", value: work.type });
  pairs.push({ name: "Authors", value: formatAuthors(work.author, 255) });
  pairs.push({ name: "Issue Date", value: formatIssuedDate(work.issued, ui) });

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

  pairs.push({ name: "DOI", value: work.DOI });
  return pairs;
}