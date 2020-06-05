
/*
 * omni_insert.ts
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
import { DOMOutputSpecArray, Mark, Fragment } from "prosemirror-model";
import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import { PandocOutput } from "../../api/pandoc";
import { ProsemirrorCommand, EditorCommandId } from "../../api/command";
import { selectionAllowsCompletions } from "../../api/completion";

const extension = {

  marks: [
    {
      name: 'omni_insert',
      spec: {
        inclusive: true,
        noInputRules: true,
        excludes: '_',
        parseDOM: [
          { tag: "span[class*='omni_insert']" },
        ],
        toDOM() : DOMOutputSpecArray {
          return [ 'span', { class: 'omni_insert' } ];
        },
      },
      pandoc: {
        readers: [],
        writer: {
          priority: 30,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeInlines(parent);
          },
        },
      },
    },
  ],

  commands: () => [new OmniInsertCommand()]
};

class OmniInsertCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.OmniInsert, ['Mod-/'], (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
          
      // check whether selection allows completions
      if (!selectionAllowsCompletions(state.selection)) {
        return false;
      }

      if (dispatch) {
        const mark = state.schema.marks.omni_insert.create();
        const node = state.schema.text('/', [mark]);
        const tr = state.tr;
        tr.replaceSelectionWith(node, false);
        dispatch(tr);
      }

      return true;
    });
  }
}

export default extension;

