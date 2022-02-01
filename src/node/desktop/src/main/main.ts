/*
 * main.ts
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

import { app } from 'electron';

import { ConsoleLogger } from '../core/console-logger';
import { logLevel, LogLevel, parseCommandLineLogLevel, setLogger, setLoggerLevel } from '../core/logger';
import { safeError } from '../core/err';

import { Application, kLogLevel } from './application';
import { setApplication } from './app-state';
import { parseStatus } from './program-status';
import { createStandaloneErrorDialog } from './utils';

import { initI18n } from './i18n-manager';
import i18next from 'i18next';

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
// eslint-disable-next-line @typescript-eslint/no-var-requires
if (require('electron-squirrel-startup') as boolean) {
  app.quit();
}

/**
 * RStudio entrypoint
 */
class RStudioMain {
  async main(): Promise<void> {
    try {
      await this.startup();
    } catch (error: unknown) {
      const err = safeError(error);
      await createStandaloneErrorDialog(i18next.t('unhandledException'), err.message);
      console.error(err.message); // logging possibly not available this early in startup
      if (logLevel() === LogLevel.DEBUG) {
        console.error(err.stack);
      }
      app.exit(1);
    }
  }

  private async startup(): Promise<void> {
    // NOTE: On Linux it looks like Electron prefers using ANGLE for GPU rendering;
    // however, we've seen in at least one case (Ubuntu 20.04 in Parallels VM) fails
    // to render in that case (we just get a white screen). Prefer 'desktop' by default,
    // but we'll need to respect the user-defined property as well.
    if (process.platform === 'linux') {
      if (!app.commandLine.hasSwitch('use-gl')) {
        app.commandLine.appendSwitch('use-gl', 'desktop');
      }
    }

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
initI18n();

const main = new RStudioMain();
void main.main();
