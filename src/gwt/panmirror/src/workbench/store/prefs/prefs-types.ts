export enum PrefsActionTypes {
  SET_SHOW_OUTLINE = 'PREFS/SET_SHOW_OUTLINE',
  SET_SHOW_MARKDOWN = 'PREFS/SET_SHOW_MARKDOWN',
}

export interface PrefsSetShowOutlineAction {
  type: PrefsActionTypes.SET_SHOW_OUTLINE;
  showOutline: boolean;
}

export interface PrefsSetShowMarkdownAction {
  type: PrefsActionTypes.SET_SHOW_MARKDOWN;
  showMarkdown: boolean;
}

export interface PrefsState {
  readonly showOutline: boolean;
  readonly showMarkdown: boolean;
}

export type PrefsActions = PrefsSetShowOutlineAction | PrefsSetShowMarkdownAction;
