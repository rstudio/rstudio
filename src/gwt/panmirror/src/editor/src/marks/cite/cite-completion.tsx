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

// TESTING
// TODO: let's make a note to ask for some targeted testing of fuzzy search weights by users that have large bibliographies

// FUTURE
// TODO: search doi, url, or crossref (data cite [hipster], pubmed?)
// TODO: Full insert reference panel including preview
// TODO: Could we adorn citations that don't resolve by id with a warning decoration as an aide to user
// TODO: How should I report errors to user (for example, invalid entry type)
// TODO: Improve large bibliography performance by 'warming up' bibliography manager

import { EditorState, Transaction } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { DecorationSet } from 'prosemirror-view';

import React from 'react';

import { BibliographyManager } from '../../api/bibliography';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorUI } from '../../api/ui';
import { getMarkRange, markIsActive } from '../../api/mark';
import { searchPlaceholderDecoration } from '../../api/placeholder';
import { PandocServer } from '../../api/pandoc';
import { CompletionItemView } from '../../api/widgets/completion';

import { BibliographyEntry, entryForSource } from './cite-bibliography_entry';
import { kEditingCiteIdRegEx } from './cite';

const kAuthorMaxChars = 28;
const kMaxCitationCompletions = 20;

export function citationCompletionHandler(ui: EditorUI, server: PandocServer): CompletionHandler<BibliographyEntry> {
  const bibliographyManager = new BibliographyManager(server);
  return {
    id: 'AB9D4F8C-DA00-403A-AB4A-05373906FD8C',

    completions: citationCompletions(ui, bibliographyManager),

    filter: (completions: BibliographyEntry[], _state: EditorState, token: string) => {
      return filterCitations(completions, token, bibliographyManager, ui);
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
      width: 400,
      height: 54,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function filterCitations(
  bibliographyEntries: BibliographyEntry[],
  token: string,
  manager: BibliographyManager,
  ui: EditorUI,
) {
  if (token.trim().length === 0) {
    return bibliographyEntries;
  }
  return manager.search(token, kMaxCitationCompletions).map(entry => entryForSource(entry, ui));
}

function citationCompletions(ui: EditorUI, manager: BibliographyManager) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    // return completions only if we are inside a cite id mark
    const markType = context.doc.type.schema.marks.cite_id;
    if (!markIsActive(context, markType)) {
      return null;
    }

    // Find the text and position of the user entry and use that for
    // the completion
    const range = getMarkRange(context.doc.resolve(context.selection.head - 1), markType);
    if (range) {
      const citeText = context.doc.textBetween(range.from, range.to);
      const match = citeText.match(kEditingCiteIdRegEx);
      if (match) {
        const token = match[2];
        const pos = range.from + match[1].length;
        return {
          token,
          pos,
          offset: -match[1].length,
          completions: (_state: EditorState) =>
            manager.sources(ui, context.doc).then(sources => sources.map(source => entryForSource(source, ui))),
          decorations:
            token.length === 0
              ? DecorationSet.create(context.doc, [searchPlaceholderDecoration(context.selection.head, ui)])
              : undefined,
        };
      }
    }

    return null;
  };
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
const BibliographySourceView: React.FC<BibliographyEntry> = entry => {
  const authorStr = entry.authorsFormatter(entry.source.author, kAuthorMaxChars - entry.source.id.length);
  const detail = `${authorStr} ${entry.issuedDateFormatter(entry.source.issued)}`;
  return (
    <CompletionItemView
      width={400}
      image={entry.image}
      title={`@${entry.source.id}`}
      subTitle={entry.source.title || ''}
      detail={detail}
      htmlTitle={true}
    />
  );
};
