import { dropCursor } from 'prosemirror-dropcursor';
import { gapCursor } from 'prosemirror-gapcursor';
import 'prosemirror-gapcursor/style/gapcursor.css';

import { Extension } from '../api/extension';

const extension: Extension = {
  plugins: () => {
    return [gapCursor(), dropCursor()];
  },
};

export default extension;
