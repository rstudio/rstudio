/*
 * math.ts
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

import { Node as ProsemirrorNode, Schema, Mark, Fragment, Slice } from 'prosemirror-model';
import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { InputRule } from 'prosemirror-inputrules';

import { Extension, extensionIfEnabled } from '../../api/extension';
import { PandocTokenType, PandocToken, PandocOutput } from '../../api/pandoc';
import { BaseKey } from '../../api/basekeys';
import { markIsActive, getMarkAttrs } from '../../api/mark';

import { InsertInlineMathCommand, InsertDisplayMathCommand, insertMath } from './math-commands';
import { mathAppendMarkTransaction } from './math-transaction';
import { mathHighlightPlugin } from './math-highlight';

import './math-styles.css';

export enum MathType {
  Inline = 'InlineMath',
  Display = 'DisplayMath',
}

const MATH_TYPE = 0;
const MATH_CONTENT = 1;

const extension: Extension = {
  marks: [
    {
      name: 'math',
      noInputRules: true,
      spec: {
        attrs: {
          type: {},
        },
        inclusive: false,
        excludes: "_",
        parseDOM: [
          {
            tag: "span[class*='math']",
            getAttrs(dom: Node | string) {
              const el = dom as Element;
              return {
                type: el.getAttribute('data-type'),
              };
            },
            preserveWhitespace: 'full',
          },
        ],

        toDOM(mark: Mark) {
          return [
            'span',
            { class: 'math pm-fixedwidth-font pm-light-text-color', 'data-type': mark.attrs.type, spellcheck: 'false' },
          ];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Math,
            mark: 'math',
            getAttrs: (tok: PandocToken) => {
              return {
                type: tok.c[MATH_TYPE].t,
              };
            },
            getText: (tok: PandocToken) => {
              const delimter = delimiterForType(tok.c[MATH_TYPE].t);
              return delimter + tok.c[MATH_CONTENT] + delimter;
            },
          },
        ],
        writer: {
          priority: 20,
          write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
            // collect math content
            let math = '';
            parent.forEach((node: ProsemirrorNode) => (math = math + node.textContent));
            // strip delimiter
            const delimiter = delimiterForType(mark.attrs.type);
            math = math.substr(delimiter.length, math.length - 2 * delimiter.length);

            // if it's just whitespace then it's not actually math (we allow this state
            // in the editor because it's the natural starting place for new equations)
            if (math.trim().length === 0) {
              output.writeText(delimiter + math + delimiter);
            } else {
              output.writeToken(PandocTokenType.Math, () => {
                // write type
                output.writeToken(
                  mark.attrs.type === MathType.Inline ? PandocTokenType.InlineMath : PandocTokenType.DisplayMath,
                );
                output.write(math);
              });
            }
          },
        },
      },
    },
  ],

  baseKeys: (_schema: Schema) => {
    return [{ key: BaseKey.Enter, command: displayMathNewline() }];
  },

  inputRules: (_schema: Schema) => {
    return [
      new InputRule(/^\$\$$/, (state: EditorState, match: string[], start: number, end: number) => {
        const tr = state.tr;
        tr.delete(start, end);
        insertMath(tr.selection, MathType.Display, tr);
        return tr;
      }),
    ];
  },

  commands: (_schema: Schema) => {
    return [new InsertInlineMathCommand(), new InsertDisplayMathCommand()];
  },

  appendMarkTransaction: (_schema: Schema) => {
    return [mathAppendMarkTransaction()];
  },

  plugins: (schema: Schema) => {
    return [
      new Plugin({
        key: new PluginKey('math'),
        props: {
          // paste plain text into math blocks
          handlePaste: handlePasteIntoMath(),
        },
      }),
      mathHighlightPlugin(schema),
    ];
  },
};

function handlePasteIntoMath() {
  return (view: EditorView, _event: Event, slice: Slice) => {
    const schema = view.state.schema;
    if (markIsActive(view.state, schema.marks.math)) {
      const tr = view.state.tr;
      let math = '';
      slice.content.forEach((node: ProsemirrorNode) => (math = math + node.textContent));
      tr.replaceSelectionWith(schema.text(math));
      view.dispatch(tr);
      return true;
    } else {
      return false;
    }
  };
}

// enable insertion of newlines
function displayMathNewline() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // display math mark must be active

    if (!displayMathIsActive(state)) {
      return false;
    }

    // insert a newline
    if (dispatch) {
      const tr = state.tr;
      tr.insertText('\n');
      dispatch(tr);
    }
    return true;
  };
}

function displayMathIsActive(state: EditorState) {
  const schema = state.schema;
  return (
    markIsActive(state, schema.marks.math) &&
    getMarkAttrs(state.doc, state.selection, schema.marks.math).type === MathType.Display
  );
}

export function delimiterForType(type: string) {
  if (type === MathType.Inline) {
    return '$';
  } else {
    return '$$';
  }
}

export default extensionIfEnabled(extension, [
  'tex_math_dollars',
  'tex_math_single_backslash',
  'tex_math_double_backslash',
]);
