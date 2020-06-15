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

// INVOCATION
// TODO: Replace with shared regex / pattern with cite.ts
// TODO: completions are not being called for special marks like [@] - I think we should call this
// which would render the initial set of options

// MARK INSERTION
// TODO: insert complete mark correctly (currently losing cite marks)
// TODO: frequent mark mangling - see if this is still happening and why

// BIBLIOGRAPHY
// TODO: Shared bibliography (set site level for books) -- distill, bookdown, etc...
// Sniff project provide shared bibliography as ui context
// TODO: Read references out of inline reference blocks and merge with bibliography references
// TODO: Read bibliography files out of ANY yaml node
// TODD: Need to filter entries to not include any duplicates

// UI
// TODO: Show preview for citation when mouseover (like inline math)
//        - would be nice if you could follow DOI to article when previewing
// TODO: search doi, url, or crossref (data cite [hipster], pubmed?)

// FUTURE
// TODO: Full insert reference panel including preview
// TODO: Could we adorn citations that don't resolve by id with a warning decoration as an aide to user

import { EditorState, Transaction } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { PandocServer } from '../../api/pandoc';

import React from 'react';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';

import { BibliographyEntry, BibliographyManager, BibliographyAuthor, BibliographyDate } from '../../api/bibliography';

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
        // TODO: need to deal with @/-@ (preserve what user typed)
        const mark = schema.marks.cite_id.create({});
        return schema.text(`@${entry.source.id}`, [mark]);
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

const kCitationCompletionRegex = `\\[(.* -?@|-?@)(\\w[\\w:\\.#\\$%&\\-\\+\\?<>~/]*)`;

function filterCitations(bibliographyEntries: BibliographyEntry[], token: string, manager: BibliographyManager) {
  if (token.trim().length === 0) {
    return bibliographyEntries;
  }
  return manager.search(token);
}

function citationCompletions(ui: EditorUI, manager: BibliographyManager) {
  return (text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    // look for requisite text sequence
    const match = text.match(kCitationCompletionRegex);
    if (match) {
      // determine insert position and prefix to search for
      const prefix = match[1];
      const query = match[2];
      const pos = context.selection.head - (query.length + prefix.length);

      return {
        token: query,
        pos,
        completions: (state: EditorState) => manager.entries(ui, context.doc),
      };
    }
    return null;
  };
}

// TODO: Needs to support localization of the final format (e.g. X and Y)
// TODO: Need to truncate long author string with et. al.
const kEtAl = 'et al.';
function formatAuthors(authors: BibliographyAuthor[], maxLength: number): string {
  let formattedAuthorString = '';
  authors
    .map(author => {
      if (author.given.length > 0) {
        // Family and Given name
        return `${author.family}, ${author.given.substring(0, 1)}`;
      } else {
        // Family name only
        return `${author.family}`;
      }
    })
    .every((value, index, values) => {
      // If we'll exceed the maximum length, append 'et al' and stop
      if (formattedAuthorString.length + value.length > maxLength) {
        formattedAuthorString = `${formattedAuthorString}, ${kEtAl}`;
        return false;
      }

      if (index === 0) {
        // The first author
        formattedAuthorString = value;
      } else if (values.length > 1 && index === values.length - 1) {
        // The last author
        formattedAuthorString = `${formattedAuthorString}, and ${value}`;
      } else {
        // Middle authors
        formattedAuthorString = `${formattedAuthorString}, ${value}`;
      }
      return true;
    });
  return formattedAuthorString;
}

function formatIssuedDate(date: BibliographyDate): string {
  // No issue date for this
  if (!date) {
    return '';
  }

  // TODO: Finalize formats
  switch (date['date-parts'].length) {
    // There is a date range
    case 2:
      return `${date['date-parts'][0]}-${date['date-parts'][1]}`;
    // Only a single date
    case 1:
      return `${date['date-parts'][0]}`;

    // Seems like a malformed date :(
    case 0:
    default:
      return '';
  }
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
            @{entry.source.id} - {formatAuthors(entry.source.author, 32)}
          </div>
          <div className={'pm-completion-citation-issuedate'}>{formatIssuedDate(entry.source.issued)}</div>
        </div>
        <div className={'pm-completion-citation-title'}>{entry.source.title}</div>
      </div>
    </div>
  );
};
