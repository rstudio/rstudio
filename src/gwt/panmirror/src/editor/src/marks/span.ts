import { Mark, Fragment, Schema } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Extension, extensionIfEnabled } from '../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { EditorUI } from '../api/ui';
import { markIsActive, getMarkAttrs, getSelectionMarkRange } from '../api/mark';
import { PandocOutput, PandocTokenType, PandocToken } from '../api/pandoc';
import { pandocAttrSpec, pandocAttrReadAST, pandocAttrParseDom, pandocAttrToDomAttr } from '../api/pandoc_attr';

const SPAN_ATTR = 0;
const SPAN_CHILDREN = 1;

const extension: Extension = {
  marks: [
    {
      name: 'span',
      spec: {
        attrs: pandocAttrSpec,
        inclusive: false,
        parseDOM: [
          {
            tag: 'span[data-span="1"]',
            getAttrs(dom: Node | string) {
              const attrs: {} = { 'data-span': 1 };
              return {
                ...attrs,
                ...pandocAttrParseDom(dom as Element, attrs),
              };
            },
          },
        ],
        toDOM(mark: Mark) {
          const attr = {
            'data-span': '1',
            ...pandocAttrToDomAttr({
              ...mark.attrs,
              classes: [...mark.attrs.classes, 'pm-span-background-color'],
            }),
          };
          return ['span', attr];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Span,
            mark: 'span',
            getAttrs: (tok: PandocToken) => {
              return pandocAttrReadAST(tok, SPAN_ATTR);
            },
            getChildren: (tok: PandocToken) => tok.c[SPAN_CHILDREN],
          },
        ],
        writer: {
          priority: 11,
          write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
            output.writeToken(PandocTokenType.Span, () => {
              output.writeAttr(mark.attrs.id, mark.attrs.classes, mark.attrs.keyvalue);
              output.writeArray(() => {
                output.writeInlines(parent);
              });
            });
          },
        },
      },
    },
  ],

  commands: (_schema: Schema, ui: EditorUI) => {
    return [new SpanCommand(ui)];
  },
};

class SpanCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(EditorCommandId.Span, [], (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
      const schema = state.schema;

      // if there is no contiguous selection and no existing span mark active
      // then the command should be disabled (unknown what the span target is)
      if (!markIsActive(state, schema.marks.span) && state.selection.empty) {
        return false;
      }

      // if the current node doesn't allow this mark return false
      if (!state.selection.$from.node().type.allowsMarkType(schema.marks.span)) {
        return false;
      }

      async function asyncEditSpan() {
        if (dispatch) {
          let attr: { [key: string]: any } = { id: null, classes: [], keyvalue: [] };
          if (markIsActive(state, schema.marks.span)) {
            attr = getMarkAttrs(state.doc, state.selection, schema.marks.span);
          }
          const result = await ui.dialogs.editSpan(attr);
          if (result) {
            const tr = state.tr;
            const range = getSelectionMarkRange(state.selection, schema.marks.span);
            tr.removeMark(range.from, range.to, schema.marks.span);
            if (result.action === 'edit') {
              const mark = schema.marks.span.create(result.attr);
              tr.addMark(range.from, range.to, mark);
            }
            dispatch(tr);
          }
        }
        if (view) {
          view.focus();
        }
      }
      asyncEditSpan();

      return true;
    });
  }
}

export default extensionIfEnabled(extension, ['bracketed_spans', 'native_spans']);
