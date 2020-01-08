import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { Plugin, PluginKey, EditorState, Transaction, NodeSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Extension } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { BaseKey } from '../../api/basekeys';
import { exitNode } from '../../api/command';
import { PandocToken, PandocTokenType, ProsemirrorWriter, PandocExtensions } from '../../api/pandoc';

import {
  imageAttrsFromDOM,
  imageNodeAttrsSpec,
  imageDOMOutputSpec,
  imagePandocOutputWriter,
  pandocImageHandler,
} from './image';
import { FigureNodeView } from './figure-view';

import './figure-styles.css';

const plugin = new PluginKey('figure');

const extension = (pandocExtensions: PandocExtensions): Extension => {
  const imageAttr = pandocExtensions.link_attributes;

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
          writer: imagePandocOutputWriter(true, imageAttr),

          // intercept paragraphs with a single image and process them as figures
          blockReader: (schema: Schema, tok: PandocToken, writer: ProsemirrorWriter) => {
            // unroll figures from paragraphs
            if (isParaWrappingFigure(tok)) {
              const handler = pandocImageHandler(true, imageAttr)(schema);
              handler(writer, tok.c[0]);
              return true;
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
                return new FigureNodeView(node, view, getPos as () => number, ui.dialogs.editImage, imageAttr);
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
  return (
    tok.t === PandocTokenType.Para && // is a paragraph
    tok.c &&
    tok.c.length === 1 && // which has 1 child
    tok.c[0].t === PandocTokenType.Image
  ); // of type image
}

export default extension;
