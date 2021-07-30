/*
 * choose-r.ts
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

import { ipcMain } from 'electron';
import path from 'path';
import { findDefault32Bit, findDefault64Bit } from '../../main/detect-r';
import { logger } from '../../core/logger';
import { ModalWindow } from '../modal';

export class ChooseRModalWindow extends ModalWindow<string> {

  private rInstalls: string[];

  constructor(rInstalls: string[]) {
    super(path.join(__dirname, 'choose-r'));
    this.rInstalls = rInstalls;
  }

  async onShowModal(): Promise<string> {

    // initialize the select widget
    this.webContents.send('initialize', this.rInstalls);

    // listen for messages from the window
    return new Promise((resolve, reject) => {

      ipcMain.on('use-default-32bit', () => {
        const path = findDefault32Bit();
        logger().logDebug(`Using default 32-bit R (${path})`);
        return resolve(`${path}/bin/i386/R.exe`);
      });

      ipcMain.on('use-default-64bit', () => {
        const path = findDefault64Bit();
        logger().logDebug(`Using default 64-bit R (${path})`);
        return resolve(`${path}/bin/x64/R.exe`);
      });

      ipcMain.on('use-custom', (event, data) => {
        logger().logDebug(`Using user-selected version of R (${data})`);
        return resolve(data);
      });

      ipcMain.on('cancel', () => {
        return reject(new Error('The dialog was closed by the user.'));
      });

      this.on('closed', () => {
        reject(new Error('The dialog was closed by the user.'));
      });

    });

  }

}
