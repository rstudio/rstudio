/*
 * cite-completion.tsx
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

import { EditorState, Transaction } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { DecorationSet } from 'prosemirror-view';

import React from 'react';

import { BibliographyManager } from '../../api/bibliography';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorUI } from '../../api/ui';
import { searchPlaceholderDecoration } from '../../api/placeholder';
import { CompletionItemView } from '../../api/widgets/completion';

import { BibliographyEntry, entryForSource } from './cite-bibliography_entry';
import { parseCitation } from './cite';

// JJA: order of imports
import { hasDOI } from '../../api/doi';

const kAuthorMaxChars = 28;
const kMaxCitationCompletions = 100;

export const kCiteCompletionWidth = 400;
const kCiteCompletionItemPadding = 10;

export const kCitationCompleteScope = 'CitationScope';

export function citationCompletionHandler(
  ui: EditorUI,
  bibManager: BibliographyManager,
): CompletionHandler<BibliographyEntry> {
  return {
    id: 'AB9D4F8C-DA00-403A-AB4A-05373906FD8C',

    scope: kCitationCompleteScope,

    completions: citationCompletions(ui, bibManager),

    filter: (_completions: BibliographyEntry[], _state: EditorState, token: string) => {
      return filterCitations(token, bibManager, ui);
    },

    replacement(_schema: Schema, entry: BibliographyEntry | null): string | ProsemirrorNode | null {
      if (entry) {
        return entry.source.id;
      } else {
        return null;
      }
    },

    view: {
      component: BibliographySourceView,
      key: entry => entry.source.id,
      width: kCiteCompletionWidth,
      height: 54,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function filterCitations(
  token: string,
  manager: BibliographyManager,
  ui: EditorUI,
) {
  // Empty query or DOI
  if (token.trim().length === 0 || hasDOI(token)) {
    return [];
  }

  // String for a search
  return manager.search(token, kMaxCitationCompletions).map(entry => entryForSource(entry, ui));
}

function citationCompletions(ui: EditorUI, manager: BibliographyManager) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    const parsed = parseCitation(context);
    if (parsed) {
      return {
        token: parsed.token,
        pos: parsed.pos,
        offset: parsed.offset,
        completions: (_state: EditorState) =>
          manager.loadBibliography(ui, context.doc).then((bibliography) => {

            // Filter duplicate sources
            const parsedIds = bibliography.sources.map(source => source.id);
            const dedupedSources = bibliography.sources.filter((source, index) => {
              return parsedIds.indexOf(source.id) === index;
            });

            // Sort by id by default
            const sortedSources = dedupedSources.sort((a, b) => a.id.localeCompare(b.id));
            return sortedSources.map(source => entryForSource(source, ui));
          }
          ),
        decorations:
          parsed.token.length === 0
            ? DecorationSet.create(
              context.doc,
              [
                searchPlaceholderDecoration(
                  context.selection.head, ui,
                  ui.context.translateText('or DOI')
                )
              ]
            )
            : undefined,
      };
    }
    return null;
  };
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
export const BibliographySourceView: React.FC<BibliographyEntry> = entry => {
  const authorStr = entry.authorsFormatter(entry.source.author, kAuthorMaxChars - entry.source.id.length);
  const detail = `${authorStr} ${entry.issuedDateFormatter(entry.source.issued)}`;
  return (
    <CompletionItemView
      width={kCiteCompletionWidth - kCiteCompletionItemPadding}
      image={entry.image}
      title={`@${entry.source.id}`}
      subTitle={entry.source.title || ''}
      detail={detail}
      htmlTitle={true}
    />
  );
};
