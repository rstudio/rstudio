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
  Heading = "heading"
}

export interface Navigation {
  type: NavigationType;
  location: string;
}

export function navigateTo(view: EditorView, predicate: Predicate, animate = true): Navigation | null {
  const result = findChildren(view.state.doc, predicate);
  if (result.length) {
    return navigateToPosition(view, result[0].pos, animate);
  } else {
    return null;
  }
}

export function navigateToId(view: EditorView, id: string, animate = true): Navigation | null {
  if (navigateTo(view, node => id === node.attrs.navigation_id, animate)) {
    return {
      type: NavigationType.Id,
      location: id
    };
  } else {
    return null;
  }

}

export function navigateToHref(view: EditorView, href: string, animate = true): Navigation | null {
  if (navigateTo(view, node => node.attrs.id === href, animate)) {
    return {
      type: NavigationType.Href,
      location: href
    };
  } else {
    return null;
  }
}

export function navigateToHeading(view: EditorView, heading: string, animate = true): Navigation | null {
  if (navigateTo(
    view,
    node => {
      return (
        node.type === view.state.schema.nodes.heading &&
        node.textContent.localeCompare(heading, undefined, { sensitivity: 'accent' }) === 0
      );
    },
    animate,
  )) {
    return {
      type: NavigationType.Heading,
      location: heading
    };
  } else {
    return null;
  }

}

export function navigateToPosition(view: EditorView, pos: number, animate = true): Navigation | null {
  // set selection
  view.dispatch(setTextSelection(pos)(view.state.tr));

  // scroll to selection
  const node = view.nodeDOM(pos);
  if (node instanceof HTMLElement) {
    const editingRoot = editingRootNode(view.state.selection)!;
    const container = view.nodeDOM(editingRoot.pos) as HTMLElement;
    const scroller = zenscroll.createScroller(container, 700, 20);
    if (animate) {
      scroller.to(node);
    } else {
      scroller.to(node, 0);
    }
    return {
      type: NavigationType.Pos,
      location: pos.toString()
    };
  } else {
    return null;
  }


}
