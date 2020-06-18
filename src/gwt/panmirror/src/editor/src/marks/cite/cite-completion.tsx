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

// BIBLIOGRAPHY
// TODO: Shared bibliography (set site level for books) -- distill, bookdown, etc...
// Sniff project provide shared bibliography as ui context
// TODO: Read references out of inline reference blocks and merge with bibliography references
// TODD: Need to filter entries to not include any duplicates

// UI
// TODO: Show preview for citation when mouseover (like inline math)
//        - would be nice if you could follow DOI to article when previewing

// FUTURE
// TODO: search doi, url, or crossref (data cite [hipster], pubmed?)
// TODO: Full insert reference panel including preview
// TODO: Could we adorn citations that don't resolve by id with a warning decoration as an aide to user

import { EditorState, Transaction } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { DecorationSet } from 'prosemirror-view';

import { PandocServer } from '../../api/pandoc';

import React from 'react';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { getMarkRange, markIsActive } from '../../api/mark';
import { iconAndTextPlaceholderDecoration } from '../../api/placeholder';

import { BibliographyEntry, BibliographyManager } from '../../api/bibliography';

import { kEditingCiteIdRegEx } from './cite';

import './cite-completion.css';

export function citationCompletionHandler(ui: EditorUI, server: PandocServer): CompletionHandler<BibliographyEntry> {
  const bibliographyManager = new BibliographyManager(server);
  return {
    id: 'AB9D4F8C-DA00-403A-AB4A-05373906FD8C',

    completions: citationCompletions(ui, bibliographyManager),

    filter: (completions: BibliographyEntry[], _state: EditorState, token: string) =>
      filterCitations(completions, token, bibliographyManager),

    replacement(schema: Schema, entry: BibliographyEntry | null): string | ProsemirrorNode | null {
      if (entry) {
        return entry.source.id;
      } else {
        return null;
      }
    },

    view: {
      component: BibliographySourceView,
      key: entry => entry.source.id,
      width: 480,
      height: 52,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function filterCitations(bibliographyEntries: BibliographyEntry[], token: string, manager: BibliographyManager) {
  if (token.trim().length === 0) {
    return bibliographyEntries;
  }
  return manager.search(token);
}

function citationCompletions(ui: EditorUI, manager: BibliographyManager) {
  return (text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    // return completions if we are inside a cite id mark
    const markType = context.doc.type.schema.marks.cite_id;
    if (!markIsActive(context, markType)) {
      return null;
    }
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
          completions: (state: EditorState) => manager.entries(ui, context.doc),
          decorations:
            token.length === 0
              ? DecorationSet.create(context.doc, [
                  iconAndTextPlaceholderDecoration(
                    context.selection.head,
                    ui.images.search!,
                    ui.context.translateText('search...'),
                  ),
                ])
              : undefined,
        };
      }
    }

    return null;
  };
}

const BibliographySourceView: React.FC<BibliographyEntry> = entry => {
  return (
    <div className={'pm-completion-citation-item'}>
      <div className={'pm-completion-citation-type'}>
        <img className={'pm-block-border-color'} src={entry.image[0]} />
      </div>
      <div className={'pm-completion-citation-summary'}>
        <div className={'pm-completion-citation-source'}>
          <div className={'pm-completion-citation-authors'}>
            @{entry.source.id} - {entry.authorsFormatter(entry.source.author, 32)}
          </div>
          <div className={'pm-completion-citation-issuedate'}>{entry.issuedDateFormatter(entry.source.issued)}</div>
        </div>
        <div className={'pm-completion-citation-title'}>{entry.source.title}</div>
      </div>
    </div>
  );
};
