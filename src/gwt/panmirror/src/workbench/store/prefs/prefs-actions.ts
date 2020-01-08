import { ActionCreator } from 'redux';

import { PrefsActionTypes, PrefsSetShowOutlineAction, PrefsSetShowMarkdownAction } from './prefs-types';

export const setPrefsShowOutline: ActionCreator<PrefsSetShowOutlineAction> = (showOutline: boolean) => ({
  type: PrefsActionTypes.SET_SHOW_OUTLINE,
  showOutline,
});

export const setPrefsShowMarkdown: ActionCreator<PrefsSetShowMarkdownAction> = (showMarkdown: boolean) => ({
  type: PrefsActionTypes.SET_SHOW_MARKDOWN,
  showMarkdown,
});
