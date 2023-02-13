/*
 * choose-r.ts
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

import { BrowserWindow, dialog } from 'electron';
import { detectREnvironment, findDefault32Bit, findDefault64Bit, isValidInstallation } from '../../main/detect-r';
import { logger } from '../../core/logger';
import { ModalDialog } from '../modal-dialog';

import { initI18n } from '../../main/i18n-manager';
import i18next, { t } from 'i18next';
import { CallbackData } from './choose-r/preload';
import { ElectronDesktopOptions } from '../../main/preferences/electron-desktop-options';

declare const CHOOSE_R_WEBPACK_ENTRY: string;
declare const CHOOSE_R_PRELOAD_WEBPACK_ENTRY: string;

function checkValid(data: CallbackData) {
  // get path to R
  const rBinaryPath = data.binaryPath as string;

  // try to run it
  const [rEnvironment, err] = detectREnvironment(rBinaryPath);

  if (err) {
    // something went wrong; let the user know they can't use
    // this version of R with RStudio
    logger().logErrorMessage(`Selected R path: ${rBinaryPath ?? 'no path chosen'}`);
    logger().logError(err);

    dialog.showMessageBoxSync({
      type: 'error',
      title: t('chooseRDialog.rLaunchFailedTitle'),
      message: t('chooseRDialog.rLaunchFailedMessage'),
      buttons: [ t('common.buttonOk'), ],
    });

    return false;
  }

  logger().logDebug(`Validated R: ${rEnvironment.rScriptPath}`);
  logger().logDebug(JSON.stringify(rEnvironment));
  return true;
}

export class ChooseRModalWindow extends ModalDialog<CallbackData | null> {
  private rInstalls: string[];

  constructor(rInstalls: string[], parentWindow: BrowserWindow | null = null) {
    super(CHOOSE_R_WEBPACK_ENTRY, CHOOSE_R_PRELOAD_WEBPACK_ENTRY, parentWindow);

    this.rInstalls = rInstalls;

    initI18n();
  }

  async maybeResolve(resolve: (data: CallbackData) => void, data: CallbackData) {
    if (checkValid(data)) {
      resolve(data);
      return true;
    } else {
      return false;
    }
  }

  async onShowModal(): Promise<CallbackData | null> {
    const r32 = findDefault32Bit();
    const r64 = findDefault64Bit();
    const initData = {
      default32bitPath: isValidInstallation(r32) ? r32 : '',
      default64bitPath: isValidInstallation(r64) ? r64 : '',
      rInstalls: this.rInstalls,
      renderingEngine: ElectronDesktopOptions().renderingEngine(),
      selectedRVersion: ElectronDesktopOptions().rExecutablePath(),
    };

    this.webContents.send('initialize', initData);

    // listen for messages from the window
    return new Promise((resolve) => {
      this.addIpcHandler('use-default-32bit', async (event, data: CallbackData) => {
        const installPath = initData.default32bitPath;
        data.binaryPath = `${installPath}/bin/i386/R.exe`;
        logger().logDebug(`Using default 32-bit version of R (${data.binaryPath})`);
        return this.maybeResolve(resolve, data);
      });

      this.addIpcHandler('use-default-64bit', async (event, data: CallbackData) => {
        const installPath = initData.default64bitPath;
        data.binaryPath = `${installPath}/bin/x64/R.exe`;
        logger().logDebug(`Using default 64-bit version of R (${data.binaryPath})`);
        return this.maybeResolve(resolve, data);
      });

      this.addIpcHandler('use-custom', async (event, data: CallbackData) => {
        logger().logDebug(`Using user-selected version of R (${data.binaryPath})`);
        return this.maybeResolve(resolve, data);
      });

      this.addIpcHandler('browse-r-exe', async (event, data: CallbackData) => {
        const response = dialog.showOpenDialogSync(this, {
          title: i18next.t('uiFolder.chooseRExecutable'),
          properties: ['openFile'],
          filters: [{ name: i18next.t('uiFolder.rExecutable'), extensions: ['exe'] }],
        });

        if (response) {
          data.binaryPath = response[0];
          logger().logDebug(`Using user-selected version of R (${data.binaryPath})`);
          return this.maybeResolve(resolve, data);
        }

        return false;
      });

      this.addIpcHandler('cancel', () => {
        return resolve(null);
      });

      this.on('closed', () => {
        return resolve(null);
      });
    });
  }
}
