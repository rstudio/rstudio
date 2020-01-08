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
