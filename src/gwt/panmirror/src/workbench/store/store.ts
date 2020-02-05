/*
 * store.ts
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

import { combineReducers, createStore, Store, applyMiddleware } from 'redux';

import { editorReducer } from './editor/editor-reducer';
import { EditorState } from './editor/editor-types';

import { prefsReducer, prefsMiddleware } from './prefs/prefs-reducer';
import { PrefsState } from './prefs/prefs-types';

export interface WorkbenchState {
  editor: EditorState;
  prefs: PrefsState;
}

const rootReducer = combineReducers<WorkbenchState>({
  editor: editorReducer,
  prefs: prefsReducer,
});

export function configureStore(): Store<WorkbenchState> {
  const store = createStore(rootReducer, applyMiddleware(prefsMiddleware()));
  return store;
}
