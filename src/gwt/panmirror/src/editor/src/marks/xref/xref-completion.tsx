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
import { XRef } from '../../api/xref';

import './xref-completion.css';

export function xrefCompletionHandler(ui: EditorUI): CompletionHandler<XRef> {
  return {
    completions: xrefCompletions(ui),

    replacement(schema: Schema, xref: XRef | null): string | ProsemirrorNode | null {
      return null;
    },

    view: {
      component: XRefView,
      key: xref => xref.key,
      width: 200,
      hideNoResults: true,
    },
  };
}

const kMaxXRefCompletions = 20;

function xrefCompletions(ui: EditorUI) {
  return (text: string, context: EditorState | Transaction): CompletionResult<XRef> | null => {


    return null;
  };
}

const XRefView: React.FC<XRef> = xref => {
  return (
    <div className={'pm-completion-xref'}>
      {xref.key}
    </div>
  );
};

