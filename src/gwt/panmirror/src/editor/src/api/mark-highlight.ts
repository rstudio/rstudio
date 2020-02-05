/*
 * mark-highlight.ts
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

import { PluginKey, Plugin, EditorState, Transaction } from 'prosemirror-state';
import { DecorationSet, Decoration } from 'prosemirror-view';
import { Node as ProsemirrorNode, MarkType } from 'prosemirror-model';
import { findChildrenByMark } from 'prosemirror-utils';
import { ReplaceStep, AddMarkStep, RemoveMarkStep } from 'prosemirror-transform';

import { getMarkRange, getMarkAttrs } from './mark';
import { RangeStep } from './transaction';

export type MarkHighligher = (
  text: string,
  attrs: { [key: string]: any },
  range: { from: number; to: number },
) => Decoration[];

export function markHighlightDecorations(
  markRange: { from: number; to: number },
  text: string,
  re: RegExp,
  className: string,
) {
  const decorations: Decoration[] = [];
  let match = re.exec(text);
  while (match) {
    decorations.push(
      Decoration.inline(markRange.from + match.index, markRange.from + re.lastIndex, { class: className }),
    );
    match = re.exec(text);
  }
  return decorations;
}

export function markHighlightPlugin(key: PluginKey<DecorationSet>, markType: MarkType, highlighter: MarkHighligher) {
  function decorationsForDoc(doc: ProsemirrorNode) {
    let decorations: Decoration[] = [];
    findChildrenByMark(doc, markType, true).forEach(markedNode => {
      decorations = decorations.concat(markDecorations(doc, markType, markedNode.pos, highlighter));
    });
    return DecorationSet.create(doc, decorations);
  }

  return new Plugin<DecorationSet>({
    key,
    state: {
      // initialize by highlighting the entire document
      init(_config: { [key: string]: any }, instance: EditorState) {
        return decorationsForDoc(instance.doc);
      },

      // whenever an edit affecting this mark type occurs then update the decorations
      apply(tr: Transaction, set: DecorationSet, _oldState: EditorState, _newState: EditorState) {
        // ignore selection changes
        if (!tr.docChanged) {
          return set;
        }

        // adjust decoration positions to changes made by the transaction (decorations that apply
        // to removed chunks of content will be removed by this)
        set = set.map(tr.mapping, tr.doc);

        // function to rehighlight parent of specified pos
        const rehighlightParent = (pos: number) => {
          const resolvedPos = tr.doc.resolve(pos);
          const parent = resolvedPos.node();
          const from = resolvedPos.start();
          const marks = findChildrenByMark(parent, markType);
          marks.forEach(mark => {
            const markRange = getMarkRange(tr.doc.resolve(from + mark.pos), markType) as { from: number; to: number };
            const removeDecorations = set.find(markRange.from, markRange.to);
            set = set.remove(removeDecorations);
            const addDecorations = markDecorations(tr.doc, markType, markRange.from, highlighter);
            set = set.add(tr.doc, addDecorations);
          });
        };

        // examine each step and update highligthing decorations as appropriate
        tr.steps.forEach(step => {
          // highlight new marks of this type
          if (step instanceof AddMarkStep && (step as any).mark.type === markType) {
            const from = (step as any).from;
            const addDecorations = markDecorations(tr.doc, markType, from, highlighter);
            set = set.add(tr.doc, addDecorations);

            // remove decorations when marks are removed
          } else if (step instanceof RemoveMarkStep && (step as any).mark.type === markType) {
            const { from, to } = step as any;
            const removeDecorations = set.find(from, to);
            set = set.remove(removeDecorations);

            // also rehighlight parent
            rehighlightParent(from);

            // rehighlight parent for normal replace steps
          } else if (step instanceof ReplaceStep) {
            const replaceStep = (step as unknown) as RangeStep;
            rehighlightParent(tr.mapping.map(replaceStep.from));
          }
        });

        return set;
      },
    },
    props: {
      decorations(state) {
        return key.getState(state);
      },
    },
  });
}

function markDecorations(doc: ProsemirrorNode, markType: MarkType, pos: number, highlighter: MarkHighligher) {
  const markRange = getMarkRange(doc.resolve(pos), markType);
  if (markRange) {
    const attrs = getMarkAttrs(doc, markRange, markType);
    const text = doc.textBetween(markRange.from, markRange.to);
    return highlighter(text, attrs, markRange);
  } else {
    return [];
  }
}
