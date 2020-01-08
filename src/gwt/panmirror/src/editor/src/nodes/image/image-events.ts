import { Node, NodeType } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { ImageEditorFn } from '../../api/ui';

import { imageDialog } from './image-dialog';

export function imageDoubleClickOn(nodeType: NodeType, onEditImage: ImageEditorFn, imageAttributes: boolean) {
  return (view: EditorView, _pos: number, node: Node) => {
    if (node.type === nodeType) {
      imageDialog(node, nodeType, view.state, view.dispatch, view, onEditImage, imageAttributes);
      return true;
    } else {
      return false;
    }
  };
}

export function imageDrop(nodeType: NodeType) {
  return (view: EditorView, event: Event) => {
    // alias to drag event so typescript knows about event.dataTransfer
    const dragEvent = event as DragEvent;

    // ensure we have data transfer
    if (!dragEvent.dataTransfer) {
      return false;
    }

    // ensure the drop coordinates map to an editor position
    const coordinates = view.posAtCoords({
      left: dragEvent.clientX,
      top: dragEvent.clientY,
    });
    if (!coordinates) {
      return false;
    }

    // see if this is a drag of image uris
    const uriList = dragEvent.dataTransfer.getData('text/uri-list');
    const html = dragEvent.dataTransfer.getData('text/html');
    if (!uriList || !html) {
      return false;
    }

    // see if we can pull an image out of the html
    const regex = /<img.*?src=["'](.*?)["']/;
    const match = regex.exec(html);
    if (!match) {
      return false;
    }

    // indicate that we can handle this drop
    event.preventDefault();

    // insert the images
    uriList.split('\r?\n').forEach(src => {
      const node = nodeType.create({ src });
      const transaction = view.state.tr.insert(coordinates.pos, node);
      view.dispatch(transaction);
    });

    return true;
  };
}
