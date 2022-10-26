/*
 * args-manager.ts
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

import { app, webContents } from 'electron';
import path from 'path';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { enableDiagnosticsOutput, logger, setLogger } from '../core/logger';
import { kRStudioInitialProject, kRStudioInitialWorkingDir } from '../core/r-user-data';
import { WinstonLogger } from '../core/winston-logger';
import { getDesktopBridge } from '../renderer/desktop-bridge';
import { Application } from './application';
import LogOptions from './log-options';
import { exitSuccess, ProgramStatus, run } from './program-status';
import { getComponentVersions } from './utils';
import { activateWindow } from './window-utils';

// RStudio command-line switches
export const kRunDiagnosticsOption = '--run-diagnostics';
export const kVersion = '--version';
export const kVersionJson = '--version-json';
export const kLogLevel = 'log-level';
export const kLoggerType = 'logger-type';
export const kLogMessageFormat = 'log-message-format';
export const kLogDir = 'log-dir';
export const kNoArg = '';
export const kDelaySession = 'session-delay';
export const kSessionExit = 'session-exit';
export const kHelp = '--help';
export const kStartupDelay = 'startup-delay';

// RStudio Pro Only
// export const kSessionServerOption = '--session-server';
// export const kSessionServerUrlOption = '--session-url';
// export const kTempCookiesOption = '--use-temp-cookies';

const defaultStartupDelaySeconds = 5;

export interface Option {
  arg: string;
  env: string;
}

export interface OptionList {
  logLevel: Option;
  loggerType: Option;
  logMessageFormat: Option;
  logDir: Option;
  logConfFile: Option;
}

// this maps the option key to the environment variable
// use kNoArg if it is not configurable from the conf file
export const options: OptionList = {
  logLevel: { arg: kLogLevel, env: 'RS_LOG_LEVEL' },
  loggerType: { arg: kLoggerType, env: 'RS_LOGGER_TYPE' },
  logMessageFormat: { arg: kLogMessageFormat, env: 'RS_LOG_MESSAGE_FORMAT' },
  logDir: { arg: kLogDir, env: 'RS_LOG_DIR' },
  logConfFile: { arg: kNoArg, env: 'RS_LOG_CONF_FILE' },
};

// !IMPORTANT: If some args should early exit the application, add them to `webpack.plugins.js`
export class ArgsManager {
  unswitchedArgs: string[] = [];

  initCommandLine(application: Application, argv: string[] = process.argv): ProgramStatus {
    // display usage help
    if (argv.indexOf(kHelp) > -1) {
      console.log('Options:');
      console.log('  --version          Display RStudio version');
      console.log('  --version-json     Display versions of major components in JSON format');
      console.log('  --run-diagnostics  Run diagnostics and save in a file');
      console.log('  --log-level        Sets the verbosity of the logging');
      console.log('                     --log-level=ERR|WARN|INFO|DEBUG');
      // eslint-disable-next-line max-len
      console.log('  --log-dir          The log directory to store log files in. The resulting log file name is based on the executable name.');
      console.log('  --session-delay    Pause the rsession so the "Loading R" screen displays longer');
      console.log('  --session-exit     Terminate the rsession immediately forcing error page to display');
      console.log('  --startup-delay    Pause before showing the application so the splash screen displays longer');
      console.log(`                     --startup-delay=<number of seconds> ${defaultStartupDelaySeconds} by default`);
      console.log('  --help             Show this help');
      return exitSuccess();
    }

    // look for a version check request; if we have one, just do that and exit
    if (argv.indexOf(kVersion) > -1) {
      console.log(app.getVersion());
      return exitSuccess();
    }

    // report extended version info and exit
    if (argv.indexOf(kVersionJson) > -1) {
      console.log(getComponentVersions());
      return exitSuccess();
    }

    if (argv.indexOf(kRunDiagnosticsOption) > -1) {
      application.runDiagnostics = true;
      enableDiagnosticsOutput();
    }

    this.setUnswitchedArgs(process.argv);

    return run();
  }

  setUnswitchedArgs(args: string[]) {
    // filter out the process path and . (occurs in dev mode)
    this.unswitchedArgs = args.filter(
      (value) => !value.startsWith('--') && value !== process.execPath && value !== '.'
    );
  }

  getProjectFileArg(): string | undefined {
    const projectFile = this.unswitchedArgs.find((arg) => {
      return FilePath.existsSync(arg) && path.extname(arg).toLowerCase() === '.rproj';
    });
    return projectFile;
  }

  getFileArgs(): string[] {
    const files = this.unswitchedArgs.filter((arg) => {
      return FilePath.existsSync(arg) && path.extname(arg).toLowerCase() !== '.rproj';
    });
    return files;
  }

  handleAfterSessionLaunchCommands() {
    if (this.unswitchedArgs.length) {
      this.unswitchedArgs.forEach((arg) => {
        if (FilePath.existsSync(arg)) {
          app
            .whenReady()
            .then(() => {
              getDesktopBridge().openFile(arg);
              const name = webContents.getAllWebContents()[0].mainFrame.name;
              activateWindow(name);
            })
            .catch((error: unknown) => {
              logger().logError(error);
            });
        }
      });
    }
  }

  handleAppReadyCommands(application: Application) {
    // switch for setting a session start delay in seconds (used for testing, troubleshooting)
    if (app.commandLine.hasSwitch(kDelaySession)) {
      application.sessionStartDelaySeconds = 5;
    }

    // switch for forcing the splash to show by adding a startup delay
    if (app.commandLine.hasSwitch(kStartupDelay)) {
      let startupDelayValue = parseInt(app.commandLine.getSwitchValue(kStartupDelay));

      if (startupDelayValue < 0 || isNaN(startupDelayValue)) {
        startupDelayValue = defaultStartupDelaySeconds;
      }

      application.startupDelayMs = startupDelayValue * 1000;
    }

    // switch for forcing rsession to exit immediately with non-zero exit code
    // (will happen after session start delay above, if also specified)
    if (app.commandLine.hasSwitch(kSessionExit)) {
      application.sessionEarlyExitCode = 1;
    }

    if (this.unswitchedArgs.length) {
      this.unswitchedArgs = this.unswitchedArgs.filter((arg) => {
        if (FilePath.existsSync(arg)) {
          const ext = path.extname(arg).toLowerCase();

          if (ext === '.rproj') {
            setenv(kRStudioInitialProject, arg);
            return false;
          } else {
            const workingDir = path.dirname(arg);
            setenv(kRStudioInitialWorkingDir, workingDir);
          }
        }
        logger().logDebug(`RS_INITIAL_WD: ${getenv(kRStudioInitialWorkingDir)}`);
        return true;
      });
    }
  }

  handleLogLevel() {
    const logOptions = new LogOptions();

    setLogger(new WinstonLogger(logOptions));
  }
}
