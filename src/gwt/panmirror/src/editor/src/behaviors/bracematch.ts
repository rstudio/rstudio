/*
 * bracematch.ts
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
