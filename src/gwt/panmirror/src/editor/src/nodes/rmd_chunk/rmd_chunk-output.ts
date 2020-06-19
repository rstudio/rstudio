/*
 * rmd_chunk-output.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { DecorationSet, Decoration, EditorView } from 'prosemirror-view';

import { findChildrenByType, setTextSelection } from 'prosemirror-utils';

import { transactionsAreTypingChange, transactionsHaveChange } from '../../api/transaction';
import { EditorUIContext } from '../../api/ui';
import { stripQuotes } from '../../api/text';

const key = new PluginKey<DecorationSet>('rmd-chunk-output');

export class RmdChunkOutputPlugin extends Plugin<DecorationSet> {
  constructor(uiContext: EditorUIContext) {
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }, state: EditorState) => {
          return chunkOutputDecorations(state, uiContext);
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          const transactions = [tr];

          // doc didn't change, return existing decorations
          if (!tr.docChanged) {
            return old.map(tr.mapping, tr.doc);

            // non-typing change, do a full rescan
          } else if (!transactionsAreTypingChange(transactions)) {
            return chunkOutputDecorations(newState, uiContext);

            // change that affects a rmd chunk block, do a full rescan
          } else if (transactionsHaveChange(transactions, oldState, newState, isRmdChunkNode)) {
            return chunkOutputDecorations(newState, uiContext);
          }

          // otherwise return the existing set (mapped)
          else {
            return old.map(tr.mapping, tr.doc);
          }
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }
}

function chunkOutputDecorations(state: EditorState, uiContext: EditorUIContext) {
  // find all rmd code chunks with knitr::include_graphics
  const decorations: Decoration[] = [];
  findChildrenByType(state.doc, state.schema.nodes.rmd_chunk).forEach(rmdChunk => {
    // look for a line with knitr::include_graphics
    const decoration = Decoration.widget(
      rmdChunk.pos + rmdChunk.node.nodeSize,
      (view: EditorView, getPos: () => number) => {
        const container = window.document.createElement('div');
        container.innerText = "Output here";
        return container;
      },
      { chunkId: "foo" },
    );
    decorations.push(decoration);
  });

  // return decorations
  return DecorationSet.create(state.doc, decorations);
}

function isRmdChunkNode(node: ProsemirrorNode) {
  return node.type === node.type.schema.nodes.rmd_chunk;
}
