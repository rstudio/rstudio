/*
 * xref-completion.tsx
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

import React from 'react';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { XRef, XRefServer } from '../../api/xref';

import './xref-completion.css';
import { markIsActive } from '../../api/mark';

export function xrefCompletionHandler(ui: EditorUI, server: XRefServer): CompletionHandler<XRef> {
  return {

    id: 'BAACC160-56BE-4322-B079-54477A880623',

    enabled: (context: EditorState | Transaction) => {
      return markIsActive(context, context.doc.type.schema.marks.xref);
    },

    completions: xrefCompletions(ui, server),

    filter: (completions: XRef[], state: EditorState, token: string) => {
      return completions.filter(xref => {
        return xref.id.includes(token);
      });
    },

    replacement(schema: Schema, xref: XRef | null): string | ProsemirrorNode | null {
      if (xref) {
        return xref.id;
      } else {
        return null;
      }
    },

    view: {
      component: XRefView,
      key: xref => xref.id,
      width: 200,
      hideNoResults: true
    },
  };
}



function xrefCompletions(ui: EditorUI, server: XRefServer) {
  const kXRefCompletionRegEx = /(@ref\()([A-Za-z0-9:-]*)$/;
  return (text: string, context: EditorState | Transaction): CompletionResult<XRef> | null => {
    const match = text.match(kXRefCompletionRegEx);
    if (match) {
      return {
        pos: context.selection.head - match[2].length,
        offset: -match[1].length,
        token: match[2],
        completions: () => {
          const docPath = ui.context.getDocumentPath();
          return docPath ? server.indexForFile(docPath) : Promise.resolve([]);
        },
      };
    } else {
      return null;
    }
  };
}

const XRefView: React.FC<XRef> = xref => {
  return (
    <div className={'pm-completion-xref'}>
      {xref.id}
    </div>
  );
};

