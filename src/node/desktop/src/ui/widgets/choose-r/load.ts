/*
 * load.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import { checkForNewLanguage } from '../../utils';
import { logString } from '../../renderer-logging';

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
const radioChoose32 = document.getElementById('use-default-32') as HTMLInputElement;
const radioChoose64 = document.getElementById('use-default-64') as HTMLInputElement;
const radioButtons = document.querySelectorAll('input[type="radio"]');

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

buttonOk.addEventListener('click', accept);
radioChoose32.addEventListener('input', validate);
radioChoose64.addEventListener('input', validate);
radioChooseCustom.addEventListener('input', validate);
selectWidget.addEventListener('input', validate);

buttonCancel.addEventListener('click', closeWindow);

let isBrowseDialogOpen = false;
buttonBrowse.addEventListener('click', async () => {
  isBrowseDialogOpen = true;
  try {
    const shouldCloseDialog = await window.callbacks.browse(callbackData());

    if (shouldCloseDialog) {
      window.close();
    }
  } catch (err) {
    logString('debug', `Error occurred when trying to browse for R: ${err}`);
  } finally {
    /** 
    * Without this timeout, the Choose R Modal will also be closed together with the Browse Dialog.
    * As the modal keeps focused while interacting with the Browse Dialog,
    * as soon as the dialog is closed, the `Esc` keypress event will also be triggered in the modal.
    */
    setTimeout(() => {
      isBrowseDialogOpen = false;
    }, 150);
  }
});

window.addEventListener('load', () => {
  window.addEventListener(
    'keyup',
    (event) => {
      switch (event.key) {
        case 'Enter':
          if (document.activeElement !== buttonBrowse) {
            void accept();
          }
          break;
        case 'Escape':
          if (!isBrowseDialogOpen) {
            closeWindow();
          }
          break;
      }
    },
    true,
  );

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

function closeWindow() {
  window.callbacks.cancel();
  window.close();
}

async function validate() {
  buttonOk.disabled = !((selectWidget.value)
    || radioChoose32.checked
    || radioChoose64.checked);
}

async function accept() {
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
}
