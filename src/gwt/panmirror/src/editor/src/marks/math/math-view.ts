/*
 * math-viewts
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

import { Plugin, PluginKey, EditorState, Transaction } from "prosemirror-state";
import { Schema } from "prosemirror-model";
import { DecorationSet, EditorView, Decoration } from "prosemirror-view";

import { findChildrenByMark, setTextSelection } from "prosemirror-utils";
import { getMarkRange, getMarkAttrs } from "../../api/mark";
import { AddMarkStep, RemoveMarkStep } from "prosemirror-transform";
import { EditorMath } from "../../api/math";
import { EditorUI } from "../../api/ui";
import { kSetMarkdownTransaction } from "../../api/transaction";

import { MathType } from "./math";

// TODO: consider divs w/ inline-block for highlighting (then can set width to 0 and keep display)
// TODO: arrow up / arrow down (esp. w/ display math)
// TODO: need custom up/down arrow handlers for display math
// TODO: why don't up/down arrow sometimes not work when editing math? (could be result of display: none)

export function mathViewPlugin(schema: Schema, ui: EditorUI, math: EditorMath) {


  const key = new PluginKey<DecorationSet>('math-view');

  function decorationsForDoc(state: EditorState) {

    const decorations: Decoration[] = [];
    findChildrenByMark(state.doc, schema.marks.math, true).forEach(markedNode => {
      // get mark range and attributes
      const range = getMarkRange(state.doc.resolve(markedNode.pos), schema.marks.math) as { from: number, to: number };
      const attrs = getMarkAttrs(state.doc, range, schema.marks.math);

      // if the selection isn't in the mark, then show the preview
      if (state.selection.from < range.from || state.selection.from >= range.to) {
        // get the math text
        const mathText = state.doc.textBetween(range.from, range.to);

        // hide the code
        decorations.push(Decoration.inline(range.from, range.to, { style: "display: none;" }));

        // show a math preview
        decorations.push(Decoration.widget(
          range.from,
          (view: EditorView, getPos: () => number) => {
            const mathjaxDiv = window.document.createElement('div');
            mathjaxDiv.classList.add('pm-math-mathjax');
            // text selection 'within' code for clicks on the preview image
            mathjaxDiv.onclick = () => {
              const tr = view.state.tr;
              let pos = getPos();
              if (attrs.type === MathType.Display) {
                // set position to first non $, non whitespace character
                const match = mathText.match(/^[$\s]+/);
                if (match) {
                  pos += match[0].length;
                }
              } else {
                // set position to the middle of the equation
                pos = pos + mathText.length / 2;
              }
              setTextSelection(pos)(tr);
              view.dispatch(tr);
              view.focus();
            };
            math.typeset(mathjaxDiv, mathText, ui.context.isActiveTab());
            return mathjaxDiv;
          },
          { key: mathText },
        ));
      }
    });

    return DecorationSet.create(state.doc, decorations);
  }

  return new Plugin<DecorationSet>({
    key,

    state: {
      init(_config: { [key: string]: any }, instance: EditorState) {
        return decorationsForDoc(instance);
      },

      apply(tr: Transaction, set: DecorationSet, oldState: EditorState, newState: EditorState) {

        // replacing the entire editor triggers decorations
        if (tr.getMeta(kSetMarkdownTransaction)) {

          return decorationsForDoc(newState);

          // if one of the steps added or removed a mark of our type then rescan the doc.
        } else if (
          tr.steps.some(
            step =>
              (step instanceof AddMarkStep && (step as any).mark.type === schema.marks.math) ||
              (step instanceof RemoveMarkStep && (step as any).mark.type === schema.marks.math),
          )
        ) {

          return decorationsForDoc(newState);

          // if the previous or current state has an active math mark, then rescan
        } else if (getMarkRange(oldState.selection.$from, schema.marks.math) ||
          getMarkRange(newState.selection.$from, schema.marks.math)) {

          return decorationsForDoc(newState);

          // incremental scanning based on presence of mark in changed regions
        } else {

          // adjust decoration positions to changes made by the transaction (decorations that apply
          // to removed chunks of content will be removed by this)
          return set.map(tr.mapping, tr.doc);

        }

      },
    },

    props: {
      decorations(state: EditorState) {
        return key.getState(state);
      },
    },

    appendTransaction: (_transactions: Transaction[], oldState: EditorState, newState: EditorState) => {
      // did we enter math view on the last transaction?
      const mathRange = getMarkRange(newState.selection.$from, schema.marks.math);
      if (mathRange) {
        if (!getMarkRange(oldState.selection.$from, schema.marks.math)) {

          // get math text
          const mathText = newState.doc.textBetween(mathRange.from, mathRange.to);

          // transaction to set the selection
          const tr = newState.tr;

          // old selection just to the left -- set selection at beginning of the math
          if (mathRange.from === (oldState.selection.from + 1)) {

            const match = mathText.match(/^[$\s]+/);
            if (match) {
              setTextSelection(mathRange.from + match[0].length)(tr);
            }

            // old selection just to the right -- set selection at the end of the math
          } else if (mathRange.to === oldState.selection.from) {

            const match = mathText.match(/[$\s]+$/);
            if (match) {
              setTextSelection(mathRange.to - match[0].length)(tr);
            }
          }

          return tr;
        }

        // not currently in a math mark
      } else {

        // did we end up just to the right of math? if so check for navigation from a distance
        // (would imply an up/down arrow or perhaps a mouse click)
        const prevMathRange = getMarkRange(newState.doc.resolve(newState.selection.from - 1), schema.marks.math);
        if (prevMathRange) {
          // if the selection came from afar then treat it as an actual selection
          const delta = oldState.selection.from - newState.selection.from;

          if (Math.abs(delta) > 2) {

            const tr = newState.tr;

            const mathText = newState.doc.textBetween(prevMathRange.from, prevMathRange.to);
            const attrs = getMarkAttrs(newState.doc, prevMathRange, schema.marks.math);

            if (attrs.type === MathType.Inline) {
              setTextSelection(prevMathRange.from + (mathText.length / 2))(tr);
            } else {
              const match = mathText.match(/^[$\s]+/);
              if (match) {
                setTextSelection(prevMathRange.from + match[0].length)(tr);
              }
            }

            return tr;
          }


        }

      }

      return null;
    }
  });
}
