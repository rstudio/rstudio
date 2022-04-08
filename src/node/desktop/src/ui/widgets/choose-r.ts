/*
 * choose-r.ts
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

import { dialog, ipcMain } from 'electron';
import { findDefault32Bit, findDefault64Bit } from '../../main/detect-r';
import { logger } from '../../core/logger';
import { ModalDialog } from '../modal-dialog';

import { initI18n } from '../../main/i18n-manager';
import i18next from 'i18next';
import { CallbackData } from './choose-r/preload';
import { ElectronDesktopOptions } from '../../main/preferences/electron-desktop-options';

declare const CHOOSE_R_WEBPACK_ENTRY: string;
declare const CHOOSE_R_PRELOAD_WEBPACK_ENTRY: string;

export class ChooseRModalWindow extends ModalDialog<CallbackData | null> {
  private rInstalls: string[];

  constructor(rInstalls: string[]) {
    super(CHOOSE_R_WEBPACK_ENTRY, CHOOSE_R_PRELOAD_WEBPACK_ENTRY);
    this.rInstalls = rInstalls;
    initI18n();
  }

  async onShowModal(): Promise<CallbackData | null> {
    
    // initialize the window
    this.webContents.send('initialize', {
      rInstalls: this.rInstalls,
      renderingEngine: ElectronDesktopOptions().renderingEngine()
    });

    // listen for messages from the window
    return new Promise((resolve) => {
      ipcMain.on('use-default-32bit', (event, data: CallbackData) => {
        const installPath = findDefault32Bit();
        data.binaryPath = `${installPath}/bin/i386/R.exe`;
        logger().logDebug(`Using default 32-bit R (${data.binaryPath})`);
        return resolve(data);
      });

      ipcMain.on('use-default-64bit', (event, data: CallbackData) => {
        const installPath = findDefault64Bit();
        data.binaryPath = `${installPath}/bin/x64/R.exe`;
        logger().logDebug(`Using default 64-bit R (${data.binaryPath})`);
        return resolve(data);
      });

      ipcMain.on('use-custom', (event, data: CallbackData) => {
        logger().logDebug(`Using user-selected version of R (${data.binaryPath})`);
        return resolve(data);
      });

      ipcMain.handle('browse-r-exe', async (event, data: CallbackData) => {
        const response = dialog.showOpenDialogSync(this, {
          title: i18next.t('uiFolder.chooseRExecutable'),
          properties: ['openFile'],
          filters: [{ name: i18next.t('uiFolder.rExecutable'), extensions: ['exe'] }],
        });

        if (response) {
          data.binaryPath = response[0];
          logger().logDebug(`Using user-selected version of R (${data.binaryPath})`);
          resolve(data);
        }

        return !!response;
      });

      ipcMain.on('cancel', () => {
        return resolve(null);
      });

      this.on('closed', () => {
        return resolve(null);
      });
    });
  }
}
