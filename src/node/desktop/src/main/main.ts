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
import { getenv } from '../core/environment';
import { setApplication } from './app-state';
import { Application } from './application';
import { kHelp, kVersion, kVersionJson } from './args-manager';
import { startRDetection } from './detect-r';
import { initI18n } from './i18n-manager';
import { startLoginShellPathQuery } from './login-shell-path';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { parseStatus } from './program-status';
import { recordProcessStart, startupCheckpoint } from './startup-timing';
import { createStandaloneErrorDialog } from './utils';
import { Xdg } from '../core/xdg';
import { ElectronFlagsConfig, kOzonePlatformSwitch, loadElectronFlags, planOzoneRelaunch } from './electron-flags';

interface ElectronFlagsLoad {
  config?: ElectronFlagsConfig;
  error?: Error;
}

interface OzoneRelaunchResult {
  relaunched: boolean;
  message?: string;
}

/**
 * RStudio entrypoint
 */
class RStudioMain {
  constructor(
    private readonly electronFlags: ElectronFlagsLoad,
    private readonly ozoneMessage: string | undefined,
  ) {}

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

  private initializeAppConfig(): void {
    const { config, error } = this.electronFlags;
    if (error) {
      throw error;
    }
    if (!config) {
      return;
    }

    logger().logDebug(`Using Electron flags from file ${config.path}`);
    for (const flag of config.flags) {
      // applied by relaunchForOzonePlatform(); appending it now would reach only
      // the child processes, which must use the same backend as this one
      if (flag.name === kOzonePlatformSwitch) {
        continue;
      }
      const configLine = `--${flag.name}${flag.value === undefined ? '' : `=${flag.value}`}`;
      logger().logDebug(`Appending switch: ${configLine}`);
      if (flag.value === undefined) {
        app.commandLine.appendSwitch(flag.name);
      } else {
        app.commandLine.appendSwitch(flag.name, flag.value);
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

  private initializeInputFeatures() {
    // Middle-click autoscroll (the "4-way" pan puck) is a Blink feature that
    // Chrome and Firefox enable by default, but Electron does not -- so a
    // middle-click in a scrollable area such as the data viewer does nothing
    // in RStudio Desktop. Opt in explicitly. `enable-blink-features` is a
    // comma-separated list; nothing else sets it, so one switch suffices.
    app.commandLine.appendSwitch('enable-blink-features', 'MiddleClickAutoscroll');
  }

  private async startup(): Promise<void> {
    await this.initializeRenderingEngine();
    await this.initializeAccessibility();
    this.initializeInputFeatures();

    const rstudio = new Application();
    rstudio.argsManager.handleLogLevel();
    setApplication(rstudio);

    if (this.ozoneMessage) {
      logger().logWarning(this.ozoneMessage);
    }
    this.initializeAppConfig();

    if (!parseStatus(await rstudio.beforeAppReady())) {
      return;
    }
    startupCheckpoint('before-app-ready-done');

    await app.whenReady();
    startupCheckpoint('app-ready');

    if (!parseStatus(await rstudio.run())) {
      return;
    }
  }
}

function loadAppConfig(): ElectronFlagsLoad {
  try {
    const configDirs = [Xdg.userConfigDir().getAbsolutePath(), app.getPath('appData')];
    return { config: loadElectronFlags(configDirs) };
  } catch (error: unknown) {
    return { error: safeError(error) };
  }
}

/**
 * Chromium picks its Ozone backend before this script runs, so a requested
 * --ozone-platform takes effect only by relaunching with it on the command
 * line. This runs before anything else starts, as the process may exit here.
 */
function relaunchForOzonePlatform(config: ElectronFlagsConfig | undefined): OzoneRelaunchResult {
  // the early-exit flags print to the caller's terminal, which a relaunch detaches from
  const earlyExitArgs = [kHelp, kVersion, kVersionJson];
  if (process.platform !== 'linux' || process.argv.some((arg) => earlyExitArgs.includes(arg))) {
    return { relaunched: false };
  }

  // Electron records the backend it picked (from XDG_SESSION_TYPE when the
  // command line names none) as --ozone-platform, so a requested value that
  // matches it, such as x11 in an X11 session, needs no relaunch
  const plan = planOzoneRelaunch({
    argv: process.argv,
    currentPlatform: app.commandLine.hasSwitch(kOzonePlatformSwitch)
      ? app.commandLine.getSwitchValue(kOzonePlatformSwitch)
      : undefined,
    chromiumArguments: getenv('RSTUDIO_CHROMIUM_ARGUMENTS'),
    config,
    isPackaged: app.isPackaged,
  });
  if (!plan.relaunchArgs) {
    return { relaunched: false, message: plan.message };
  }

  console.log(plan.message); // logging is not set up yet, and this process is exiting
  app.relaunch({ args: plan.relaunchArgs });
  app.exit(0);
  return { relaunched: true };
}

// Startup
const electronFlags = loadAppConfig();
const ozoneRelaunch = relaunchForOzonePlatform(electronFlags.config);
if (!ozoneRelaunch.relaunched) {
  recordProcessStart();
  startupCheckpoint('main-entry');

  // the login shell is slow to answer and the session will need its PATH, so
  // ask before anything else (see login-shell-path.ts); likewise start asking
  // R about itself, which takes about as long as Electron's own startup
  startLoginShellPathQuery();
  startRDetection();

  initI18n();

  const main = new RStudioMain(electronFlags, ozoneRelaunch.message);
  void main.main();
}
