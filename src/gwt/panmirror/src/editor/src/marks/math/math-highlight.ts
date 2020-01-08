import { PluginKey } from 'prosemirror-state';
import { DecorationSet, Decoration } from 'prosemirror-view';
import { Schema } from 'prosemirror-model';

import { markHighlightPlugin } from '../../api/mark-highlight';

import { delimiterForType } from './math';

const key = new PluginKey<DecorationSet>('math-highlight');

export function mathHighlightPlugin(schema: Schema) {
  return markHighlightPlugin(key, schema.marks.math, (_text, attrs, markRange) => {
    const kDelimClass = 'pm-markup-text-color';
    const delim = delimiterForType(attrs.type);
    if (markRange.to - markRange.from === delim.length * 2) {
      return [Decoration.inline(markRange.from, markRange.to, { class: kDelimClass })];
    } else {
      return [
        Decoration.inline(markRange.from, markRange.from + delim.length, { class: kDelimClass }),
        Decoration.inline(markRange.to - delim.length, markRange.to, { class: kDelimClass }),
      ];
    }
  });
}
