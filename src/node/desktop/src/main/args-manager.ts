import { app } from 'electron';
import { WinstonLogger } from '../core/winston-logger';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';

import { enableDiagnosticsOutput, parseCommandLineLogLevel, setLogger } from '../core/logger';
import { Application } from './application';
import { exitSuccess, ProgramStatus, run } from './program-status';
import { getComponentVersions, userLogPath } from './utils';

// How to use:
// [MAC] RStudio.app/Contents/MacOS/RStudio --session-exit
// [MAC] RStudio.app/Contents/MacOS/RStudio --help
// [Windows] C:\Program Files\RStudio\rstudio.exe --help
// [Linux] C:\Program Files\RStudio\rstudio.exe --help
// npm run start -- -- --session-exit
// npm run start -- -- --log-level=err
// npm run start -- -- --help
// npm run start -- -- --version
// RStudio command-line switches
export const kRunDiagnosticsOption = '--run-diagnostics';
export const kVersion = '--version';
export const kVersionJson = '--version-json';
export const kLogLevel = 'log-level';
export const kDelaySession = 'session-delay';
export const kSessionExit = 'session-exit';

// RStudio Pro Only
// export const kSessionServerOption = '--session-server';
// export const kSessionServerUrlOption = '--session-url';
// export const kTempCookiesOption = '--use-temp-cookies';

// !IMPORTANT: If some args should early exit the application, add them to `webpack.plugins.js`
export class ArgsManager {
  argsList = [
    {
      name: kVersionJson,
      describe: 'Display versions of major components in JSON format',
    },
    {
      name: kRunDiagnosticsOption,
      describe: 'Run diagnostics and save in a file',
    },
    {
      name: kLogLevel,
      describe: 'Sets the verbosity of the logging --log-level=ERR|WARN|INFO|DEBUG',
    },
    {
      name: kDelaySession,
      describe: 'Causes the rsession to pause so the user can see the "Loading R" screen longer',
    },
    {
      name: kSessionExit,
      describe: 'Causes the rsession to terminate immediately so the user can see the error page',
    },
  ];

  handleHelp(argv: string[]) {
    const yargsHelper = yargs(hideBin(argv)); //.argv;

    this.argsList.forEach((arg) => {
      yargsHelper.option(arg.name.replace('--', ''), { describe: arg.describe });
    });

    yargsHelper.help().parseSync();
  }

  initCommandLine(application: Application, argv: string[] = process.argv): ProgramStatus {
    this.handleHelp(argv);

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
