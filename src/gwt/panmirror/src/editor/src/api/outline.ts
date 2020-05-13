/*
 * outline.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { findTopLevelBodyNodes } from './node';

export interface EditorOutlineItem {
  navigation_id: string;
  type: EditorOutlineItemType;
  level: number;
  title: string;
  children: EditorOutlineItem[];
}

export const kHeadingOutlineItemType = 'heading';
export const kRmdchunkOutlineItemType = 'rmd_chunk';
export const kYamlMetadataOutlineItenItem = 'yaml_metadata';

export type EditorOutlineItemType = 'heading' | 'rmd_chunk' | 'yaml_metadata';

export type EditorOutline = EditorOutlineItem[];


export function outlineNodes(doc: ProsemirrorNode) {
  return findTopLevelBodyNodes(doc, isOutlineNode);
}

export function isOutlineNode(node: ProsemirrorNode) {
  if (node.type.spec.attrs) {
    return node.type.spec.attrs.hasOwnProperty('navigation_id');
  } else {
    return false;
  }
}