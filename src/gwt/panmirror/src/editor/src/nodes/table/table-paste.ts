import { Plugin, PluginKey } from 'prosemirror-state';
import { Slice } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { sliceHasNode } from '../../api/slice';
import { fixupTableWidths } from './table-columns';

export function tablePaste() {
  return new Plugin({
    key: new PluginKey('table-paste'),
    props: {
      handlePaste: (view: EditorView, _event: Event, slice: Slice) => {
        // if the slice contains a table then we handle it
        if (sliceHasNode(slice, node => node.type === node.type.schema.nodes.table)) {
          // based on https://github.com/ProseMirror/prosemirror-view/blob/fb799aae4e9dd5cfc256708a6845d76aaaf145bf/src/input.js#L503-L510
          const tr = view.state.tr.replaceSelection(slice);
          view.dispatch(
            tr
              .scrollIntoView()
              .setMeta('paste', true)
              .setMeta('uiEvent', 'paste'),
          );
          view.dispatch(fixupTableWidths(view)(view.state.tr));
          return true;
        } else {
          return false;
        }
      },
    },
  });
}
