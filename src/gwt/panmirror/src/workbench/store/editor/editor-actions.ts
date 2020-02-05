/*
 * editor-actions.ts
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

import { ActionCreator } from 'redux';

import { EditorOutline } from 'editor/src/api/outline';

import {
  EditorActionTypes,
  EditorSetTitleAction,
  EditorSetSelectionAction,
  EditorSetMarkdownAction,
  EditorSetOutlineAction,
} from './editor-types';

export const setEditorTitle: ActionCreator<EditorSetTitleAction> = (title: string) => ({
  type: EditorActionTypes.SET_TITLE,
  title,
});

export const setEditorMarkdown: ActionCreator<EditorSetMarkdownAction> = (markdown: string) => ({
  type: EditorActionTypes.SET_MARKDOWN,
  markdown,
});

export const setEditorOutline: ActionCreator<EditorSetOutlineAction> = (outline: EditorOutline) => ({
  type: EditorActionTypes.SET_OUTLINE,
  outline,
});

export const setEditorSelection: ActionCreator<EditorSetSelectionAction> = (selection: {}) => ({
  type: EditorActionTypes.SET_SELECTION,
  selection,
});
