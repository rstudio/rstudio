/*
 * args-manager.ts
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
import { WinstonLogger } from '../core/winston-logger';

import { enableDiagnosticsOutput, parseCommandLineLogLevel, setLogger } from '../core/logger';
import { Application } from './application';
import { exitSuccess, ProgramStatus, run } from './program-status';
import { getComponentVersions, userLogPath } from './utils';

// RStudio command-line switches
export const kRunDiagnosticsOption = '--run-diagnostics';
export const kVersion = '--version';
export const kVersionJson = '--version-json';
export const kLogLevel = 'log-level';
export const kDelaySession = 'session-delay';
export const kSessionExit = 'session-exit';
export const kHelp = '--help';

// RStudio Pro Only
// export const kSessionServerOption = '--session-server';
// export const kSessionServerUrlOption = '--session-url';
// export const kTempCookiesOption = '--use-temp-cookies';

// !IMPORTANT: If some args should early exit the application, add them to `webpack.plugins.js`
export class ArgsManager {

  initCommandLine(application: Application, argv: string[] = process.argv): ProgramStatus {

    // display usage help
    if (argv.indexOf(kHelp) > -1) {
      console.log('Options:');
      console.log('  --version          Display RStudio version');
      console.log('  --version-json     Display versions of major components in JSON format');
      console.log('  --run-diagnostics  Run diagnostics and save in a file');
      console.log('  --log-level        Sets the verbosity of the logging');
      console.log('                     --log-level=ERR|WARN|INFO|DEBUG');
      console.log('  --session-delay    Pause the rsession so the "Loading R" screen displays longer');
      console.log('  --session-exit     Terminate the rsession immediately forcing error page to display');
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

    return run();
  }

  handleAppReadyCommands(application: Application) {
    // switch for setting a session start delay in seconds (used for testing, troubleshooting)
    if (app.commandLine.hasSwitch(kDelaySession)) {
      application.sessionStartDelaySeconds = 5;
    }

    // switch for forcing rsession to exit immediately with non-zero exit code
    // (will happen after session start delay above, if also specified)
    if (app.commandLine.hasSwitch(kSessionExit)) {
      application.sessionEarlyExitCode = 1;
    }
  }

  handleLogLevel() {
    const logLevelFromArgs = app.commandLine.getSwitchValue(kLogLevel);

    const logLevel = parseCommandLineLogLevel(logLevelFromArgs, 'warn');

    setLogger(new WinstonLogger(userLogPath().completeChildPath('rdesktop.log'), logLevel));
  }
}
