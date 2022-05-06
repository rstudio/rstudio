/*
 * load.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

/* eslint-disable @typescript-eslint/no-implicit-any-catch */
// eslint-disable-next-line @typescript-eslint/no-unused-vars

import { Callbacks, CallbackData } from './preload';
import { changeLanguage, initI18n, localize } from '../../../main/i18n-manager';

import './styles.css';
import { logger } from '../../../core/logger';
import { checkForNewLanguage } from '../../utils';

declare global {
  interface Window {
    callbacks: Callbacks;
  }
}

// load internationalization infrastructure
initI18n();

const updateLabels = () => {
  localize(document, 'chooseRDialog');
};

// ensure that the custom select box is only enabled when the associated
// radio button is checked
const selectWidget = document.getElementById('select') as HTMLSelectElement;
const radioChooseCustom = document.getElementById('use-custom') as HTMLInputElement;
const radioButtons = document.querySelectorAll('input[type="radio"]');

selectWidget.disabled = !radioChooseCustom.checked;
radioButtons.forEach((radioButton) => {
  radioButton.addEventListener('click', () => {
    selectWidget.disabled = !radioChooseCustom.checked;
  });
});

// set up callbacks for OK + Cancel buttons
const buttonOk = document.getElementById('button-ok') as HTMLButtonElement;
const buttonCancel = document.getElementById('button-cancel') as HTMLButtonElement;
const buttonBrowse = document.getElementById('button-browse') as HTMLButtonElement;

// get reference to rendering engine select widget
const renderingEngineSelect = document.getElementById('rendering-engine') as HTMLSelectElement;

// helper function for building response
function callbackData(binaryPath?: string): CallbackData {
  return {
    binaryPath: binaryPath,
    renderingEngine: renderingEngineSelect.value,
  };
}

buttonOk.addEventListener('click', async () => {
  const useDefault32Radio = document.getElementById('use-default-32') as HTMLInputElement;
  if (useDefault32Radio.checked) {
    const shouldCloseWindow = await window.callbacks.useDefault32bit(callbackData());
    if (shouldCloseWindow) {
      window.close();
    }
    return;
  }

  const useDefault64Radio = document.getElementById('use-default-64') as HTMLInputElement;
  if (useDefault64Radio.checked) {
    const shouldCloseWindow = await window.callbacks.useDefault64bit(callbackData());
    if (shouldCloseWindow) {
      window.close();
    }
    return;
  }

  const useCustomRadio = document.getElementById('use-custom') as HTMLInputElement;
  if (useCustomRadio.checked) {
    const selectWidget = document.getElementById('select') as HTMLSelectElement;
    const selection = selectWidget.value;
    const shouldCloseWindow = await window.callbacks.use(callbackData(selection));
    if (shouldCloseWindow) {
      window.close();
    }
    return;
  }
});

buttonCancel.addEventListener('click', () => {
  window.callbacks.cancel();
  window.close();
});

buttonBrowse.addEventListener('click', async () => {
  try {
    const shouldCloseDialog = await window.callbacks.browse(callbackData());
    if (shouldCloseDialog) {
      window.close();
    }
  } catch (err) {
    logger().logDebug(`Error occurred when trying to browse for R: ${err}`);
  }
});

window.addEventListener('load', () => {
  checkForNewLanguage()
    .then(async (newLanguage: any) =>
      changeLanguage('' + newLanguage).then(() => {
        updateLabels();
      }),
    )
    .catch((err: any) => {
      console.error('An error happened when trying to fetch a new locale: ', err);
    });
});
