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

import { ConsoleLogger } from '../core/console-logger';
import { LogLevel, parseCommandLineLogLevel, setLogger, setLoggerLevel } from '../core/logger';

import { Application, kLogLevel } from './application';
import { setApplication } from './app-state';
import { parseStatus } from './program-status';

/**
 * RStudio entrypoint
 */
class RStudioMain {

  async main(): Promise<void> {
    try {
      await this.startup();
    } catch (error) {
      dialog.showErrorBox('Unhandled Exception', error.message);
      console.error(error.message); // logging possibly not available this early in startup
      app.exit(1);
    }
  }

  private async startup(): Promise<void> {
    const rstudio = new Application();
    setLogger(new ConsoleLogger());
    setLoggerLevel(parseCommandLineLogLevel(app.commandLine.getSwitchValue(kLogLevel), LogLevel.ERR));
    setApplication(rstudio);

    if (!parseStatus(await rstudio.beforeAppReady())) {
      return;
    }

    await app.whenReady();
    if (!parseStatus(await rstudio.run())) {
      return;
    }
  }
}

// Startup
const main = new RStudioMain();
main.main();
