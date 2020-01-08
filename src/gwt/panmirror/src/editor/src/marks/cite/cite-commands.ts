import { EditorState, Transaction } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';
import { EditorView } from 'prosemirror-view';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { EditorUI } from '../../api/ui';

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
              tr.insertText(`[${citation.id}${locator}]`);
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
    );
  }
}
