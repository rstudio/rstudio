/*
 * modal-dialog-tracker.ts
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

import { ModalDialog } from '../ui/modal-dialog';
import { appState } from './app-state';

/**
 * Tracks modals that should enable/disable the main menu, including:
 * - ModalDialog (src/node/desktop/src/ui/modal-dialog.ts) - eg. Choose R
 * - GWT modals - eg. Global Options
 * - Electron.dialog (native modals) - eg. dialog.showMessageBox(...)
 *
 * Based on src/gwt/src/org/rstudio/core/client/widget/ModalDialogTracker.java
 */
export class ModalDialogTracker {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private modals: ModalDialog<any>[] = [];
  private electronModalsShowing = 0;
  private gwtModalsShowing = 0;

  /**
   * Returns the number of modals showing across all modal types
   * @returns Number of modals showing across all modal types
   */
  public numModalsShowing(): number {
    return this.modals.length + this.gwtModalsShowing + this.electronModalsShowing;
  }

  /**
   * Re-enables the main menu if there are no modals showing
   */
  public maybeReenableMainMenu() {
    if (this.numModalsShowing() === 0) {
      appState().gwtCallback?.mainWindow.menuCallback.setMainMenuEnabled(true);
    }
  }

  /**
   * Adds a ModalDialog to tracking and disables the main menu
   * @param modal ModalDialog to add to tracking
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public async addModalDialog(modal: ModalDialog<any>) {
    this.modals.push(modal);
    appState().gwtCallback?.mainWindow.menuCallback.setMainMenuEnabled(false);
  }

  /**
   * Removes a ModalDialog from tracking and re-enables the main menu if applicable
   * @param modal ModalDialog to remove from tracking
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public async removeModalDialog(modal: ModalDialog<any>) {
    this.modals = this.modals.filter((m) => m !== modal);
    this.maybeReenableMainMenu();
  }

  /**
   * Sets the number of GWT modals showing
   * @param gwtModalsShowing Number of GWT modals showing
   */
  public setNumGwtModalsShowing(gwtModalsShowing: number) {
    this.gwtModalsShowing = gwtModalsShowing;
  }

  /**
   * Tracks a modal dialog that is shown by Electron.dialog (async) to ensure that the
   * main menu is disabled while the dialog is open and re-enabled when the
   * dialog is closed.
   * @param func async Electron.dialog function to call
   * @returns Promise that resolves to the return value of func
   */
  public async trackElectronModalAsync<T>(func: () => Promise<T>): Promise<T> {
    this.addElectronModal();
    return func().finally(() => this.removeElectronModal());
  }

  /**
   * Tracks a modal dialog that is shown by Electron.dialog (sync) to ensure that the
   * main menu is disabled while the dialog is open and re-enabled when the
   * dialog is closed.
   * @param func Electron.dialog function to call
   * @returns Return value of func
   */
  public trackElectronModalSync<T>(func: () => T): T {
    this.addElectronModal();
    const retVal = func();
    this.removeElectronModal();
    return retVal;
  }

  /**
   * Increments the number of Electron modals showing and disables the main menu
   */
  private addElectronModal() {
    this.electronModalsShowing++;
    appState().gwtCallback?.mainWindow.menuCallback.setMainMenuEnabled(false);
  }

  /**
   * Decrements the number of Electron modals showing and re-enables the main menu
   */
  private removeElectronModal() {
    if (this.electronModalsShowing > 0) this.electronModalsShowing--;
    this.maybeReenableMainMenu();
  }
}
