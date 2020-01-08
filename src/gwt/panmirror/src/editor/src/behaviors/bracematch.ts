import { Schema } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { EditorOptions } from '../api/options';
import { PandocExtensions } from '../api/pandoc';

const braces = new Map([
  ['{', '}'],
  ['(', ')'],
  ['[', ']'],
]);

const extension: Extension = {
  inputRules: (_schema: Schema) => {
    return [
      new InputRule(/(^|[^^\\])([{([])$/, (state: EditorState, match: string[], start: number, end: number) => {
        const tr = state.tr;
        tr.insertText(match[2] + braces.get(match[2]));
        setTextSelection(start + match[1].length + 1)(tr);
        return tr;
      }),
    ];
  },
};

export default (_pandocExtensions: PandocExtensions, options: EditorOptions) => {
  if (options.braceMatching) {
    return extension;
  } else {
    return null;
  }
};
