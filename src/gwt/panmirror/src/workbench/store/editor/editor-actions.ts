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
