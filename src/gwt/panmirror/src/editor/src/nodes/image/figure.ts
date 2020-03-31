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

import { Node as ProsemirrorNode, Schema, Fragment } from 'prosemirror-model';
import { Plugin, PluginKey, EditorState, Transaction, NodeSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Transform } from 'prosemirror-transform';

import { findChildrenByType } from 'prosemirror-utils';

import { Extension } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { BaseKey } from '../../api/basekeys';
import { exitNode } from '../../api/command';
import { EditorOptions } from '../../api/options';
import { EditorEvents } from '../../api/events';
import { FixupContext } from '../../api/fixup';
import { isSingleLineHTML } from '../../api/html';
import { getMarkAttrs } from '../../api/mark';
import { PandocToken, PandocTokenType, ProsemirrorWriter, PandocExtensions, kRawBlockContent, kRawBlockFormat, imageAttributesAvailable } from '../../api/pandoc';

import {
  imageAttrsFromDOM,
  imageNodeAttrsSpec,
  imageDOMOutputSpec,
  imagePandocOutputWriter,
  pandocImageHandler,
  imageAttrsFromHTML,
} from './image';
import { ImageNodeView } from './image-view';
import { inlineHTMLIsImage } from './image-util';

import './figure-styles.css';




const plugin = new PluginKey('figure');

const extension = (pandocExtensions: PandocExtensions, options: EditorOptions, ui: EditorUI, events: EditorEvents) : Extension | null => {

  const imageAttr = imageAttributesAvailable(pandocExtensions);

  return {
    nodes: [
      {
        name: 'figure',
        spec: {
          attrs: imageNodeAttrsSpec(true, imageAttr),
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
          writer: imagePandocOutputWriter(true, ui),

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

    fixups: (_schema: Schema) => {
      return [
        (tr: Transaction, context: FixupContext) => {
          if (context === FixupContext.Load) {
            return convertImagesToFigure(tr);
          } else {
            return tr;
          }
        }
      ];
    },

    appendTransaction: (schema: Schema) => {
      return [
        {
          name: 'figure-convert',
          nodeFilter: node => node.type === schema.nodes.image,
          append: convertImagesToFigure
        }
      ];
    },
    
    baseKeys: (schema: Schema) => {
      return [
        { key: BaseKey.Enter, command: exitNode(schema.nodes.figure, -1, false) },
        { key: BaseKey.Backspace, command: deleteCaption() },
      ];
    },

    plugins: (_schema: Schema) => {
      return [
        new Plugin({
          key: plugin,
          props: {
            nodeViews: {
              figure(node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) {
                return new ImageNodeView(node, view, getPos as () => number, ui, events, pandocExtensions);
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

function convertImagesToFigure(tr: Transaction) {

  // create a new transform so we can do position mapping relative
  // to the actions taken here (b/c the transaction might already
  // have other steps so we can't do tr.mapping.map)
  const newActions = new Transform(tr.doc);

  const schema = tr.doc.type.schema;
  const images = findChildrenByType(tr.doc, schema.nodes.image);
  images.forEach(image => {

    // position reflecting steps already taken in this handler
    const mappedPos = newActions.mapping.mapResult(image.pos);

    // process image so long as it wasn't deleted by a previous step
    if (!mappedPos.deleted) {
      
      // resolve image pos 
      const imagePos = tr.doc.resolve(mappedPos.pos);

      // if it's an image in a standalone paragraph, convert it to a figure
      if (imagePos.parent.type === schema.nodes.paragraph && 
          imagePos.parent.childCount === 1) {

        // figure attributes
        const attrs = image.node.attrs;

        // extract linkTo from link mark (if any)
        if (schema.marks.link.isInSet(image.node.marks)) {
          const linkAttrs = getMarkAttrs(tr.doc, { from: image.pos, to: image.pos + image.node.nodeSize}, schema.marks.link);
          if (linkAttrs && linkAttrs.href) {
            attrs.linkTo = linkAttrs.href;
          }
        }

        // figure content
        const content = attrs.alt ? Fragment.from(schema.text(attrs.alt)) : Fragment.empty;
        
        // create figure
        const figure = schema.nodes.figure.createAndFill(attrs, content);

        // replace image with figure
        tr.replaceRangeWith(mappedPos.pos, mappedPos.pos + image.node.nodeSize, figure);
      }
    }
    
  });

  // copy the contents of newActions to the actual transaction
  for (const step of newActions.steps) {
    tr.step(step);
  }

  // return transaction
  return tr;
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

export default extension;
