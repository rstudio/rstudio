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
 * Track modals that extend ModalDialog (src/node/desktop/src/ui/modal-dialog.ts)
 * Based on src/gwt/src/org/rstudio/core/client/widget/ModalDialogTracker.java
 */
export class ModalDialogTracker {
  private modals: ModalDialog<any>[] = [];
  private gwtModalsShowing = 0;

  public async addModal(modal: ModalDialog<any>) {
    this.modals.push(modal);
    appState().gwtCallback?.mainWindow.menuCallback.setMainMenuEnabled(false);
  }

  public async removeModal(modal: ModalDialog<any>) {
    this.modals = this.modals.filter((m) => m !== modal);
    if (this.numModalsShowing() === 0) {
      appState().gwtCallback?.mainWindow.menuCallback.setMainMenuEnabled(true);
    }
  }

  public numModalsShowing(): number {
    return this.modals.length + this.gwtModalsShowing;
  }

  public setNumGwtModalsShowing(gwtModalsShowing: number) {
    this.gwtModalsShowing = gwtModalsShowing;
  }
}
