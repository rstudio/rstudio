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
