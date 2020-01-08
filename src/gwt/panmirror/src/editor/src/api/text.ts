import { Node as ProsemirrorNode } from 'prosemirror-model';

export interface TextWithPos {
  readonly text: string;
  readonly pos: number;
}

export function mergedTextNodes(
  doc: ProsemirrorNode,
  filter?: (node: ProsemirrorNode, parentNode: ProsemirrorNode) => boolean,
): TextWithPos[] {
  const textNodes: TextWithPos[] = [];
  let nodeIndex = 0;
  doc.descendants((node, pos, parentNode) => {
    if (node.isText) {
      // apply filter
      if (filter && !filter(node, parentNode)) {
        return false;
      }

      // join existing contiguous range of text nodes or create a new one
      if (textNodes[nodeIndex]) {
        textNodes[nodeIndex] = {
          text: textNodes[nodeIndex].text + node.text,
          pos: textNodes[nodeIndex].pos,
        };
      } else {
        textNodes[nodeIndex] = {
          text: node.text || '',
          pos,
        };
      }
    } else {
      nodeIndex += 1;
    }
  });
  return textNodes;
}
