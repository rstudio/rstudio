/*
 * session-launcher.js
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

const child_process = require('child_process');
const fs = require('fs');
const path = require('path');
const MainWindow = require('./main-window');

module.exports = class SessionLauncher {
  constructor(sessionPath, confPath) {
      this.sessionPath_ = sessionPath;
      this.confPath_ = confPath;
      this.port_ = '';
      this.host = '';
      this.mainWindow = null;
      this.sessionProcess = null;
  }

  static s_launcherToken = '';

  launchSession(argList) {
    // #ifdef Q_OS_DARWIN
    // on macOS with the hardened runtime, we can no longer rely on dyld
    // to lazy-load symbols from libR.dylib; to resolve this, we use
    // DYLD_INSERT_LIBRARIES to inject the library we wish to use on
    // launch 
    let rHome = process.env.R_HOME;
    let rLib = path.join(rHome, 'lib/libR.dylib');
    if (fs.existsSync(rLib))
    {
        process.env.DYLD_INSERT_LIBRARIES = path.resolve(rLib);
    }
    // #endif // Q_OS_DARWIN

    const sessionProc = child_process.spawn(this.sessionPath_, argList);
    sessionProc.stdout.on('data', (data) => {
      console.log(`stdout: ${data}`);
    });
    sessionProc.stderr.on('data', (data) => {
      console.log(`stderr: ${data}`);
    });
    sessionProc.on('close', (code) => {
      console.log(`child process exited with code ${code}`);
    });

    return sessionProc;
  }

  launchFirstSession(installPath, devMode) {
    let launchContext = this.buildLaunchContext();

    // show help home on first run
    launchContext.argList.push('--show-help-home', '1');

    // launch the process
    this.sessionProcess = this.launchSession(launchContext.argList);

    this.mainWindow = new MainWindow(launchContext.url, false);
    this.mainWindow.sessionLauncher = this;
    this.mainWindow.sessionProcess = this.sessionProcess;

    // show the window
    this.mainWindow.load(launchContext.url);
  }

  static get launcherToken() {
    if (SessionLauncher.s_launcherToken.length == 0) {
       SessionLauncher.s_launcherToken = '7F83A8BD';
    }
    return SessionLauncher.s_launcherToken;
  }

  get port() {
    if (this.port_.length == 0) {
      this.port_ = '40810'; // see DesktopOptions.cpp portNumber() for how to randomly generate
    }
    return this.port_;
  }

  set port(value) {
    this.port_ = value;
  }

  buildLaunchContext() {
    this.host = '127.0.0.1';
    return {
        host: this.host,
        port: `${this.port}`,
        url: `http://${this.host}:${this.port}`,
        argList: [
            '--config-file', this.confPath_,
            '--program-mode', 'desktop',
            '--www-port', this.port,
            '--launcher-token', SessionLauncher.launcherToken,
        ],
    };
  }
};
