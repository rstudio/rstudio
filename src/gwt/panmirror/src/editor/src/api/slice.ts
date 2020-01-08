import { Slice, Node as ProsemirrorNode } from 'prosemirror-model';

export function sliceContentLength(slice: Slice) {
  let length = 0;
  for (let i = 0; i < slice.content.childCount; i++) {
    length += slice.content.child(i).textContent.length;
  }
  return length;
}

export function sliceHasNode(slice: Slice, predicate: (node: ProsemirrorNode) => boolean) {
  let hasNode = false;
  slice.content.descendants(node => {
    if (predicate(node)) {
      hasNode = true;
      return false;
    }
  });
  return hasNode;
}
