/*
 * image.ts
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

import { Node as ProsemirrorNode, Schema, DOMOutputSpec } from 'prosemirror-model';
import { EditorState, NodeSelection, Transaction, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { Extension } from '../../api/extension';
import { canInsertNode } from '../../api/node';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrReadAST } from '../../api/pandoc_attr';
import {
  PandocOutput,
  PandocTokenType,
  ProsemirrorWriter,
  PandocToken,
  tokensCollectText,
  PandocExtensions,
} from '../../api/pandoc';
import { EditorUI } from '../../api/ui';

import { imageDialog } from './image-dialog';
import { imageDrop } from './image-events';
import { ImageNodeView } from './image-view';
import { selectionIsImageNode } from '../../api/selection';

const TARGET_URL = 0;
const TARGET_TITLE = 1;

const IMAGE_ATTR = 0;
const IMAGE_ALT = 1;
const IMAGE_TARGET = 2;

const plugin = new PluginKey('image');

const extension = (pandocExtensions: PandocExtensions): Extension => {
  const imageAttr = pandocExtensions.link_attributes;

  return {
    nodes: [
      {
        name: 'image',
        spec: {
          inline: true,
          attrs: imageNodeAttrsSpec(imageAttr),
          group: 'inline',
          draggable: true,
          parseDOM: [
            {
              tag: 'img[src]',
              getAttrs(dom: Node | string) {
                return imageAttrsFromDOM(dom as Element, imageAttr);
              },
            },
          ],
          toDOM(node: ProsemirrorNode) {
            return imageDOMOutputSpec(node, imageAttr);
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Image,
              handler: pandocImageHandler(false, imageAttr),
            },
          ],
          writer: imagePandocOutputWriter(false, imageAttr),
        },
      },
    ],

    commands: (_schema: Schema, ui: EditorUI) => {
      return [new ProsemirrorCommand(EditorCommandId.Image, ['Shift-Mod-i'], imageCommand(ui, imageAttr))];
    },

    plugins: (schema: Schema, ui: EditorUI) => {
      return [
        new Plugin({
          key: plugin,
          props: {
            nodeViews: {
              image(node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) {
                return new ImageNodeView(node, view, getPos as () => number, ui, imageAttr);
              },
            },
            handleDOMEvents: {
              drop: imageDrop(schema.nodes.image),
            },
          },
        }),
      ];
    },
  };
};

export function pandocImageHandler(figure: boolean, imageAttributes: boolean) {
  return (schema: Schema) => (writer: ProsemirrorWriter, tok: PandocToken) => {
    // get attributes
    const target = tok.c[IMAGE_TARGET];
    const attrs = {
      src: target[TARGET_URL],
      title: readPandocTitle(target[TARGET_TITLE]),
      alt: '',
      ...(imageAttributes ? pandocAttrReadAST(tok, IMAGE_ATTR) : {}),
    };

    // add alt as plain text if it's not a figure
    if (!figure) {
      attrs.alt = tokensCollectText(tok.c[IMAGE_ALT]);
    }

    // read image and (if appropriate) children
    writer.openNode(figure ? schema.nodes.figure : schema.nodes.image, attrs);
    if (figure) {
      writer.writeTokens(tok.c[IMAGE_ALT]);
    }
    writer.closeNode();
  };
}

export function imagePandocOutputWriter(figure: boolean, imageAttributes: boolean) {
  return (output: PandocOutput, node: ProsemirrorNode) => {
    const writer = () => {
      output.writeToken(PandocTokenType.Image, () => {
        if (imageAttributes) {
          output.writeAttr(node.attrs.id, node.attrs.classes, node.attrs.keyvalue);
        } else {
          output.writeAttr();
        }
        output.writeArray(() => {
          if (figure) {
            output.writeInlines(node.content);
          } else {
            output.writeText(node.attrs.alt);
          }
        });
        output.write([node.attrs.src, node.attrs.title || '']);
      });
    };

    if (figure) {
      output.writeToken(PandocTokenType.Para, writer);
    } else {
      writer();
    }
  };
}

export function imageDOMOutputSpec(node: ProsemirrorNode, imageAttributes: boolean): DOMOutputSpec {
  return [
    'img',
    {
      src: node.attrs.src,
      title: node.attrs.title,
      alt: node.attrs.alt || node.textContent,
      ...(imageAttributes ? pandocAttrToDomAttr(node.attrs) : {}),
    },
  ];
}

export function imageNodeAttrsSpec(imageAttributes: boolean) {
  return {
    src: {},
    title: { default: null },
    alt: { default: null },
    ...(imageAttributes ? pandocAttrSpec : {}),
  };
}

export function imageAttrsFromDOM(el: Element, imageAttributes: boolean) {
  const attrs: { [key: string]: string | null } = {
    src: el.getAttribute('src') || null,
    title: el.getAttribute('title') || null,
    alt: el.getAttribute('alt') || null,
  };
  return {
    ...attrs,
    ...(imageAttributes ? pandocAttrParseDom(el, attrs) : {}),
  };
}

function imageCommand(editorUI: EditorUI, imageAttributes: boolean) {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    const schema = state.schema;

    if (!canInsertNode(state, schema.nodes.image) && !canInsertNode(state, schema.nodes.figure)) {
      return false;
    }

    if (dispatch) {
      // see if we are editing an existing node
      let node: ProsemirrorNode | null = null;
      let nodeType = schema.nodes.image;
      if (selectionIsImageNode(schema, state.selection)) {
        node = (state.selection as NodeSelection).node;
        nodeType = node.type;
      }

      // see if we are in an empty paragraph (in that case insert a figure)
      const { $head } = state.selection;
      if ($head.parent.type === schema.nodes.paragraph && $head.parent.childCount === 0) {
        nodeType = schema.nodes.figure;
      }

      // show dialog
      imageDialog(node, nodeType, state, dispatch, view, editorUI, imageAttributes);
    }

    return true;
  };
}

function readPandocTitle(title: string | null) {
  if (title) {
    return title.replace(/^(fig:)/, '');
  } else {
    return title;
  }
}

export default extension;
