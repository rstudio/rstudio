import { NodeWithPos } from 'prosemirror-utils';
import { DecorationAttrs, Decoration } from 'prosemirror-view';

export function nodeDecoration(nodeWithPos: NodeWithPos, attrs: DecorationAttrs) {
  return Decoration.node(nodeWithPos.pos, nodeWithPos.pos + nodeWithPos.node.nodeSize, attrs);
}
