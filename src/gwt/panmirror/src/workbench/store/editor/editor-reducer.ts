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
