/*
 * xref.ts
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

import { Schema, Node as ProsemirrorNode, Mark, Fragment } from "prosemirror-model";
import { EditorState, Transaction } from "prosemirror-state";
import { toggleMark } from "prosemirror-commands";
import { InputRule } from "prosemirror-inputrules";

import { setTextSelection, findChildren } from "prosemirror-utils";

import { Extension } from "../api/extension";
import { PandocExtensions, PandocOutput } from "../api/pandoc";
import { PandocCapabilities } from "../api/pandoc_capabilities";
import { EditorOptions } from "../api/options";
import { EditorUI } from "../api/ui";
import { detectAndApplyMarks, removeInvalidatedMarks } from "../api/mark";
import { MarkTransaction } from "../api/transaction";
import { FixupContext } from "../api/fixup";
import { ProsemirrorCommand, EditorCommandId } from "../api/command";
import { canInsertNode } from "../api/node";

const kRefRegEx = /@ref\([A-Za-z0-9:-]*\)/g;

const extension = (
  _pandocExtensions: PandocExtensions, 
  _pandocCapabilities: PandocCapabilities, 
  _ui: EditorUI, 
  options: EditorOptions): Extension | null => {

  if (!options.rmdBookdownXRef) {
    return null;
  }

  return {

    marks: [
      {
        name: 'xref',
        spec: {
          inclusive: false,
          attrs: {},
          parseDOM: [
            {
              tag: "span[class*='xref']",
            },
          ],
          toDOM(_mark: Mark) {
            return ['span', { class: 'xref pm-link-text-color pm-fixedwidth-font' } ];
          },
        },
        pandoc: {
          readers: [],
          writer: {
            priority: 17,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeInlines(parent);
            },
          },
        },
      },
    ],

    fixups: (schema: Schema) => {
      return [
        (tr: Transaction, context: FixupContext) => {
          if (context === FixupContext.Load) {
            const predicate = (node: ProsemirrorNode) => {
              return node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.quoted);
            };
            findChildren(tr.doc, predicate).forEach(nodeWithPos => {
              const { node, pos } = nodeWithPos;
              detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kRefRegEx, node.type.schema.marks.xref);
            });
          }
          return tr;
        }
      ];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'xref-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.xref),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            removeInvalidatedMarks(tr, node, pos, kRefRegEx, node.type.schema.marks.xref);
            detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kRefRegEx, node.type.schema.marks.xref);
          },
        }
      ];
    },

    inputRules: (_schema: Schema) => {
      return [
        new InputRule(/\\?@ref\($/, (state: EditorState, match: string[], start: number, end: number) => {
          const tr = state.tr;
          tr.delete(start, end);
          insertRef(tr);
          return tr;
        }),
      ];
    },

    commands: (schema: Schema) => {
      if (options.rmdBookdownXRefCommand) {
        return [
          new ProsemirrorCommand(
            EditorCommandId.CrossReference, [],
            (state: EditorState, dispatch?: (tr: Transaction<any>) => void) => {
              // enable/disable command
              if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.xref)(state)) {
                return false;
              }
              if (dispatch) {
                const tr = state.tr;
                insertRef(tr);
                dispatch(tr);
              }
              return true;
            }
          )
        ];
      } else {
        return [];
      }
      
    },
    

  };
};

function insertRef(tr: Transaction) {
  const schema = tr.doc.type.schema;
  const selection = tr.selection;
  const refText = "@ref()";
  tr.replaceSelectionWith(schema.text(refText));
  setTextSelection(tr.mapping.map(selection.head) - 1)(tr);
}

export default extension;