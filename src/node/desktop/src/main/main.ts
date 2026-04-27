/*
 * main.ts
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

import { app } from 'electron';
import i18next from 'i18next';
import { safeError } from '../core/err';
import { logLevel, logger } from '../core/logger';
import { setApplication } from './app-state';
import { Application } from './application';
import { initI18n } from './i18n-manager';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { parseStatus } from './program-status';
import { createStandaloneErrorDialog } from './utils';
import { Xdg } from '../core/xdg';
import { existsSync, readFileSync } from 'fs';
import path from 'path';

/**
 * RStudio entrypoint
 */
class RStudioMain {
  async main(): Promise<void> {
    try {
      await this.startup();
    } catch (error: unknown) {
      const err = safeError(error);
      await app.whenReady(); // can't show upcoming error message window until app is ready
      await createStandaloneErrorDialog(i18next.t('mainTs.unhandledException'), err.message);
      console.error(err.message); // logging possibly not available this early in startup
      if (logLevel() === 'debug') {
        console.error(err.stack);
      }
      app.exit(1);
    }
  }

  private initializeAppConfig() {
    const configDirs = [Xdg.userConfigDir().getAbsolutePath(), app.getPath('appData')];
    for (const configDir of configDirs) {
      const configPath = path.join(configDir, 'electron-flags.conf');
      if (existsSync(configPath)) {
        logger().logDebug(`Using Electron flags from file ${configPath}`);
        const configContents = readFileSync(configPath, { encoding: 'utf-8' });
        const configLines = configContents.split(/\r?\n/);
        for (const configLine of configLines) {
          if (configLine.startsWith('--')) {
            logger().logDebug(`Appending switch: ${configLine}`);
            const equalsIndex = configLine.indexOf('=');
            if (equalsIndex !== -1) {
              const name = configLine.substring(2, equalsIndex);
              const value = configLine.substring(equalsIndex + 1);
              app.commandLine.appendSwitch(name, value);
            } else {
              const name = configLine.substring(2);
              app.commandLine.appendSwitch(name);
            }
          }
        }
        return;
      }
    }
  }

  private async initializeRenderingEngine() {
    const options = ElectronDesktopOptions();

    if (!options.useGpuDriverBugWorkarounds()) {
      app.commandLine.appendSwitch('disable-gpu-driver-bug-workarounds');
    }

    if (!options.useGpuExclusionList()) {
      app.commandLine.appendSwitch('ignore-gpu-blocklist');
    }

    // read rendering engine, if any
    const engine = ElectronDesktopOptions().renderingEngine().toLowerCase();

    // for whatever reason, setting '--use-gl=desktop' doesn't seem to enable
    // the GPU on macOS; testing on other platforms would be worthwhile but
    // Chromium will enable GPU acceleration by default where possible so it
    // seems okay to ignore here
    if (engine.length === 0 || engine === 'desktop' || engine == 'auto') {
      return;
    }

    // handle gles (primarily for linux)
    if (engine === 'gles') {
      app.commandLine.appendSwitch('use-gl', 'gles');
      return;
    }

    // handle software rendering
    if (engine === 'software') {
      app.commandLine.appendSwitch('disable-gpu');
      return;
    }
  }

  private async initializeAccessibility() {
    // there have been cases, historically, where Chromium accessibility
    // would enable itself and introduce performance issues even though the
    // user was not using an accessibility aid such as a screen reader, e.g.:
    // https://github.com/rstudio/rstudio/issues/1990)
    if (ElectronDesktopOptions().disableRendererAccessibility()) {
      app.commandLine.appendSwitch('disable-renderer-accessibility');
    }
  }

  private async startup(): Promise<void> {
    await this.initializeRenderingEngine();
    await this.initializeAccessibility();

    const rstudio = new Application();
    rstudio.argsManager.handleLogLevel();
    setApplication(rstudio);

    this.initializeAppConfig();

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
