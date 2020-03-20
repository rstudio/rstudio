/*
 * figure.ts
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

import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { Plugin, PluginKey, EditorState, Transaction, NodeSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Extension } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { BaseKey } from '../../api/basekeys';
import { exitNode } from '../../api/command';
import { PandocToken, PandocTokenType, ProsemirrorWriter, PandocExtensions, kRawBlockContent, kRawBlockFormat } from '../../api/pandoc';

import {
  imageAttrsFromDOM,
  imageNodeAttrsSpec,
  imageDOMOutputSpec,
  imagePandocOutputWriter,
  pandocImageHandler,
  imageAttrsFromHTML,
} from './image';
import { ImageNodeView } from './image-view';

import './figure-styles.css';
import { inlineHTMLIsImage } from './image-util';
import { isSingleLineHTML } from '../../api/html';

const plugin = new PluginKey('figure');

const extension = (pandocExtensions: PandocExtensions): Extension | null => {

  const imageAttr = pandocExtensions.link_attributes || pandocExtensions.raw_html;

  return {
    nodes: [
      {
        name: 'figure',
        spec: {
          attrs: imageNodeAttrsSpec(imageAttr),
          content: 'inline*',
          group: 'block',
          draggable: true,
          selectable: true,
          defining: true,
          parseDOM: [
            {
              tag: 'figure',
              contentElement: 'figcaption',
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                const img = el.querySelector('img');
                if (img && img.parentNode === dom) {
                  return imageAttrsFromDOM(img, imageAttr);
                } else {
                  return {
                    src: null,
                    title: null,
                  };
                }
              },
            },
          ],
          toDOM(node: ProsemirrorNode) {
            return ['figure', imageDOMOutputSpec(node, imageAttr), ['figcaption', { class: 'pm-figcaption' }, 0]];
          },
        },
        pandoc: {
          writer: imagePandocOutputWriter(true, pandocExtensions),

          // intercept  paragraphs with a single image and process them as figures
          blockReader: (schema: Schema, tok: PandocToken, writer: ProsemirrorWriter) => {

            // helper to process html image
            const handleHTMLImage = (html: string) => {
              const attrs = imageAttrsFromHTML(html);
              if (attrs) {
                writer.addNode(schema.nodes.figure, attrs, []);
                return true;
              } else {
                return false;
              }
            };

            // unroll figure from paragraph with single image
            if (isParaWrappingFigure(tok)) {
              const handler = pandocImageHandler(true, imageAttr)(schema);
              handler(writer, tok.c[0]);
              return true;
            // unroll figure from html RawBlock with single <img> tag
            } else if (isHTMLImageBlock(tok)) {
              return handleHTMLImage(tok.c[kRawBlockContent]);
            } else {
              return false;
            }
          },
        },
      },
    ],

    baseKeys: (schema: Schema) => {
      return [
        { key: BaseKey.Enter, command: exitNode(schema.nodes.figure, -1, false) },
        { key: BaseKey.Backspace, command: deleteCaption() },
      ];
    },

    plugins: (_schema: Schema, ui: EditorUI) => {
      return [
        new Plugin({
          key: plugin,
          props: {
            nodeViews: {
              figure(node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) {
                return new ImageNodeView(node, view, getPos as () => number, ui, imageAttr);
              },
            },
          },
        }),
      ];
    },
  };
};

export function deleteCaption() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // must be a selection within an empty figure
    const schema = state.schema;
    const { $head } = state.selection;
    if ($head.parent.type !== schema.nodes.figure || $head.parent.childCount !== 0) {
      return false;
    }

    if (dispatch) {
      // set a node selection for the figure
      const tr = state.tr;
      tr.setSelection(NodeSelection.create(tr.doc, $head.pos - 1));
      dispatch(tr);
    }

    return true;
  };
}

function isParaWrappingFigure(tok: PandocToken) {
  return isSingleChildParagraph(tok) && tok.c[0].t === PandocTokenType.Image;
}

function isHTMLImageBlock(tok: PandocToken) {
  if (tok.t === PandocTokenType.RawBlock) {
    const format = tok.c[kRawBlockFormat];
    const text = tok.c[kRawBlockContent] as string;
    return format === 'html' && isSingleLineHTML(text) && inlineHTMLIsImage(text);
  } else {
    return false;
  }
}

function isSingleChildParagraph(tok: PandocToken) {
  return tok.t === PandocTokenType.Para && 
         tok.c &&
         tok.c.length === 1;
}

function isHTMLImage(tok: PandocToken) {
  if (tok.t === PandocTokenType.RawInline) {
    return tok.c[0] === 'html' && inlineHTMLIsImage(tok.c[1]);
  } else {
    return false;
  }
}

export default extension;
