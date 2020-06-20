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
import { DecorationSet } from 'prosemirror-view';

import React from 'react';

import Fuse from 'fuse.js';
import uniqby from 'lodash.uniqby';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { XRef, XRefServer, xrefKey } from '../../api/xref';
import { markIsActive } from '../../api/mark';
import { searchPlaceholderDecoration } from '../../api/placeholder';
import { CompletionItemView } from '../../api/widgets/completion';

import './xref-completion.css';


const kMaxResults = 20;

export function xrefCompletionHandler(ui: EditorUI, server: XRefServer): CompletionHandler<XRef> {

  const index = new FuseIndex();

  return {

    id: 'BAACC160-56BE-4322-B079-54477A880623',

    enabled: (context: EditorState | Transaction) => {
      return markIsActive(context, context.doc.type.schema.marks.xref);
    },

    completions: xrefCompletions(ui, server, index),

    filter: (completions: XRef[], _state: EditorState, token: string) => {
      if (token.length > 0) {
        return index.search(token, kMaxResults);
      } else {
        return completions.slice(0, kMaxResults);
      }
    },

    replacement(schema: Schema, xref: XRef | null): string | ProsemirrorNode | null {
      if (xref) {
        return xrefKey(xref);
      } else {
        return null;
      }
    },

    view: {
      component: xrefView(ui),
      key: xref => xrefKey(xref),
      height: 52,
      width: 350,
      maxVisible: 5,
      hideNoResults: true
    },
  };
}

class FuseIndex {

  private fuse: Fuse<XRef, Fuse.IFuseOptions<XRef>>;

  private keys: Fuse.FuseOptionKeyObject[] = [
    { name: 'type', weight: 10 },
    { name: 'id', weight: 10 },
    { name: 'title', weight: 10 },
  ];

  constructor() {
    this.fuse = this.createIndex([]);
  }

  public update(xrefs: XRef[]) {
    this.fuse = this.createIndex(xrefs);
  }

  public search(query: string, limit: number) {
    const options = {
      isCaseSensitive: false,
      shouldSort: true,
      includeMatches: false,
      limit,
      keys: this.keys,
    };
    const results = this.fuse.search(query, options);
    const xrefs = results.map((result: { item: XRef }) => result.item);
    return uniqby(xrefs, xrefKey);
  }

  private createIndex(xrefs: XRef[]) {
    const options = {
      keys: this.keys.map(key => key.name)
    };
    const index = Fuse.createIndex(options.keys, xrefs);
    return new Fuse(xrefs, options, index);
  }
}



function xrefCompletions(ui: EditorUI, server: XRefServer, index: FuseIndex) {
  const kXRefCompletionRegEx = /(@ref\()([A-Za-z0-9:-]*)$/;
  return (text: string, context: EditorState | Transaction): CompletionResult<XRef> | null => {
    const match = text.match(kXRefCompletionRegEx);
    if (match) {
      const pos = context.selection.head - match[2].length;
      const token = match[2];
      return {
        pos,
        offset: -match[1].length,
        token,
        completions: async () => {
          const docPath = ui.context.getDocumentPath();
          if (docPath) {
            const xrefs = await server.indexForFile(docPath);
            index.update(xrefs);
            return xrefs;
          } else {
            index.update([]);
            return Promise.resolve([]);
          }
        },
        decorations: token.length === 0
          ? DecorationSet.create(context.doc, [searchPlaceholderDecoration(pos, ui)])
          : undefined,
      };
    } else {
      return null;
    }
  };
}

function xrefView(ui: EditorUI): React.FC<XRef> {

  return (xref: XRef) => {

    let image = ui.images.omni_insert?.generic!;
    if (xref.type === 'fig') {
      image = ui.prefs.darkMode() ? ui.images.omni_insert?.image_dark! : ui.images.omni_insert?.image!;
    } else if (xref.type === 'tab') {
      image = ui.prefs.darkMode() ? ui.images.omni_insert?.table_dark! : ui.images.omni_insert?.table!;
    } else if (xref.type === 'eq') {
      image = ui.prefs.darkMode() ? ui.images.omni_insert?.math_display_dark! : ui.images.omni_insert?.math_display!;
    }

    const idView = <>
      <div className={'pm-xref-completion-id pm-xref-completion-id-key pm-fixedwidth-font'}>{xrefKey(xref)}</div>
      <div className={'pm-xref-completion-id pm-xref-completion-id-file'}>{xref.file}</div>
    </>;

    return (
      <CompletionItemView
        width={350}
        classes={[]}
        image={image}
        idView={idView}
        title={xref.title}
      />
    );
  };

}

