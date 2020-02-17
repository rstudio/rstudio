import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { ReplaceStep, AddMarkStep, RemoveMarkStep } from 'prosemirror-transform';
import { findChildrenByMark } from 'prosemirror-utils';
import { Node as ProsemirrorNode } from 'prosemirror-model';

import { RangeStep } from '../api/transaction';
import { Extension } from '../api/extension';
import { PandocFormatComment, pandocFormatCommentFromState } from '../api/pandoc_format';

const plugin = new PluginKey<PandocFormatComment>('format-comment');

const extension: Extension = {
  plugins: () => [formatCommentPlugin()],
};

function formatCommentPlugin() {
  return new Plugin<PandocFormatComment>({
    key: plugin,
    state: {
      init(_config, state: EditorState) {
        return formatCommentFromState(state);
      },

      apply(tr: Transaction, comment: PandocFormatComment, oldState: EditorState, newState: EditorState) {
        if (!tr.docChanged) {
          return comment;
        }

        const hasRawInline = (pos: number, doc: ProsemirrorNode) => {
          const resolvedPos = doc.resolve(pos);
          const parent = resolvedPos.node();
          return findChildrenByMark(parent, schema.marks.raw_inline).length > 0;
        };

        const schema = tr.doc.type.schema;
        const isCommentEdit = tr.steps.some(step => {
          // add or remove of a raw_inline mark w/ comment: true
          if (step instanceof AddMarkStep || step instanceof RemoveMarkStep) {
            return (step as any).mark.type === schema.marks.raw_inline && (step as any).mark.comment;
            // replace step that affects raw_inline
          } else if (step instanceof ReplaceStep) {
            const replaceStep = (step as unknown) as RangeStep;
            return (
              hasRawInline(replaceStep.from, oldState.doc) || hasRawInline(tr.mapping.map(replaceStep.from), tr.doc)
            );
          } else {
            return false;
          }
        });

        if (isCommentEdit) {
          return formatCommentFromState(newState);
        } else {
          return comment;
        }
      },
    },
  });
}

export function getFormatComment(state: EditorState): PandocFormatComment {
  return plugin.getState(state);
}

function formatCommentFromState(state: EditorState): PandocFormatComment {
  const comment = pandocFormatCommentFromState(state);
  return comment;
}

export default extension;
