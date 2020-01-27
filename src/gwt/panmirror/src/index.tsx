/*
 * index.tsx
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import React from 'react';
import ReactDOM from 'react-dom';

import { i18nInit } from './i18n';

import axios from 'axios';

import Workbench from './workbench/Workbench';

import { configureStore } from 'workbench/store/store';

import { setEditorMarkdown } from 'workbench/store/editor/editor-actions';

import 'normalize.css/normalize.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import '@blueprintjs/icons/lib/css/blueprint-icons.css';

async function runApp() {
  try {
    // configure store
    const store = configureStore();

    // initialize w/ demo content
    const contentUrl = `content/${window.location.search.slice(1) || 'content.md'}`;
    const result = await axios.get(contentUrl);
    store.dispatch(setEditorMarkdown(result.data));

    // init localization
    await i18nInit();

    // render app
    ReactDOM.render(<Workbench store={store} />, document.getElementById('root'));
  } catch (error) {
    alert(error.message);
  }
}

runApp();
