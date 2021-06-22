/*
 * activation-overlay.ts
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

import { EventEmitter } from 'events';

export enum ActivationEvents {
  LAUNCH_FIRST_SESSION = 'launchFirstSession',
  LAUNCH_ERROR = 'launchError'
}

/* eslint-disable @typescript-eslint/no-empty-function */
export class DesktopActivation extends EventEmitter {

  getInitialLicense(): void {
    this.emitLaunchFirstSession();
  }

  allowProductUsage(): boolean {
    return true;
  }

  editionName(): string {
    return 'RStudio';
  }

  // license has been lost while using the program
  emitLicenseLostSignal(): void {
  }

  // no longer need to show a license warning bar
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  emitUpdateLicenseWarningBarSignal(message: string): void {
  }

  // start a session after validating initial license
  emitLaunchFirstSession(): void {
    this.emit(ActivationEvents.LAUNCH_FIRST_SESSION);
  }

  // show a messagebox (if message is non-empty) then exit the program
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  emitLaunchError(message: string): void {
    this.emit(ActivationEvents.LAUNCH_ERROR, message);
  }

  // detect (or re-detect) license status
  emitDetectLicense(): void {
  }

}