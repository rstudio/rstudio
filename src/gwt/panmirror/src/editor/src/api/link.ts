/*
 * link.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { findChildren, findChildrenByType } from 'prosemirror-utils';

export const kLinkTargetUrl = 0;
export const kLinkTargetTitle = 1;

export const kLinkAttr = 0;
export const kLinkChildren = 1;
export const kLinkTarget = 2;

export enum LinkType {
  URL = 0,
  Heading = 1,
  ID = 2,
}

export interface LinkCapabilities {
  headings: boolean;
  attributes: boolean;
  text: boolean;
}

export interface LinkTargets {
  readonly ids: string[];
  readonly headings: LinkHeadingTarget[];
}

export interface LinkHeadingTarget {
  readonly level: number;
  readonly text: string;
}

export async function linkTargets(doc: ProsemirrorNode) {
  const ids = findChildren(doc, node => !!node.attrs.id).map(value => value.node.attrs.id);

  const headings = findChildrenByType(doc, doc.type.schema.nodes.heading).map(heading => ({
    level: heading.node.attrs.level,
    text: heading.node.textContent,
  }));

  return {
    ids,
    headings,
  };
}
