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

import { app } from 'electron';
import fs from 'fs';
import path from 'path';
import SessionLauncher from './session-launcher';

export default class Main {
  constructor() {
  }

  run() {
    // look for a version check request; if we have one, just do that and exit
    if (process.argv.includes('--version', 1)) {
      console.log('RSTUDIO_VERSION is 0.0.0.0.00001');
      app.exit(0);
      return;
    }

    // ignore SIGPIPE
    process.on('SIGPIPE', () => {});

    this.initializeSharedSecret();

    // get install path
    let installPath = '../../../build/src/cpp';
    if (!fs.existsSync(installPath)) {
      console.log(`Unable to find ${installPath}`);
      app.exit(1);
      return;
    }
    installPath = path.resolve(installPath);

    // calculate paths to config file, rsession, and desktop scripts
    let confPath = path.join(installPath, 'conf/rdesktop-dev.conf');
    let sessionExe = process.platform === 'win32' ? 'rsession.exe' : 'rsession';
    let sessionPath = path.join(installPath, 'session', sessionExe);
    let scriptsPath = path.join(installPath, 'desktop');
    let devMode = true;

    if (!this.prepareEnvironment(scriptsPath)) {
      console.log('Failed to prepare environment');
      app.exit(1);
      return;
    }

    let launcher = new SessionLauncher(sessionPath, confPath);
    launcher.launchFirstSession(installPath, devMode);
  }

  initializeSharedSecret() {
    let secret = '12345';
    process.env.RS_SHARED_SECRET = secret;

  }

  prepareEnvironment(scriptsPath: string) {
    // attempt to detect R environment
    // let ldScriptPath = path.join(scriptsPath, '../session/r-ldpath');

    // whole bunch of code..., hardcoded for prototype
    if (process.platform === 'darwin') {
      let rHome = '/usr/local/Cellar/r/4.0.5/lib/R';
      process.env.R_HOME = rHome;
      process.env.R_SHARE_DIR = `${rHome}/share`;
      process.env.R_INCLUDE_DIR = `${rHome}/include`;
      process.env.R_DOC_DIR = `${rHome}/doc`;
      process.env.DYLD_FALLBACK_LIBRARY_PATH = `${rHome}/lib:/Users/gary/lib:/usr/local/lib:/usr/lib:::/lib:/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/lib/server`;
      process.env.RS_CRASH_HANDLER_PATH = '/opt/rstudio-tools/crashpad/crashpad/out/Default/crashpad_handler';
    } else if (process.platform === 'linux') {
      process.env.R_HOME = '/opt/R/3.6.3/lib/R';
      process.env.R_SHARE_DIR = '/opt/R/3.6.3/lib/R/share';
      process.env.R_INCLUDE_DIR = '/opt/R/3.6.3/lib/R/include';
      process.env.R_DOC_DIR = '/opt/R/3.6.3/lib/R/doc';
    } else if (process.platform === 'win32') {
      process.env.R_HOME = 'C:\\R\\R-35~1.0';
      process.env.PATH = `C:\\R\\R-3.5.0\\bin\\x64;${process.env.PATH}`; 
    } else {
      console.log(`Unsupported platform ${process.platform}`);
      return false;
    }

    // uncomment to stall start of rsession for # seconds so you can attach debugger to it
    // process.env.RSTUDIO_SESSION_SLEEP_ON_STARTUP = "15";

    return true;
  }
};
