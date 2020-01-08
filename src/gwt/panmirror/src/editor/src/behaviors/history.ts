import { history, redo, undo } from 'prosemirror-history';

import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';

const extension: Extension = {
  commands: () => {
    return [
      new ProsemirrorCommand(EditorCommandId.Undo, ['Mod-z'], undo),
      new ProsemirrorCommand(EditorCommandId.Redo, ['Shift-Mod-z'], redo),
    ];
  },

  plugins: () => {
    return [history()];
  },
};

export default extension;
