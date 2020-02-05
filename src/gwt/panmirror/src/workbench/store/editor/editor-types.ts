/*
 * editor-types.ts
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

import { EditorOutline } from 'editor/src/api/outline';

export enum EditorActionTypes {
  SET_TITLE = 'EDITOR/SET_TITLE',
  SET_MARKDOWN = 'EDITOR/SET_MARKDOWN',
  SET_OUTLINE = 'EDITOR/SET_OUTLINE',
  SET_SELECTION = 'EDITOR/SET_SELECTION',
}

export interface EditorSetTitleAction {
  type: EditorActionTypes.SET_TITLE;
  title: string;
}

export interface EditorSetMarkdownAction {
  type: EditorActionTypes.SET_MARKDOWN;
  markdown: string;
}

export interface EditorSetOutlineAction {
  type: EditorActionTypes.SET_OUTLINE;
  outline: EditorOutline;
}

export interface EditorSetSelectionAction {
  type: EditorActionTypes.SET_SELECTION;
  selection: {};
}

export interface EditorState {
  readonly title: string;
  readonly markdown: string;
  readonly outline: EditorOutline;
  readonly selection: {};
}

export type EditorActions =
  | EditorSetTitleAction
  | EditorSetMarkdownAction
  | EditorSetOutlineAction
  | EditorSetSelectionAction;
