import { Schema } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';

import { markInputRule } from '../../api/mark';
import { markPasteHandler } from '../../api/clipboard';
import { EditorOutlineItem } from '../../api/outline';

import { getOutline } from '../../behaviors/outline';

export function linkInputRules(autoLink: boolean, headingLink: boolean) {
  return (schema: Schema) => {
    const rules = [
      // <link> style link
      markInputRule(/(?:<)([a-z]+:\/\/[^>]+)(?:>)$/, schema.marks.link, (match: string[]) => ({ href: match[1] })),
      // full markdown link
      markInputRule(/(?:\[)([^\]]+)(?:\]\()([^)]+)(?:\))$/, schema.marks.link, (match: string[]) => ({
        href: match[2],
      })),
    ];

    if (autoLink) {
      // plain link
      rules.push(
        new InputRule(/([a-z]+:\/\/[^\s]+) $/, (state: EditorState, match: string[], start: number, end: number) => {
          const tr = state.tr;
          end = start + match[1].length;
          tr.addMark(start, end, schema.marks.link.create({ href: match[1] }));
          tr.removeStoredMark(schema.marks.link);
          tr.insertText(' ');
          setTextSelection(end + 1)(tr);
          return tr;
        }),
      );
    }

    if (headingLink) {
      rules.push(
        new InputRule(/(?:\[)([^@]+)(?:\] )/, (state: EditorState, match: string[], start: number, end: number) => {
          // check to see if the text matches a heading
          const text = match[1];
          const outline = getOutline(state);
          const hasMatchingHeading = (item: EditorOutlineItem) => {
            if (item.type === 'heading' && item.title === text) {
              return true;
            } else {
              return item.children.some(hasMatchingHeading);
            }
          };
          if (outline.some(hasMatchingHeading)) {
            const tr = state.tr;
            tr.addMark(
              start + 1,
              start + 1 + text.length,
              state.schema.marks.link.create({ href: text, heading: text }),
            );
            tr.delete(start, start + 1);
            tr.delete(end - 2, end - 1);
            tr.insertText(' ');
            return tr;
          } else {
            return null;
          }
        }),
      );
    }

    return rules;
  };
}

export function linkPasteHandler(schema: Schema) {
  return markPasteHandler(/[a-z]+:\/\/[^\s]+/g, schema.marks.link, url => ({ href: url }));
}
