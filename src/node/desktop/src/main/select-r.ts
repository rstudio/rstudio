/*
 * select-r.ts
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

import { BrowserWindow, ipcMain } from 'electron';
import { existsSync } from 'fs';
import path from 'path';
import { logger } from '../core/logger';
import { findDefault32Bit, findDefault64Bit, findRInstallationsWin32 } from './detect-r';

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
      const html = `<option value="${r64}">[64-bit] ${rInstall}</option>`;
      rListEntries.push(html);
    }

    // check for 32 bit executable
    const r32 = `${rInstall}/bin/i386/R.exe`;
    if (existsSync(r32)) {
      const html = `<option value="${r32}">[32-bit] ${rInstall}</option>`;
      rListEntries.push(html);
    }

  });

  // collapse into single HTML string
  const rOptionsHtml = rListEntries.join('');

  return String.raw`<!DOCTYPE html>
<html>
  <head>

    <title>Choose R Installation</title>

    <style>
    
    body {
      background-color: rgb(240, 240, 240);
      font-size: 9pt;
      font-family: "Helvetica", "Arial", sans-serif;
    }

    label {
      vertical-align: 2px;
    }

    .select {
      margin-top: 4px;
      width: 100%;
    }

    .select option {
      font-size: 9pt;
      padding: 2px 4px;
    }

    .footer {
      position: fixed;
      bottom: 8px;
      right: 8px;
    }

    .button {
      width: 96px;
      padding: 4px 8px;
      font-size: 9pt;
    }

    </style>

  </head>

  <body>

    <p>RStudio requires an existing installation of R.</p>
    <p>Please select the version of R to use.</p>

    <div>
      <input type="radio" id="use-default-64" name="r" value="use-default-64" checked>
      <label for="use-default-64">Use your machine's default 64-bit version of R</label>
    </div>

    <div>
      <input type="radio" id="use-default-32" name="r" value="use-default-32">
      <label for="use-default-32">Use your machine's default 32-bit version of R</label>
    </div>

    <div>
      <input type="radio" id="use-custom" name="r" value="use-custom">
      <label for="use-custom">Choose a specific version of R:</label>
    </div>

    <select id="select" class="select" name="select" size="5">
    ${rOptionsHtml}
    </select>

    <div class="footer">
      <button id="button-ok" class="button" type="button">OK</button>
      <button id="button-cancel" class="button" type="button">Cancel</button>
    </div>

    <script type="text/javascript">

    // ensure that the custom select box is only enabled when the associated
    // radio button is checked
    const selectWidget = document.getElementById("select");
    const radioButtons = document.querySelectorAll("input[type='radio']");
    const radioChooseCustom = document.getElementById("use-custom");

    selectWidget.disabled = !radioChooseCustom.checked;
    for (const radioButton of radioButtons) {
      radioButton.addEventListener("click", function(event) {
        selectWidget.disabled = !radioChooseCustom.checked;
      })
    }

    // set up callbacks for OK + Cancel buttons
    const buttonOk = document.getElementById("button-ok");
    const buttonCancel = document.getElementById("button-cancel");

    buttonOk.addEventListener("click", function(event) {

      var useDefault32 = document.getElementById("use-default-32");
      if (useDefault32.checked) {
         window.callbacks.useDefault32bit();
         window.close();
         return;
      }

      var useDefault64 = document.getElementById("use-default-64");
      if (useDefault64.checked) {
        window.callbacks.useDefault64bit();
        window.close();
        return;
      }

      var choose = document.getElementById("use-custom");
      if (choose.checked) {
        var selectWidget = document.getElementById("select");
        var selection = selectWidget.value;
        window.callbacks.use(selection);
        window.close();
      }

    });

    buttonCancel.addEventListener("click", function(event) {
      window.callbacks.cancel();
      window.close();
    });

    </script>

  </body>

</html>
`;

}

export async function chooseRInstallation(): Promise<string> {

  const dialog = new BrowserWindow({
    center: true,
    width: 400,
    height: 340,
    minWidth: 400,
    minHeight: 340,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'select-r.preload.js'),
    },
  });

  // make this look and behave like a modal
  dialog.setMenu(null);
  dialog.setFullScreenable(false);
  dialog.setMinimizable(false);
  dialog.setMaximizable(false);

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
