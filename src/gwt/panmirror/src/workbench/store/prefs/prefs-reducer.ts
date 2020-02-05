/*
 * prefs-reducer.ts
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

import { Reducer, MiddlewareAPI, Dispatch, Middleware } from 'redux';

import { PrefsState, PrefsActions, PrefsActionTypes } from './prefs-types';

export const prefsReducer: Reducer<PrefsState, PrefsActions> = (state, action) => {
  switch (action.type) {
    case PrefsActionTypes.SET_SHOW_OUTLINE: {
      return {
        ...state,
        showOutline: action.showOutline,
      };
    }
    case PrefsActionTypes.SET_SHOW_MARKDOWN: {
      return {
        ...state,
        showMarkdown: action.showMarkdown,
      };
    }
  }
  return state || loadPrefs();
};

// automatically save and load prefs from local storage

const kPrefsLocalStorage = 'panmirror_prefs';

export function prefsMiddleware() {
  const middleware: Middleware = ({ getState }: MiddlewareAPI) => (next: Dispatch) => action => {
    const result = next(action);
    if (Object.values(PrefsActionTypes).includes(action.type)) {
      const prefs = getState().prefs;
      try {
        const serializedPrefs = JSON.stringify(prefs);
        localStorage.setItem(kPrefsLocalStorage, serializedPrefs);
      } catch {
        // ignore write errors
      }
    }
    return result;
  };
  return middleware;
}

function loadPrefs() {
  const defaultPrefs = {
    showOutline: false,
    showMarkdown: false,
  };
  try {
    const serializedPrefs = localStorage.getItem(kPrefsLocalStorage);
    if (serializedPrefs === null) {
      return defaultPrefs;
    }
    return {
      ...defaultPrefs,
      ...JSON.parse(serializedPrefs),
    };
  } catch (err) {
    return defaultPrefs;
  }
}
