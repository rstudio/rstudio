import { PluginKey } from 'prosemirror-state';
import { DecorationSet, Decoration } from 'prosemirror-view';
import { Schema } from 'prosemirror-model';

import { markHighlightPlugin, markHighlightDecorations } from '../../api/mark-highlight';

const key = new PluginKey<DecorationSet>('cite-highlight');

export function citeHighlightPlugin(schema: Schema) {
  return markHighlightPlugin(key, schema.marks.cite, (text, _attrs, markRange) => {
    // id decorations
    const kIdClass = 'pm-link-text-color';
    const re = /-?@[\w:.#$%&-+?<>~/]+/g;
    const decorations = markHighlightDecorations(markRange, text, re, kIdClass);

    // delimiter decorations
    const kDelimClass = 'cite-delimiter';
    return decorations.concat([
      Decoration.inline(markRange.from, markRange.from + 1, { class: kDelimClass }),
      Decoration.inline(markRange.to - 1, markRange.to, { class: kDelimClass }),
    ]);
  });
}
