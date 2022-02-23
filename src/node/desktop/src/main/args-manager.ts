import { app } from 'electron';

import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';

import { enableDiagnosticsOutput } from '../core/logger';
import { appState } from './app-state';
import { exitSuccess, ProgramStatus, run } from './program-status';
import { getComponentVersions } from './utils';

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

// If some args should early exit the application, add them to `webpack.plugins.js`
export class ArgsManager {
  argsList = [
    {
      name: kVersionJson,
      describe: 'Report an extended version info',
    },
    {
      name: kRunDiagnosticsOption,
      describe: 'Run diagnostics and save in a file',
    },
    {
      name: kLogLevel,
      describe: 'Sets the verbosity of the logging',
    },
    {
      name: kDelaySession,
      describe: 'Causes the rsession to pause so the user can see the "Loading R" screen longer. [unimplemented]',
    },
    {
      name: kSessionExit,
      describe: 'Causes the rsession to terminate immediately so the user can see the error page. [unimplemented]',
    },
  ];

  handleHelp(argv: string[]) {
    const yargsHelper = yargs(hideBin(argv)); //.argv;

    this.argsList.forEach((arg) => {
      yargsHelper.option(arg.name.replace('--', ''), { describe: arg.describe });
    });

    yargsHelper.help().parseSync();
  }

  initCommandLine(argv: string[] = process.argv): ProgramStatus {
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
      appState().runDiagnostics = true;
      enableDiagnosticsOutput();
    }

    return run();
  }
}
