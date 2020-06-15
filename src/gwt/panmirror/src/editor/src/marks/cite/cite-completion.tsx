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

// TODO: Could we adorn citations that don't resolve by id with a warning decoration as an aide to user
// TODO: Replace with shared regex / pattern with cite.ts
// TODO: completions are not being called for special marks like [@] - I think we should call this
// which would render the initial set of options

import { Selection, EditorState, Transaction } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { PandocServer } from '../../api/pandoc';

import React from 'react';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';

import {
  bibliographyFilesFromDoc,
  BibliographyFiles,
  BibliographyEntry,
  BibliographyManager,
} from '../../api/bibliography';

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
        // TODO: Probably need to replace the whole citation (currently leaving selection inside the citation)
        // TODO: Frequenly mangles the cite marks outside cite_id
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
      height: 70,
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

      // scan for completions that match the prefix (truncate as necessary)
      const bibliographyFiles: BibliographyFiles | null = bibliographyFilesFromDoc(context.doc, ui.context);
      if (bibliographyFiles) {
        return {
          token: query,
          pos,
          completions: (state: EditorState) => manager.entries(bibliographyFiles),
        };
      }
    }
    return null;
  };
}

const BibliographySourceView: React.FC<BibliographyEntry> = entry => {
  // TODO: ensure that this is sanitized html
  const safeHtml = () => ({ __html: entry.html });
  return <div className={'pm-completion-citation-item'} dangerouslySetInnerHTML={safeHtml()} />;
};
