/*
 * application-launch.ts
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

import { MainWindow } from './main-window';

/**
 * Not clear yet if we'll need this class from the Qt implementation, but keeping
 * it for now. If it ends up being useful, probably need to create an interface
 * describing behavior (for easier unit testing).
 */
export class ApplicationLaunch {
  mainWindow?: MainWindow;
  static init(): ApplicationLaunch {
    return new ApplicationLaunch();
  }

  setActivationWindow(window: MainWindow): void {
    this.mainWindow = window;
  }

  activateWindow(): void {
    // TODO - reimplement (if needed at all)
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  launchRStudio(args: string[], initialDir: string): void {
    // TODO - reimplement
  }
}
