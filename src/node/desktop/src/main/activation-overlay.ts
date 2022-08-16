/*
 * activation-overlay.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { BrowserWindow } from 'electron';
import { EventEmitter } from 'events';

export class DesktopActivation extends EventEmitter {
  static LAUNCH_FIRST_SESSION = 'desktop-activation-launch_first_session';
  static LAUNCH_ERROR = 'desktop-activation-launch_error';

  getInitialLicense(): void {
    this.emitLaunchFirstSession();
  }

  allowProductUsage(): boolean {
    return true;
  }

  /**
   * @returns License state description if expired or within certain time window before
   * expiring, otherwise empty string.
   */
  currentLicenseStateMessage(): string {
    // TODO - reimplement
    return '';
  }

  /**
   * @returns Description of license state
   */
  licenseStatus(): string {
    // TODO - reimplement
    return '';
  }

  /**
   * Set main window, so we can supply it as default parent of message boxes
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setMainWindow(window: BrowserWindow): void {}

  /**
   * @returns Name of product edition, for use in UI
   */
  editionName(): string {
    return 'RStudio';
  }

  /**
   * license has been lost while using the program
   */
  emitLicenseLostSignal(): void {}

  /**
   * no longer need to show a license warning bar
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  emitUpdateLicenseWarningBarSignal(message: string): void {}

  /**
   * start a session after validating initial license
   */
  emitLaunchFirstSession(): void {
    this.emit(DesktopActivation.LAUNCH_FIRST_SESSION);
  }

  /**
   * show a messagebox (if message is non-empty) then exit the program
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  emitLaunchError(message: string): void {
    this.emit(DesktopActivation.LAUNCH_ERROR, message);
  }

  /**
   * detect (or re-detect) license status
   */
  emitDetectLicense(): void {}
}
