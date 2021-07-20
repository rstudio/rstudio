/*
 * cite-completion.tsx
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import { CompletionHandler, CompletionResult, CompletionHeaderProps } from '../../api/completion';
import { hasDOI } from '../../api/doi';
import { searchPlaceholderDecoration } from '../../api/placeholder';
import { EditorUI } from '../../api/ui';
import { CompletionItemView } from '../../api/widgets/completion';

import { PandocServer } from '../../api/pandoc';
import { EditorEvents } from '../../api/events';

import { parseCitation } from './cite';

import './cite-completion.css';
import { bibliographyCiteCompletionProvider, CiteCompletionProvider } from './cite-completion-bibliography';

const kAuthorMaxChars = 28;
const kMaxCitationCompletions = 100;
const kHeaderHeight = 20;

export const kCiteCompletionWidth = 400;
const kCiteCompletionItemPadding = 10;

export const kCitationCompleteScope = 'CitationScope';

// An entry which includes the source as well
// additional metadata for displaying a bibliograph item
export interface ReferenceEntry {
  id: string;
  type: string;
  primaryText: string;
  secondaryText: (len: number) => string;
  detailText: string;
  image?: string;
  imageAdornment?: string;
  replace: (view: EditorView, pos: number, server: PandocServer) => Promise<void>;
}


export function citationCompletionHandler(
  ui: EditorUI,
  _events: EditorEvents,
  bibManager: BibliographyManager,
  server: PandocServer,
): CompletionHandler<ReferenceEntry> {

  const completionProvider = bibliographyCiteCompletionProvider(ui, bibManager);

  return {
    id: 'AB9D4F8C-DA00-403A-AB4A-05373906FD8C',

    scope: kCitationCompleteScope,

    completions: citationCompletions(ui, completionProvider),

    filter: (entries: ReferenceEntry[], state: EditorState, token: string) => {
      return filterCitations(token, completionProvider, entries, ui, state.doc);
    },

    replace(view: EditorView, pos: number, entry: ReferenceEntry | null) {
      entry?.replace(view, pos, server);
      return Promise.resolve();
    },

    replacement(_schema: Schema, entry: ReferenceEntry | null): string | ProsemirrorNode | null {
      if (entry) {
        return entry.id;
      } else {
        return null;
      }
    },

    view: {
      header: () => {
        if (bibManager.warning()) {
          return {
            component: CompletionWarningHeaderView,
            height: kHeaderHeight,
            message: bibManager.warning(),
          };
        }
      },
      component: ReferenceCompletionItemView,
      key: entry => entry.id,
      width: kCiteCompletionWidth,
      height: 54,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function filterCitations(token: string, completionProvider: CiteCompletionProvider, entries: ReferenceEntry[], ui: EditorUI, doc: ProsemirrorNode) {
  // Empty query or DOI
  if (token.trim().length === 0 || hasDOI(token)) {
    return entries;
  }

  // Filter an exact match - if its exact match to an entry in the bibliography already, skip completion
  // Ignore any punctuation at the end of the token
  const tokenWithoutEndPunctuation = token.match(/.*[^\,\!\?\.\:]/);
  const completionId = tokenWithoutEndPunctuation ? tokenWithoutEndPunctuation[0] : token;
  if (completionProvider.exactMatch(completionId)) {
    return [];
  }

  // Perform a search
  const searchResults = completionProvider.search(token, kMaxCitationCompletions);
  return dedupe(searchResults || []);
}


function dedupe(entries: ReferenceEntry[]): ReferenceEntry[] {
  return uniqby(entries, (entry: ReferenceEntry) => `${entry.id}${entry.type}`);;
}

function sortEntries(entries: ReferenceEntry[]): ReferenceEntry[] {
  const dedupedSources = dedupe(entries);
  return dedupedSources.sort((a, b) => a.id.localeCompare(b.id));
}

function citationCompletions(ui: EditorUI, completionProvider: CiteCompletionProvider) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<ReferenceEntry> | null => {


    const parsed = parseCitation(context);
    if (parsed) {
      return {
        token: parsed.token,
        pos: parsed.pos,
        offset: parsed.offset,
        completions: async (_state: EditorState) => {

          // otherwise, do search and provide results when ready
          const currentEntries = completionProvider.currentEntries();
          if (currentEntries) {

            // kick off another load which we'll stream in by setting entries
            let loadedEntries: ReferenceEntry[] | null = null;
            completionProvider.streamEntries(context.doc, (entries: ReferenceEntry[]) => {
              loadedEntries = sortEntries(entries);
            });

            // return stream
            return {
              items: sortEntries(currentEntries),
              stream: () => loadedEntries,
            };

          } else {
            return completionProvider.awaitEntries(context.doc);
          }
        },
        decorations:
          parsed.token.length === 0
            ? DecorationSet.create(context.doc, [
              searchPlaceholderDecoration(context.selection.head, ui, ui.context.translateText('or DOI')),
            ])
            : undefined,
      };
    }
    return null;
  };
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
export const ReferenceCompletionItemView: React.FC<ReferenceEntry> = entry => {
  return (
    <CompletionItemView
      width={kCiteCompletionWidth - kCiteCompletionItemPadding}
      image={entry.image}
      imageAdornment={entry.imageAdornment}
      title={`@${entry.primaryText}`}
      detail={entry.secondaryText(kAuthorMaxChars - entry.primaryText.length)}
      subTitle={entry.detailText}
      htmlTitle={true}
    />
  );
};

const CompletionWarningHeaderView: React.FC<CompletionHeaderProps> = props => {
  return (
    <div className={'pm-completion-cite-warning pm-pane-border-color'}>
      {props.ui.context.translateText(props.message || 'An unexpected warning occurred.')}
    </div>
  );
};
