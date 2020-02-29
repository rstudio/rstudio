/*
 * prefs-types.ts
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
