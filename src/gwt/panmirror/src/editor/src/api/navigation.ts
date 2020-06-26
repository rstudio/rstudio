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

import { setTextSelection, Predicate, findChildren } from 'prosemirror-utils';

import zenscroll from 'zenscroll';

import { editingRootNode } from './node';

export enum NavigationType {
  Pos = "pos",
  Id = "id",
  Href = "href",
  Heading = "heading",
  Top = "top"
}

export interface Navigation {
  type: NavigationType;
  location: string;
  pos: number;
}

export function navigateTo(view: EditorView, navigation: Navigation, animate = true): Navigation | null {
  switch (navigation.type) {
    case NavigationType.Pos:
      return navigateToPos(view, parseInt(navigation.location, 10), animate);
    case NavigationType.Id:
      return navigateToId(view, navigation.location, animate);
    case NavigationType.Href:
      return navigateToHref(view, navigation.location, animate);
    case NavigationType.Heading:
      return navigateToHeading(view, navigation.location, animate);
    case NavigationType.Top:
      return navigateToTop(view, animate);
    default:
      return null;
  }
}

export function navigateToId(view: EditorView, id: string, animate = true): Navigation | null {
  const nav = navigate(view, node => id === node.attrs.navigation_id, animate);
  if (nav) {
    return {
      type: NavigationType.Id,
      location: id,
      pos: nav.pos
    };
  } else {
    return null;
  }

}

export function navigateToHref(view: EditorView, href: string, animate = true): Navigation | null {
  const nav = navigate(view, node => node.attrs.id === href, animate);
  if (nav) {
    return {
      type: NavigationType.Href,
      location: href,
      pos: nav.pos
    };
  } else {
    return null;
  }
}

export function navigateToHeading(view: EditorView, heading: string, animate = true): Navigation | null {

  const nav = navigate(
    view,
    node => {
      return (
        node.type === view.state.schema.nodes.heading &&
        node.textContent.localeCompare(heading, undefined, { sensitivity: 'accent' }) === 0
      );
    },
    animate,
  );

  if (nav) {
    return {
      type: NavigationType.Heading,
      location: heading,
      pos: nav.pos
    };
  } else {
    return null;
  }

}

export function navigateToPos(view: EditorView, pos: number, animate = true): Navigation | null {
  // set selection
  view.dispatch(setTextSelection(pos)(view.state.tr));

  // scroll to selection
  const node = view.nodeDOM(pos);
  if (node instanceof HTMLElement) {

    // perform navigation
    const editingRoot = editingRootNode(view.state.selection)!;
    const container = view.nodeDOM(editingRoot.pos) as HTMLElement;
    const scroller = zenscroll.createScroller(container, 700, 20);
    if (animate) {
      scroller.to(node);
    } else {
      scroller.to(node, 0);
    }

    // pos for locating dom elements is 1 before the target, so add 1 to pos before returning it
    pos++;

    // return nav info
    return {
      type: NavigationType.Pos,
      location: pos.toString(),
      pos
    };
  } else {
    return null;
  }
}

export function navigateToTop(view: EditorView, animate = true): Navigation | null {
  const nav = navigateToPos(view, 2, animate);
  if (nav) {
    return {
      type: NavigationType.Top,
      location: "2",
      pos: 2
    };
  } else {
    return null;
  }
}

function navigate(view: EditorView, predicate: Predicate, animate = true): Navigation | null {
  const result = findChildren(view.state.doc, predicate);
  if (result.length) {
    return navigateToPos(view, result[0].pos, animate);
  } else {
    return null;
  }
}

