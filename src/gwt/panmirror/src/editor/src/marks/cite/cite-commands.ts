/*
 * cite-commands.ts
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

import { EditorState, Transaction } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';
import { EditorView } from 'prosemirror-view';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { EditorUI } from '../../api/ui';
import { OmniInsertGroup } from '../../api/omni_insert';

export class InsertCitationCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.Citation,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        // enable/disable command
        const schema = state.schema;
        if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.cite)(state)) {
          return false;
        }

        async function asyncInsertCitation() {
          if (dispatch) {
            const citation = await ui.dialogs.insertCitation();
            if (citation) {
              const tr = state.tr;
              const locator = citation.locator ? ' ' + citation.locator : '';

              const citeMark = schema.marks.cite.create();
              const cite = schema.text(`[${citation.id}${locator}]`, [citeMark]);
              tr.replaceSelectionWith(cite, false);

              const citeIdMark = schema.marks.cite_id.create();
              tr.addMark(state.selection.from + 1, state.selection.from + 1 + citation.id.length, citeIdMark);
              dispatch(tr);
            }
            if (view) {
              view.focus();
            }
          }
        }
        asyncInsertCitation();

        return true;
      },
      {
        name: ui.context.translateText('Citation...'),
        description: ui.context.translateText('Reference to a source'),
        group: OmniInsertGroup.References,
        priority: 1,
        image: () => ui.images.omni_insert?.generic!,
      },
    );
  }
}
