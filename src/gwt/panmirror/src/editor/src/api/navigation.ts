/*
 * navigation.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { EditorView } from 'prosemirror-view';

import { setTextSelection, Predicate, findChildren, findDomRefAtPos } from 'prosemirror-utils';

import zenscroll from 'zenscroll';

import { editingRootNode } from './node';
import { kNavigationTransaction } from './transaction';
import { xrefPosition } from './xref';

export interface EditorNavigation {
  navigate: (type: NavigationType, location: string, animate?: boolean) => void;
}

export enum NavigationType {
  Pos = "pos",
  Id = "id",
  Href = "href",
  Heading = "heading",
  XRef = "xref"
}

export interface Navigation {
  pos: number;
  prevPos: number;
}

export function navigateTo(view: EditorView, type: NavigationType, location: string, animate = true): Navigation | null {

  switch (type) {
    case NavigationType.Pos:
      return navigateToPos(view, parseInt(location, 10), animate);
    case NavigationType.Id:
      return navigateToId(view, location, animate);
    case NavigationType.Href:
      return navigateToHref(view, location, animate);
    case NavigationType.Heading:
      return navigateToHeading(view, location, animate);
    case NavigationType.XRef:
      return navigateToXRef(view, location, animate);
    default:
      return null;
  }
}

export function navigateToId(view: EditorView, id: string, animate = true): Navigation | null {
  return navigate(view, node => id === node.attrs.navigation_id, animate);
}

export function navigateToHref(view: EditorView, href: string, animate = true): Navigation | null {
  return navigate(view, node => node.attrs.id === href, animate);
}

export function navigateToHeading(view: EditorView, heading: string, animate = true): Navigation | null {
  return navigate(
    view,
    node => {
      return (
        node.type === view.state.schema.nodes.heading &&
        node.textContent.localeCompare(heading, undefined, { sensitivity: 'accent' }) === 0
      );
    },
    animate,
  );
}

export function navigateToXRef(view: EditorView, xref: string, animate = true): Navigation | null {
  const xrefPos = xrefPosition(view.state.doc, xref);
  if (xrefPos !== -1) {
    return navigateToPos(view, xrefPos, animate);
  } else {
    return null;
  }
}


export function navigateToPos(view: EditorView, pos: number, animate = true): Navigation | null {

  // get previous position
  const prevPos = view.state.selection.from;

  // need to target at least the body
  pos = Math.max(pos, 2);

  // set selection
  const tr = view.state.tr;
  setTextSelection(pos)(tr);
  tr.setMeta(kNavigationTransaction, true);
  view.dispatch(tr);

  // find a targetable dom node at the position
  const node = findDomRefAtPos(pos, view.domAtPos.bind(view));
  if (node instanceof HTMLElement) {

    // auto-scroll to position (delay so we can grab the focus, as autoscrolling
    // doesn't seem to work unless you have the focus)
    setTimeout(() => {
      view.focus();
      const editingRoot = editingRootNode(view.state.selection)!;
      const container = view.nodeDOM(editingRoot.pos) as HTMLElement;
      const scroller = zenscroll.createScroller(container, 700, 20);
      if (animate) {
        scroller.to(node);
      } else {
        scroller.to(node, 0);
      }
    }, 200);

    return { pos, prevPos };

  } else {
    return null;
  }
}

function navigate(view: EditorView, predicate: Predicate, animate = true): Navigation | null {
  const result = findChildren(view.state.doc, predicate);
  if (result.length) {
    // pos points immediately before the node so add 1 to it
    const pos = result[0].pos + 1;
    return navigateToPos(view, pos, animate);
  } else {
    return null;
  }
}

