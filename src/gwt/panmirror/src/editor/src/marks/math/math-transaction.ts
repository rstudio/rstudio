import { Node as ProsemirrorNode } from 'prosemirror-model';
import { findChildrenByMark } from 'prosemirror-utils';

import { getMarkRange, getMarkAttrs } from '../../api/mark';
import { mergedTextNodes } from '../../api/text';
import { AppendMarkTransactionHandler, MarkTransaction } from '../../api/transaction';

import { delimiterForType, MathType } from './math';

export function mathAppendMarkTransaction(): AppendMarkTransactionHandler {
  return {
    name: 'math-marks',

    filter: node => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.math),

    append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
      // find all math blocks and convert them to text if they no longer conform
      const schema = node.type.schema;
      const maths = findChildrenByMark(node, schema.marks.math, true);
      for (const math of maths) {
        const from = pos + 1 + math.pos;
        const mathRange = getMarkRange(tr.doc.resolve(from), schema.marks.math);
        if (mathRange) {
          const mathAttr = getMarkAttrs(tr.doc, mathRange, schema.marks.math);
          const mathDelim = delimiterForType(mathAttr.type);
          const mathText = tr.doc.textBetween(mathRange.from, mathRange.to);
          const charAfter = tr.doc.textBetween(mathRange.to, mathRange.to + 1);

          const noDelims = !mathText.startsWith(mathDelim) || !mathText.endsWith(mathDelim);
          const unescapedDollarWithin =
            mathAttr.type === MathType.Inline && mathText.search(/[^\\]\$/) !== mathText.length - 1;
          const escapedDollarAtEdge = mathAttr.type === MathType.Inline && mathText.endsWith('\\$');
          const spaceAtEdge =
            mathAttr.type === MathType.Inline &&
            (mathText.startsWith(mathDelim + ' ') || mathText.endsWith(' ' + mathDelim));
          const numberAfter = mathAttr.type === MathType.Inline && /\d/.test(charAfter);

          if (noDelims || unescapedDollarWithin || escapedDollarAtEdge || spaceAtEdge || numberAfter) {
            tr.removeMark(mathRange.from, mathRange.to, schema.marks.math);
            tr.removeStoredMark(schema.marks.math);
          }
        }
      }

      // get updated view of the node
      node = tr.doc.nodeAt(pos)!;

      // find unmarked math and see if it conforms
      const dollarNodes = mergedTextNodes(node, hasUnmarkedDollar);
      dollarNodes.forEach(dollarNode => {
        const text = dollarNode.text;
        let dollarIdx = text.indexOf('$');
        while (dollarIdx !== -1) {
          const math = findMath(text.substring(dollarIdx));
          if (math.type) {
            const from = pos + 1 + dollarNode.pos + dollarIdx;
            const to = from + math.length;
            if (!tr.doc.rangeHasMark(from, to, schema.marks.math)) {
              const mark = schema.mark('math', { type: math.type });
              tr.addMark(from, to, mark);
            }
          }
          dollarIdx = text.indexOf('$', dollarIdx + math.length + 1);
        }
      });
    },
  };
}

// look for unmarked dollar signs
const hasUnmarkedDollar = (node: ProsemirrorNode, parentNode: ProsemirrorNode) => {
  return (
    parentNode.type.allowsMarkType(node.type.schema.marks.math) &&
    node.isText &&
    /\$/.test(node.textContent) &&
    !node.type.schema.marks.math.isInSet(node.marks)
  );
};

function findMath(str: string): { type: MathType | undefined; length: number } {
  // look for display math
  const displayMatch = str.match(/^\$\$.*\$\$/);
  if (displayMatch) {
    return {
      type: MathType.Display,
      length: displayMatch[0].length,
    };
  }

  // look for empty inline math
  const emptyInlineMatch = str.match(/^\$\$/);
  if (emptyInlineMatch) {
    return {
      type: MathType.Display,
      length: emptyInlineMatch[0].length,
    };
  }

  // look for inline math terminator if we don't have a space after the $
  if (!str.startsWith('$ ')) {
    const endDollarIndex = str.substr(1).search(/[^ \\]\$($|[^\d])/);
    if (endDollarIndex !== -1) {
      return {
        type: MathType.Inline,
        length: 1 + endDollarIndex + 2,
      };
    }
  }

  // no math found
  return {
    type: undefined,
    length: 0,
  };
}
