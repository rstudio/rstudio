/*
 * cite-completion_doi.tsx
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


// TODO: Be sure to use polite pool for CrossRef.append mailto:query param to query string


import { EditorView } from 'prosemirror-view';
import { EditorState, Transaction } from 'prosemirror-state';

import React from 'react';

import { EditorUI, InsertCiteProps, InsertCitePreviewPair } from '../../api/ui';
import { CrossrefServer, parseDOI, CrossrefWork } from '../../api/crossref';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { parseCitation, kCitationCompleteScope } from './cite-completion';
import { imageForType, formatAuthors, formatIssuedDate, suggestId } from './cite-bibliography_entry';
import { CompletionItemDetailedView } from '../../api/widgets/completion-detailed';
import { performCompletionReplacement } from '../../behaviors/completion/completion';
import { BibliographyManager, bibliographyPaths } from '../../api/bibliography';

export function citationDoiCompletionHandler(
  ui: EditorUI,
  bibManager: BibliographyManager,
  server: CrossrefServer,
): CompletionHandler<CrossrefEntry> {
  return {
    id: '56DA14DD-6E3A-4481-93A9-938DC00393A5',

    scope: kCitationCompleteScope,

    completions: citationDOICompletions(ui, server),

    replace(view: EditorView, pos: number, completion: CrossrefEntry | null) {
      if (completion) {
        bibManager.sources(ui, view.state.doc).then(sources => {
          // Figure out a unique Id (across all bibliographies in use) to suggest
          const existingIds = sources.map(source => source.id);
          const suggestedId = suggestId(existingIds, completion.title[0], completion.author, completion.issued);

          const previewPairs = new Array<InsertCitePreviewPair>();
          previewPairs.push({ name: "Title", value: completion.title[0] });
          previewPairs.push({ name: "Type", value: completion.type });
          previewPairs.push({ name: "Authors", value: formatAuthors(completion.author, 255) });
          previewPairs.push({ name: "Issue Date", value: formatIssuedDate(completion.issued, ui) });

          const containerTitle = completion["container-title"];
          if (containerTitle) {
            previewPairs.push({ name: "Publication", value: containerTitle });
          }

          const volume = completion.volume;
          if (volume) {
            previewPairs.push({ name: "Volume", value: volume });
          }

          const page = completion.page;
          if (volume) {
            previewPairs.push({ name: "Page(s)", value: page });
          }

          previewPairs.push({ name: "DOI", value: completion.DOI });

          // Read bibliographies out of the document and pass those alone
          const bibliographies = bibliographyPaths(ui, view.state.doc);
          const citeProps: InsertCiteProps = {
            suggestedId,
            bibliographyFiles: bibliographies?.bibliography || [],
            previewPairs
          };

          // Ask the user to provide information that we need in order to populate
          // this citation (id, bibliography)
          const citation = ui.dialogs.insertCite(citeProps).then(result => {
            // If the user provided an id, insert the citation
            if (result && result.id.length) {
              // Use the biblography manager to write an entry to the user specified bibliography

              performCompletionReplacement(view, pos, result.id);
            }
          });
        });
      }
    },

    view: {
      component: CrossrefWorkView,
      key: work => work.DOI,
      width: 400,
      height: 120,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function citationDOICompletions(ui: EditorUI, server: CrossrefServer) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<CrossrefEntry> | null => {
    const parsed = parseCitation(context);
    if (parsed) {
      const doi = parseDOI(parsed.token);
      if (doi) {
        return {
          token: parsed.token,
          pos: parsed.pos,
          offset: parsed.offset,
          completions: (_state: EditorState) =>
            server.doi(parsed.token).then(work => [
              {
                ...work,
                image: imageForType(ui, work.type)[ui.prefs.darkMode() ? 1 : 0],
                formattedAuthor: formatAuthors(work.author, 50),
                formattedIssueDate: formatIssuedDate(work.issued, ui),
              },
            ]),
        };
      }
    }
    return null;
  };
}

interface CrossrefEntry extends CrossrefWork {
  image?: string;
  formattedAuthor: string;
  formattedIssueDate: string;
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
const CrossrefWorkView: React.FC<CrossrefEntry> = work => {
  return (
    <CompletionItemDetailedView
      width={400}
      image={work.image}
      heading={work['short-container-title'] || work.publisher}
      title={work.title[0]}
      subTitle={`${work.formattedAuthor} ${work.formattedIssueDate}` || ''}
    />
  );
};
