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
import { DecorationSet, EditorView } from 'prosemirror-view';

import React from 'react';
import uniqby from 'lodash.uniqby';

import { BibliographyManager, BibliographySource } from '../../api/bibliography/bibliography';
import { CompletionHandler, CompletionResult, performCompletionReplacement } from '../../api/completion';
import { hasDOI } from '../../api/doi';
import { searchPlaceholderDecoration } from '../../api/placeholder';
import { EditorUI } from '../../api/ui';
import { CompletionItemView } from '../../api/widgets/completion';

import { PandocServer } from '../../api/pandoc';
import { EditorEvents } from '../../api/events';
import { FocusEvent } from '../../api/event-types';

import { BibliographyEntry, entryForSource } from './cite-bibliography_entry';
import { parseCitation, insertCitation } from './cite';

const kAuthorMaxChars = 28;
const kMaxCitationCompletions = 100;

export const kCiteCompletionWidth = 400;
const kCiteCompletionItemPadding = 10;

export const kCitationCompleteScope = 'CitationScope';

export function citationCompletionHandler(
  ui: EditorUI,
  events: EditorEvents,
  bibManager: BibliographyManager,
  server: PandocServer
): CompletionHandler<BibliographyEntry> {

  // prime bibliography on initial focus
  const focusUnsubscribe = events.subscribe(FocusEvent, (doc) => {
    bibManager.load(ui, doc!);
    focusUnsubscribe();
  });

  return {
    id: 'AB9D4F8C-DA00-403A-AB4A-05373906FD8C',

    scope: kCitationCompleteScope,

    completions: citationCompletions(ui, bibManager),

    filter: (entries: BibliographyEntry[], _state: EditorState, token: string) => {
      return filterCitations(token, bibManager, entries, ui);
    },

    replace(view: EditorView, pos: number, entry: BibliographyEntry | null) {
      if (entry && bibManager.findIdInLocalBibliography(entry.source.id)) {
        // It's already in the bibliography, just write the id
        const tr = view.state.tr;
        const schema = view.state.schema;
        const id = schema.text(entry.source.id, [schema.marks.cite_id.create()]);
        performCompletionReplacement(tr, pos, id);
        view.dispatch(tr);
      } else if (entry && entry.source.DOI) {
        // It isn't in the bibliography, show the insert cite dialog
        insertCitation(view, entry.source.DOI, bibManager, pos, ui, server, entry.source, entry.source.provider);
      }
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
  entries: BibliographyEntry[],
  ui: EditorUI,
) {
  // Empty query or DOI
  if (token.trim().length === 0 || hasDOI(token)) {
    return entries;
  }

  // String for a search
  const searchResults = manager.searchAllSources(token, kMaxCitationCompletions).map(entry => entryForSource(entry, ui));
  const dedupedResults = uniqby(searchResults, (entry: BibliographyEntry) => entry.source.id);

  // If we hav an exact match, no need for completions
  if (dedupedResults.length === 1 && dedupedResults[0].source.id === token) {
    return [];
  } else {
    return dedupedResults || [];
  }
}

function citationCompletions(ui: EditorUI, manager: BibliographyManager) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    const parsed = parseCitation(context);
    if (parsed) {
      return {
        token: parsed.token,
        pos: parsed.pos,
        offset: parsed.offset,
        completions: async (_state: EditorState) => {

          // function to retreive entries from the bib manager
          const managerEntries = () => {
            // Filter duplicate sources
            const dedupedSources = uniqby(manager.allSources(), (source: BibliographySource) => source.id);

            // Sort by id by default
            const sortedSources = dedupedSources.sort((a, b) => a.id.localeCompare(b.id));
            return sortedSources.map(source => entryForSource(source, ui));
          };

          // if we already have some completions from a previous load, then
          // return them and kick off a refresh (new results will stream in)
          if (manager.hasSources()) {

            // kick off another load which we'll stream in by setting entries
            let loadedEntries: BibliographyEntry[] | null = null;
            manager.load(ui, context.doc).then(() => {
              loadedEntries = managerEntries();
            });

            // return stream
            return {
              items: managerEntries(),
              stream: () => loadedEntries
            };

            // no previous load, just perform the load and return the entries
          } else {
            return manager.load(ui, context.doc).then(() => {
              return managerEntries();
            });
          }
        },
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
      adornmentImage={entry.adornmentImage}
      title={`@${entry.source.id}`}
      subTitle={entry.source.title || ''}
      detail={detail}
      htmlTitle={true}
    />
  );
};
