/*
 * rmd_chunk-image.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

const key = new PluginKey<DecorationSet>('rmd-chunk-image-preview');

export class RmdChunkImagePreviewPlugin extends Plugin<DecorationSet> {
  constructor(uiContext: EditorUIContext) {
  
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }, state: EditorState) => {
          return imagePreviewDecorations(state, uiContext);
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          
          const transactions = [tr];

          // doc didn't change, return existing decorations
          if (!tr.docChanged) {
            return old;

            // non-typing change, do a full rescan
          } else if (!transactionsAreTypingChange(transactions)) {
            return imagePreviewDecorations(newState, uiContext);

            // change that affects a rmd chunk block, do a full rescan
          } else if (transactionsHaveChange(transactions, oldState, newState, isRmdChunkNode)) {
            return imagePreviewDecorations(newState, uiContext);
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


function imagePreviewDecorations(state: EditorState, uiContext: EditorUIContext) {

  // find all rmd code chunks with knitr::include_graphics
  const decorations: Decoration[] = [];
  findChildrenByType(state.doc, state.schema.nodes.rmd_chunk).forEach(rmdChunk => {

    // look for a line with knitr::include_graphics
    const match = rmdChunk.node.textContent.match(/^(knitr::)?include_graphics\((['"])([^\2]+)\2/m);
    if (match) {
      const imagePath = match[3];
      const decoration = Decoration.widget(
        rmdChunk.pos + rmdChunk.node.nodeSize, 
        (view: EditorView, getPos: () => number) => {
          const container = window.document.createElement('div');
          container.style.marginTop = '-1em'; // to bridge back to the codemirror block
                                              // which has a margin-block-end of 1em
          container.classList.add('pm-image-preview');
          container.classList.add('pm-block-border-color');
          const img = window.document.createElement('img');
          img.src = uiContext.mapResourcePath(imagePath);
          img.draggable = false;
          img.onload = () => {
            img.alt = '';
          };
          img.onerror = () => {
            img.alt = ` ${uiContext.translateText('Image not found')}: ${imagePath}`;
          };
          img.onclick = () => {
            const tr = view.state.tr;
            setTextSelection(getPos() - 1)(tr);
            view.dispatch(tr);
          };
          container.append(img);
          return container;
        }, 
        { key: imagePath }
      );
      decorations.push(decoration);
    }
   

  });

  // return decorations
  return DecorationSet.create(state.doc, decorations);
}


function isRmdChunkNode(node: ProsemirrorNode) {
  return node.type === node.type.schema.nodes.rmd_chunk;
}





