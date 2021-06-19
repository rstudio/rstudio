/*
 * main.ts
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

import { app, dialog } from 'electron';
import { Application } from './application';
import { setApplication } from './app-state';

/**
 * RStudio entrypoint
 */
class RStudioMain {

  async main(): Promise<void> {
    try {
      await this.startup();
    } catch (error) {
      if (!app.isPackaged) {
        dialog.showErrorBox('Unhandled exception', error.message);
      }
      console.error(error.message);
      app.exit(1);
    }
  }

  private async startup(): Promise<void> {
    const rstudio = new Application();
    setApplication(rstudio);

    const initStatus = await rstudio.beforeAppReady();
    if (initStatus.exit) {
      app.exit(initStatus.exitCode);
    } else {
      app.whenReady().then(() => {
        rstudio.run();
      });
    }
  }
}

// Startup
const main = new RStudioMain();
main.main();
