/*
 * editor-reducer.ts
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

import { Reducer } from 'redux';

import { EditorState, EditorActions, EditorActionTypes } from './editor-types';

const initialEditorState: EditorState = {
  title: '',
  markdown: '',
  outline: [],
  selection: {},
};

export const editorReducer: Reducer<EditorState, EditorActions> = (state = initialEditorState, action) => {
  switch (action.type) {
    case EditorActionTypes.SET_TITLE: {
      return {
        ...state,
        title: action.title,
      };
    }
    case EditorActionTypes.SET_MARKDOWN: {
      return {
        ...state,
        markdown: action.markdown,
      };
    }
    case EditorActionTypes.SET_OUTLINE: {
      return {
        ...state,
        outline: action.outline,
      };
    }
    case EditorActionTypes.SET_SELECTION: {
      return {
        ...state,
        selection: action.selection,
      };
    }
  }
  return state || initialEditorState;
};
