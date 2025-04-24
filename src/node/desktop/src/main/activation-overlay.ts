/*
 * activation-overlay.ts
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

import { BrowserWindow } from 'electron';
import { EventEmitter } from 'events';
import { FilePath } from '../core/file-path';

export class DesktopActivation extends EventEmitter {
  static LAUNCH_FIRST_SESSION = 'desktop-activation-launch_first_session';
  static LAUNCH_ERROR = 'desktop-activation-launch_error';

  // pro-only start
  // pro-only end

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  getInitialLicense(installPath: FilePath, devMode: boolean): void {
    this.emitLaunchFirstSession();
  }

  /**
   * Set main window, so we can supply it as default parent of message boxes
   */
  setMainWindow(_window: BrowserWindow): void {
    /**/
  }

  /**
   * @returns Name of product edition, for use in UI
   */
  editionName(): string {
    return 'RStudio';
  }

  /**
   * start a session after validating initial license
   */
  emitLaunchFirstSession(): void {
    this.emit(DesktopActivation.LAUNCH_FIRST_SESSION);
  }

  /**
   * show a messagebox (if message is non-empty) then exit the program
   */
  emitLaunchError(message: string): void {
    this.emit(DesktopActivation.LAUNCH_ERROR, message);
  }

  // pro-only below
}
