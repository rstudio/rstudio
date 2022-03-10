/*
 * preload.ts
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
import { contextBridge, ipcRenderer } from 'electron';
import { existsSync } from 'fs';
import path from 'path';

export interface Callbacks {
  useDefault32bit(): void;
  useDefault64bit(): void;
  use(version: string): void;
  cancel(): void;
  browse(): Promise<boolean>;
}

ipcRenderer.on('css', (event, data) => {
  const styleEl = document.createElement('style');
  styleEl.setAttribute('language', 'text/css');
  styleEl.innerText = data;
  document.head.appendChild(styleEl);
});

// initialize select input
ipcRenderer.on('initialize', (event, data) => {
  // cast received data
  const rInstalls = data as string[];
  console.log(rInstalls);

  // get access to the select element
  const selectEl = document.getElementById('select') as HTMLSelectElement;

  // keep track of which R installations we've already visited,
  // in case the registry had multiple or duplicate copies
  const visitedInstallations: { [index: string]: boolean } = {};

  // sort so that newer versions are shown first
  rInstalls.sort((lhs, rhs) => {
    return rhs.localeCompare(lhs);
  });

  rInstalls.forEach((rInstall) => {
    // normalize separators, etc
    rInstall = path.normalize(rInstall).replace(/[/\\]+$/g, '');

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
    }

    // check for 32 bit executable
    const r32 = `${rInstall}/bin/i386/R.exe`;
    if (existsSync(r32)) {
      const optionEl = window.document.createElement('option');
      optionEl.value = r32;
      optionEl.innerText = `[32-bit] ${rInstall}`;
      selectEl.appendChild(optionEl);
    }
  });
});

// export callbacks
const callbacks: Callbacks = {
  useDefault32bit: () => {
    ipcRenderer.send('use-default-32bit');
  },

  useDefault64bit: () => {
    ipcRenderer.send('use-default-64bit');
  },

  use: (version: string) => {
    ipcRenderer.send('use-custom', version);
  },

  cancel: () => {
    ipcRenderer.send('cancel');
  },

  browse: async () => {
    const shouldCloseDialog = await ipcRenderer.invoke('browse-r-exe');
    return shouldCloseDialog;
  },
};

contextBridge.exposeInMainWorld('callbacks', callbacks);
