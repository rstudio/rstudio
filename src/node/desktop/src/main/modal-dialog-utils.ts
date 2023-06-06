/*
 * modal-dialog-utils.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

import { appState } from './app-state';

export class Dialog {
  public static async showDialogAsync<T>(func: () => Promise<T>): Promise<T> {
    appState().modalTracker.addElectronModal();
    const retVal = await func();
    appState().modalTracker.removeElectronModal();
    return retVal;
  }

  public static showDialogSync<T>(func: () => T): T {
    appState().modalTracker.addElectronModal();
    const retVal = func();
    appState().modalTracker.removeElectronModal();
    return retVal;
  }
}
