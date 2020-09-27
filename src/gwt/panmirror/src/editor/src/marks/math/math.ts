/*
 * math.ts
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

import { Node as ProsemirrorNode, Schema, Mark, Fragment, Slice } from 'prosemirror-model';
import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { InputRule } from 'prosemirror-inputrules';

import { Extension, ExtensionContext } from '../../api/extension';
import { PandocTokenType, PandocToken, PandocOutput } from '../../api/pandoc';
import { BaseKey } from '../../api/basekeys';
import { markIsActive, getMarkAttrs } from '../../api/mark';
import { kCodeText } from '../../api/code';
import { kMathContent, kMathType, delimiterForType, MathType } from '../../api/math';
import { MarkInputRuleFilter } from '../../api/input_rule';

import { InsertInlineMathCommand, InsertDisplayMathCommand, insertMath } from './math-commands';
import { mathAppendMarkTransaction } from './math-transaction';
import { mathHighlightPlugin } from './math-highlight';
import { MathPopupPlugin } from './math-popup';
import { mathViewPlugins } from './math-view';

import './math-styles.css';

const kInlineMathPattern = '\\$[^ ].*?[^\\ ]?\\$';
const kInlineMathRegex = new RegExp(kInlineMathPattern);

const kSingleLineDisplayMathPattern = '\\$\\$[^\n]*?\\$\\$';
const kSingleLineDisplayMathRegex = new RegExp(kSingleLineDisplayMathPattern);

const extension = (context: ExtensionContext): Extension | null => {
  const { pandocExtensions, ui, format, math, events } = context;

  if (!pandocExtensions.tex_math_dollars) {
    return null;
  }

  // special blogdown handling for markdown renderers that don't support math
  const blogdownMathInCode = format.rmdExtensions.blogdownMathInCode;
  const singleLineDisplayMath = blogdownMathInCode;

  return {
    marks: [
      {
        name: 'math',
        noInputRules: true,
        noSpelling: true,
        spec: {
          attrs: {
            type: {},
          },
          inclusive: false,
          excludes: 'formatting',
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
              {
                class: 'math pm-fixedwidth-font pm-light-text-color',
                'data-type': mark.attrs.type,
                spellcheck: 'false',
              },
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
                  type: tok.c[kMathType].t,
                };
              },
              getText: (tok: PandocToken) => {
                const delimter = delimiterForType(tok.c[kMathType].t);
                return delimter + tok.c[kMathContent] + delimter;
              },
            },
            // extract math from backtick code for blogdown
            ...(blogdownMathInCode
              ? [
                {
                  token: PandocTokenType.Code,
                  mark: 'math',
                  match: (tok: PandocToken) => {
                    const text = tok.c[kCodeText];
                    return kSingleLineDisplayMathRegex.test(text) || kInlineMathRegex.test(text);
                  },
                  getAttrs: (tok: PandocToken) => {
                    const text = tok.c[kCodeText];
                    return {
                      type: kSingleLineDisplayMathRegex.test(text) ? MathType.Display : MathType.Inline,
                    };
                  },
                  getText: (tok: PandocToken) => {
                    return tok.c[kCodeText];
                  },
                },
              ]
              : []),
          ],
          writer: {
            priority: 1,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              // collect math content
              let mathText = '';
              parent.forEach((node: ProsemirrorNode) => (mathText = mathText + node.textContent));

              // if this is blogdownMathInCode just write the content in a code mark
              if (blogdownMathInCode) {
                output.writeToken(PandocTokenType.Code, () => {
                  output.writeAttr();
                  output.write(mathText);
                });
              } else {
                // check for delimeter (if it's gone then write this w/o them math mark)
                const delimiter = delimiterForType(mark.attrs.type);
                if (mathText.startsWith(delimiter) && mathText.endsWith(delimiter)) {

                  // remove delimiter
                  mathText = mathText.substr(delimiter.length, mathText.length - 2 * delimiter.length);

                  // trim inline math
                  if (mark.attrs.type === MathType.Inline) {
                    mathText = mathText.trim();
                  }

                  // if it's just whitespace then it's not actually math (we allow this state
                  // in the editor because it's the natural starting place for new equations)
                  if (mathText.length === 0) {
                    output.writeText(delimiter + mathText + delimiter);
                  } else {
                    output.writeToken(PandocTokenType.Math, () => {
                      // write type
                      output.writeToken(
                        mark.attrs.type === MathType.Inline ? PandocTokenType.InlineMath : PandocTokenType.DisplayMath,
                      );
                      output.write(mathText);
                    });
                  }
                } else {
                  // user removed the delimiter so write the content literally. when it round trips
                  // back into editor it will no longer be parsed by pandoc as math
                  output.writeRawMarkdown(mathText);
                }
              }
            },
          },
        },
      },
    ],

    baseKeys: (_schema: Schema) => {
      if (!singleLineDisplayMath) {
        return [{ key: BaseKey.Enter, command: displayMathNewline() }];
      } else {
        return [];
      }
    },

    inputRules: (schema: Schema, filter: MarkInputRuleFilter) => {
      return [
        // inline math
        new InputRule(
          new RegExp('(^|[^`])' + kInlineMathPattern + '$'),
          (state: EditorState, match: string[], start: number, end: number) => {
            if (!markIsActive(state, schema.marks.math) && filter(state, start, end)) {
              const tr = state.tr;
              tr.insertText('$');
              const mark = schema.marks.math.create({ type: MathType.Inline });
              tr.addMark(start + match[1].length, end + 1, mark);
              return tr;
            } else {
              return null;
            }
          },
        ),
        new InputRule(/(?:^|[^`])\$$/, (state: EditorState, match: string[], start: number, end: number) => {
          if (!markIsActive(state, schema.marks.math)) {
            const { parent, parentOffset } = state.selection.$head;
            const text = '$' + parent.textContent.slice(parentOffset);
            if (text.length > 0) {
              const length = mathLength(text);
              if (length > 1) {
                if (filter(state, start, start + length)) {
                  const tr = state.tr;
                  tr.insertText('$');
                  const startMath = tr.selection.from - 1;
                  const mark = schema.marks.math.create({ type: MathType.Inline });
                  tr.addMark(startMath, startMath + length, mark);
                  return tr;
                }
              }
            }
          }
          return null;
        }),
        // display math
        new InputRule(/^\$\$$/, (state: EditorState, match: string[], start: number, end: number) => {
          if (filter(state, start, end)) {
            const tr = state.tr;
            tr.delete(start, end);
            insertMath(tr.selection, MathType.Display, !singleLineDisplayMath, tr);
            return tr;
          } else {
            return null;
          }
        }),
      ];
    },

    commands: (_schema: Schema) => {
      return [new InsertInlineMathCommand(ui), new InsertDisplayMathCommand(ui, !singleLineDisplayMath)];
    },

    appendMarkTransaction: (_schema: Schema) => {
      return [mathAppendMarkTransaction()];
    },

    plugins: (schema: Schema) => {
      const plugins = [
        new Plugin({
          key: new PluginKey('math'),
          props: {
            // paste plain text into math blocks
            handlePaste: handlePasteIntoMath(),
          },
        }),
        mathHighlightPlugin(schema),
      ];
      if (math) {
        plugins.push(new MathPopupPlugin(ui, math, events, false));
        plugins.push(...mathViewPlugins(schema, ui, math));
      }
      return plugins;
    },
  };
};

function mathLength(text: string) {
  const match = text.match(kInlineMathRegex);
  if (match) {
    return match[0].length;
  } else {
    return 0;
  }
}

function handlePasteIntoMath() {
  return (view: EditorView, _event: Event, slice: Slice) => {
    const schema = view.state.schema;
    if (markIsActive(view.state, schema.marks.math)) {
      const tr = view.state.tr;
      tr.setMeta('paste', true);
      tr.setMeta('uiEvent', 'paste');
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

export default extension;
