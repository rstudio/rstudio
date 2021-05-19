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

import { execSync } from 'child_process';
import { app } from 'electron';
import fs from 'fs';
import { existsSync } from 'fs';
import { platform } from 'os';
import path from 'path';
import scanForR from './r';
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

  prepareEnvironmentLdPaths(rHome: string) {

    // nothing to do on Windows
    if (platform() === "win32") {
      return false;
    }

    // get name of ld path variable
    // on macOS, we need to set DYLD_FALLBACK_LIBRARY_PATH
    // on Linux, we need to set LD_LIBRARY_PATH
    let ldPathVar = (platform() === "darwin")
      ? "DYLD_FALLBACK_LIBRARY_PATH"
      : "LD_LIBRARY_PATH";
    
    // get path to ldpaths script
    let ldPathsScript = `${rHome}/etc/ldpaths`;
    if (!existsSync(ldPathsScript)) {
      return false;
    }
     
    // source it and read it
    let command = `source "${ldPathsScript}" && echo "\${${ldPathVar}}"`;
    let ldPath = execSync(command).toString().trim();
    process.env[ldPathVar] = ldPath;
    
  }

  prepareEnvironment(scriptsPath: string) {

    // locate R, and then set up environment variables
    let R = scanForR();
    if (R.length === 0) {
      console.log("Could not locate R (try placing the R binary on the PATH)");
      return false;
    }

    // read some important environment variables
    let rHome = execSync(`${R} --no-save --no-restore RHOME`).toString().trim();
    process.env.R_HOME = `${rHome}`;
    process.env.R_SHARE_DIR = `${rHome}/share`;
    process.env.R_DOC_DIR = `${rHome}/doc`;

    // read and execute ldpaths script from R
    this.prepareEnvironmentLdPaths(rHome);

    return true;

  }

};
