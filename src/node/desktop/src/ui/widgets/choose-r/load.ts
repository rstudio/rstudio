/*
 * load.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

// extend Window object with 'callbacks', injected via preload script
// eslint-disable-next-line @typescript-eslint/no-unused-vars
interface Window {
  callbacks: any;
}

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
const buttonOk = document.getElementById('button-ok')! as HTMLButtonElement;
const buttonCancel = document.getElementById('button-cancel') as HTMLButtonElement;

buttonOk.addEventListener('click', () => {

  const useDefault32Radio = document.getElementById('use-default-32')! as HTMLInputElement;
  if (useDefault32Radio.checked) {
    window.callbacks.useDefault32bit();
    window.close();
    return;
  }

  const useDefault64Radio = document.getElementById('use-default-64') as HTMLInputElement;
  if (useDefault64Radio.checked) {
    window.callbacks.useDefault64bit();
    window.close();
    return;
  }

  const useCustomRadio = document.getElementById('use-custom') as HTMLInputElement;
  if (useCustomRadio.checked) {
    const selectWidget = document.getElementById('select') as HTMLSelectElement;
    const selection = selectWidget.value;
    window.callbacks.use(selection);
    window.close();
  }

});

buttonCancel.addEventListener('click', () => {
  window.callbacks.cancel();
  window.close();
});

/*
import { BrowserWindow, ipcMain } from 'electron';
import { existsSync } from 'fs';
import path from 'path';
import { ModalWindow } from 'src/ui/modal';
import { logger } from '../../../core/logger';
import { findDefault32Bit, findDefault64Bit, findRInstallationsWin32 } from '../../../main/detect-r';

function buildHtmlContent(rInstalls: string[]): string {

  const visitedInstallations: { [index: string]: boolean } = {};
  const rListEntries: string[] = [];

  // sort so that newer versions are shown first
  rInstalls.sort((lhs, rhs) => {
    return rhs.localeCompare(lhs);
  });

  // make the HTML for each installation entry
  rInstalls.forEach(rInstall => {

    // normalize separators, etc
    rInstall = path.normalize(rInstall).replaceAll(/[/\\]+$/g, '');

    // skip if we've already seen this
    if (visitedInstallations[rInstall]) {
      return;
    }
    visitedInstallations[rInstall] = true;

    // check for 64 bit executable
    const r64 = `${rInstall}/bin/x64/R.exe`;
    if (existsSync(r64)) {
      const html = `<option value='${r64}'>[64-bit] ${rInstall}</option>`;
      rListEntries.push(html);
    }

    // check for 32 bit executable
    const r32 = `${rInstall}/bin/i386/R.exe`;
    if (existsSync(r32)) {
      const html = `<option value='${r32}'>[32-bit] ${rInstall}</option>`;
      rListEntries.push(html);
    }

  });

  // collapse into single HTML string
  const rOptionsHtml = rListEntries.join('');

}

export async function chooseRInstallation(): Promise<string> {

  const dialog = new ModalWindow();
  dialog.setWidth(400);
  dialog.setHeight(300);

  // find R installations, and generate the HTML using that
  const rInstalls = findRInstallationsWin32();
  const html = buildHtmlContent(rInstalls);

  // load the HTML
  await dialog.loadURL(`data:text/html;charset=utf-8,${html}`);

  // show the page
  dialog.show();

  return new Promise((resolve, reject) => {

    ipcMain.on('choose-r-dialog', (event, message, data) => {

      switch (message) {
        
      // use default 32bit version of R
      case 'use-default-32bit': {
        const path = findDefault32Bit();
        logger().logDebug(`Using default 32-bit R (${path})`);
        return resolve(`${path}/bin/i386/R.exe`);
      }

      // use default 64bit version of R
      case 'use-default-64bit': {
        const path = findDefault64Bit();
        logger().logDebug(`Using default 64-bit R (${path})`);
        return resolve(`${path}/bin/x64/R.exe`);
      }

      // use selected version of R
      case 'use-custom': {
        logger().logDebug(`Using user-selected version of R (${data})`);
        return resolve(data);
      }

      // user cancel
      case 'cancel': {
        return reject(new Error('The dialog was closed by the user.'));
      }

      }
    });

    dialog.on('closed', () => {
      reject(new Error('The dialog was closed by the user.'));
    });

  });

}
*/