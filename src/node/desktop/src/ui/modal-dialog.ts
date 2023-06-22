/*
 * modal-dialog.ts
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

import { BrowserWindow, BrowserWindowConstructorOptions, ipcMain } from 'electron';
import { err, Expected, ok } from '../core/expected';
import { safeError } from '../core/err';
import { getenv } from '../core/environment';
import { appState } from '../main/app-state';

export abstract class ModalDialog<T> extends BrowserWindow {
  abstract onShowModal(): Promise<T>;

  private readonly _widgetUrl: string;
  private _ipcMainChannels: string[];

  constructor(url: string, preload: string, parentWindow: BrowserWindow | null = null) {
    let options: BrowserWindowConstructorOptions = {
      minWidth: 450,
      minHeight: 400,
      width: 450,
      height: 400,
      show: false,
      webPreferences: {
        nodeIntegration: false,
        preload: preload,
      },
    };

    if (parentWindow !== null) {
      options = { ...options, parent: parentWindow, modal: true };
    }

    super(options);

    // initialize instance variables
    this._widgetUrl = url;
    this._ipcMainChannels = [];

    // remove any registered ipc handlers on close
    this.on('closed', async () => {
      await appState().modalTracker.removeModalDialog(this);
      for (const channel of this._ipcMainChannels) {
        ipcMain.removeHandler(channel);
      }
    });

    this.on('show', async () => {
      await appState().modalTracker.addModalDialog(this);
    });

    // make this look and behave like a modal
    this.setMenuBarVisibility(false);
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

    const showDevTools = getenv('RSTUDIO_DESKTOP_MODAL_DEVTOOLS').length !== 0;
    if (showDevTools) {
      this.webContents.openDevTools();
    }

    // invoke derived class's callback and return the response
    return this.onShowModal();
  }
}
