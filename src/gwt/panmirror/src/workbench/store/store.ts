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
