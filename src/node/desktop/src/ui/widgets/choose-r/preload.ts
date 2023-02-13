/*
 * preload.ts
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
import { contextBridge, ipcRenderer } from 'electron';
import { existsSync } from 'fs';
import path from 'path';
import { getDesktopLoggerBridge } from '../../../renderer/logger-bridge';
import { normalizeSeparatorsNative } from '../../utils';

export interface CallbackData {
  binaryPath?: string | unknown;
  renderingEngine?: string;
}

export interface Callbacks {
  useDefault32bit(data: CallbackData): Promise<boolean>;
  useDefault64bit(data: CallbackData): Promise<boolean>;
  use(data: CallbackData): Promise<boolean>;
  browse(data: CallbackData): Promise<boolean>;
  cancel(): void;
}

// this needs to be done in the preload of any widget wanting to use logging
contextBridge.exposeInMainWorld('desktopLogger', getDesktopLoggerBridge());

ipcRenderer.on('css', (_event, data) => {
  const styleEl = document.createElement('style');
  styleEl.setAttribute('language', 'text/css');
  styleEl.innerText = data;
  document.head.appendChild(styleEl);
});

// initialize select input
ipcRenderer.on('initialize', (_event, data) => {
  const use32 = document.getElementById('use-default-32') as HTMLInputElement;
  const use64 = document.getElementById('use-default-64') as HTMLInputElement;
  const useCustomEl = document.getElementById('use-custom') as HTMLInputElement;
  const selectCustom = document.getElementById('select') as HTMLSelectElement;
  const buttonOk = document.getElementById('button-ok') as HTMLButtonElement;

  // if we have a default 32-bit R installation, enable it
  const default32Bit = data.default32bitPath as string;
  let isDefault32Selected = false;
  if (default32Bit) {
    use32?.removeAttribute('disabled');

    if (isRVersionSelected('' + data.selectedRVersion, default32Bit + '/bin/i386/R.exe')) {
      use32.checked = true;
      isDefault32Selected = true;
    }
  }

  // if we have a default 64-bit R installation, enable it
  const default64Bit = data.default64bitPath as string;
  let isDefault64Selected = false;
  if (default64Bit) {
    use64?.removeAttribute('disabled');

    if (isRVersionSelected('' + data.selectedRVersion, default64Bit + '/bin/x64/R.exe')) {
      use64.checked = true;
      isDefault64Selected = true;
    }
  }

  // cast received data
  const rInstalls = data.rInstalls as string[];
  const renderingEngine = data.renderingEngine as string;

  // set the current rendering engine
  const renderingEngineEl = document.getElementById('rendering-engine') as HTMLSelectElement;
  renderingEngineEl.value = renderingEngine;

  // get access to the select element
  const selectEl = document.getElementById('select') as HTMLSelectElement;

  // keep track of which R installations we've already visited,
  // in case the registry had multiple or duplicate copies
  const visitedInstallations: { [index: string]: boolean } = {};

  // sort so that newer versions are shown first
  rInstalls.sort((lhs, rhs) => {
    return rhs.localeCompare(lhs);
  });

  const selectWidget = document.getElementById('select') as HTMLSelectElement;
  selectWidget.disabled = !(rInstalls.length > 0 && useCustomEl.checked);

  rInstalls.forEach((rInstall) => {
    // normalize separators, etc
    rInstall = normalizeSeparatorsNative(path.normalize(rInstall));

    // skip if we've already seen this
    if (visitedInstallations[rInstall]) {
      return;
    }
    visitedInstallations[rInstall] = true;

    // check for 64 bit executable
    const r64 = `${rInstall}/bin/x64/R.exe`;
    if (existsSync(r64)) {
      const optionEl = window.document.createElement('option');
      optionEl.value = r64;
      optionEl.innerText = `[64-bit] ${rInstall}`;
      selectEl.appendChild(optionEl);

      if (isRVersionSelected(data.selectedRVersion as string, r64) && !isDefault64Selected) {
        const useCustomRadioInput = document.getElementById('use-custom') as any;
        useCustomRadioInput.checked = true;

        selectEl.value = r64;

        selectWidget.disabled = false;
        selectWidget.focus();
      }
    }

    // check for 32 bit executable
    const r32 = `${rInstall}/bin/i386/R.exe`;
    if (existsSync(r32)) {
      const optionEl = window.document.createElement('option');
      optionEl.value = r32;
      optionEl.innerText = `[32-bit] ${rInstall}`;
      selectEl.appendChild(optionEl);

      if (isRVersionSelected(data.selectedRVersion as string, r32) && !isDefault32Selected) {
        const useCustomRadioInput = document.getElementById('use-custom') as any;
        useCustomRadioInput.checked = true;

        selectWidget.disabled = false;

        selectEl.value = r32;
        selectWidget.focus();
      }
    }
  });

  useCustomEl.checked = !default32Bit && !default64Bit && (rInstalls.length > 0);
  selectWidget.disabled = !useCustomEl.checked;

  buttonOk.disabled = !((useCustomEl.checked && selectCustom.value) || use32.checked || use64.checked) ;
});

// export callbacks
const callbacks: Callbacks = {
  useDefault32bit: async (data: CallbackData) => {
    const shouldCloseDialog = await ipcRenderer.invoke('use-default-32bit', data);
    return shouldCloseDialog;
  },

  useDefault64bit: async (data: CallbackData) => {
    const shouldCloseDialog = await ipcRenderer.invoke('use-default-64bit', data);
    return shouldCloseDialog;
  },

  use: async (data: CallbackData) => {
    const shouldCloseDialog = await ipcRenderer.invoke('use-custom', data);
    return shouldCloseDialog;
  },

  browse: async (data: CallbackData) => {
    const shouldCloseDialog = await ipcRenderer.invoke('browse-r-exe', data);
    return shouldCloseDialog;
  },

  cancel: () => {
    ipcRenderer.send('cancel');
  },
};

contextBridge.exposeInMainWorld('callbacks', callbacks);

function isRVersionSelected(selectedVersion: string, versionToCompare: string) {
  return normalizeSeparatorsNative(selectedVersion) === normalizeSeparatorsNative(versionToCompare);
}
