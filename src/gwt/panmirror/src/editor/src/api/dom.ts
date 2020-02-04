import { EditorView } from "prosemirror-view";

 export function bodyElement(view: EditorView) : HTMLElement {
    return view.dom.firstChild as HTMLElement;
  }
