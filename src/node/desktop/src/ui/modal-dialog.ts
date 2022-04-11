/*
 * modal-dialog.ts
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

import { BrowserWindow, ipcMain } from 'electron';
import { err, Expected, ok } from '../core/expected';
import { safeError } from '../core/err';

export abstract class ModalDialog<T> extends BrowserWindow {

  abstract onShowModal(): Promise<T>;

  private readonly _widgetUrl : string;
  private _ipcMainChannels : string[];

  constructor(url: string, preload: string) {
    super({
      minWidth: 400,
      minHeight: 400,
      width: 400,
      height: 400,
      show: false,
      webPreferences: {
        preload: preload,
      },
    });

    // initialize instance variables
    this._widgetUrl = url;
    this._ipcMainChannels = [];

    // remove any registered ipc handlers on close
    this.on('closed', () => {
      for (const channel of this._ipcMainChannels) {
        ipcMain.removeHandler(channel);
      }
    });

    // make this look and behave like a modal
    this.setMenu(null);
    this.setMinimizable(false);
    this.setMaximizable(false);
    this.setFullScreenable(false);
  }

  // a helper function for registering handles on ipcMain,
  // with the registered handlers being cleaned up on close
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  addIpcHandler(channel: string, callback: (event: Electron.IpcMainInvokeEvent, ...args: any[]) => any) {
    ipcMain.handle(channel, callback);
    this._ipcMainChannels.push(channel);
  }

  async showModal(): Promise<Expected<T>> {
    try {
      const result = await this.showModalImpl();
      return ok(result);
    } catch (error: unknown) {
      return err(safeError(error));
    }
  }

  async showModalImpl(): Promise<T> {

    // load the associated HTML
    await this.loadURL(this._widgetUrl);

    // show the window after loading everything
    this.show();

    // invoke derived class's callback and return the response
    return this.onShowModal();

  }
}
