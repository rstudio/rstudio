/*
 * xref.ts
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

import { Schema, Node as ProsemirrorNode, Mark, Fragment } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';
import { InputRule } from 'prosemirror-inputrules';
import { Transform } from 'prosemirror-transform';

import { setTextSelection, findChildren, findChildrenByMark } from 'prosemirror-utils';

import { Extension, ExtensionContext } from '../api/extension';
import { detectAndApplyMarks, removeInvalidatedMarks, getMarkRange } from '../api/mark';
import { MarkTransaction, trTransform } from '../api/transaction';
import { FixupContext } from '../api/fixup';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { canInsertNode } from '../api/node';
import { fragmentText } from '../api/fragment';
import { PandocOutput } from '../api/pandoc';
import { OmniInsertGroup } from '../api/omni_insert';

const kRefRegExDetectAndApply = /(?:^|[^`])(\\?@ref\([A-Za-z0-9:-]*\))/g;

const extension = (context: ExtensionContext): Extension | null => {

  const { pandocExtensions, format, ui } = context;

  if (!format.rmdExtensions.bookdownXRef) {
    return null;
  }

  return {
    marks: [
      {
        name: 'xref',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          attrs: {},
          parseDOM: [
            {
              tag: "span[class*='xref']",
            },
          ],
          toDOM(_mark: Mark) {
            return ['span', { class: 'xref pm-link-text-color pm-fixedwidth-font' }];
          },
        },
        pandoc: {
          readers: [],
          writer: {
            priority: 17,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              // alias xref (may need to transform it to deal with \ prefix)
              let xref = parent;

              // if it starts with a \ then don't write the slash (pandoc will
              // either create one automatically or we'll write one explicitly
              // if pandoc won't b/c it doesn't have all_symbols_escapable)
              if (fragmentText(xref).startsWith('\\')) {
                xref = xref.cut(1, xref.size);
              }

              // if all symbols aren't escapable then we need an explicit \
              // (because pandoc won't automatically escape the \)
              if (!pandocExtensions.all_symbols_escapable) {
                output.writeRawMarkdown('\\');
              }

              // write xref
              output.writeInlines(xref);
            },
          },
        },
      },
    ],

    fixups: (schema: Schema) => {
      return [
        (tr: Transaction, fixupContext: FixupContext) => {
          if (fixupContext === FixupContext.Load) {
            // apply marks
            const markType = schema.marks.xref;
            const predicate = (node: ProsemirrorNode) => {
              return node.isTextblock && node.type.allowsMarkType(markType);
            };
            const markTr = new MarkTransaction(tr);
            findChildren(tr.doc, predicate).forEach(nodeWithPos => {
              const { pos } = nodeWithPos;
              detectAndApplyMarks(
                markTr,
                tr.doc.nodeAt(pos)!,
                pos,
                kRefRegExDetectAndApply,
                markType,
                () => ({}),
                match => match[1],
              );
            });

            // remove leading \ as necessary (this would occur if the underlying format includes
            // a \@ref and doesn't have all_symbols_escapable, e.g. blackfriday)
            trTransform(tr, stripRefBackslashTransform);
          }
          return tr;
        },
      ];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'xref-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.xref),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            removeInvalidatedMarks(tr, node, pos, kRefRegExDetectAndApply, node.type.schema.marks.xref);
            detectAndApplyMarks(
              tr,
              tr.doc.nodeAt(pos)!,
              pos,
              kRefRegExDetectAndApply,
              node.type.schema.marks.xref,
              () => ({}),
              match => match[1],
            );
          },
        },
      ];
    },

    inputRules: (_schema: Schema) => {
      return [
        // recoginize new ref
        new InputRule(/(^|[^`])(\\?@ref\()$/, (state: EditorState, match: string[], start: number, end: number) => {
          const tr = state.tr;
          tr.delete(start + match[1].length, end);
          insertRef(tr);
          return tr;
        }),
      ];
    },

    commands: (schema: Schema) => {
      if (format.rmdExtensions.bookdownXRefUI) {
        return [
          new ProsemirrorCommand(
            EditorCommandId.CrossReference,
            [],
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
            },
            {
              name: ui.context.translateText('Cross Reference'),
              description: ui.context.translateText('Reference to related content'),
              group: OmniInsertGroup.References,
              priority: 0,
              image: () => ui.prefs.darkMode() ? ui.images.omni_insert!.cross_reference_dark! : ui.images.omni_insert!.cross_reference!,
            },
          ),
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
  const refText = '@ref()';
  tr.replaceSelectionWith(schema.text(refText), false);
  setTextSelection(tr.mapping.map(selection.head) - 1)(tr);
}

function stripRefBackslashTransform(tr: Transform) {
  const markType = tr.doc.type.schema.marks.xref;
  findChildrenByMark(tr.doc, markType).forEach(markedNode => {
    const pos = tr.mapping.map(markedNode.pos);
    if (markType.isInSet(markedNode.node.marks)) {
      const markRange = getMarkRange(tr.doc.resolve(pos), markType);
      if (markRange) {
        const text = tr.doc.textBetween(markRange.from, markRange.to);
        if (text.startsWith('\\')) {
          tr.deleteRange(markRange.from, markRange.from + 1);
        }
      }
    }
  });
}

export default extension;
