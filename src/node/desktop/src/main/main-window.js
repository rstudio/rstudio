/*
 * main-window.js
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

const { BrowserWindow, session } = require('electron');
const path = require('path');
const { DesktopCallback } = require('./desktop-callback');
const MenuCallback = require('./menu-callback');
const RCommandEvaluator = require('./r-command-evaluator');

// corresponds to DesktopMainWindow.cpp/hpp
module.exports = class MainWindow {
  constructor(url, isRemoteDesktop) {
    this.url = url;
    this.isRemoteDesktop = isRemoteDesktop;
    this.sessionLauncher_ = null;
    this.sessionProcess_ = null;
    this.window = null;
    this.desktopCallback = new DesktopCallback(this, this, isRemoteDesktop);
    this.menuCallback = new MenuCallback(this);
    this.quitConfirmed = false;
    this.workbenchInitialized_ = false;
    this.pendingWindows = [];

    RCommandEvaluator.setMainWindow(this);
  }

  set sessionLauncher(value) {
    this.sessionLauncher_ = value;
  }

  get sessionLauncher() {
    return this.sessionLauncher_;
  }

  set sessionProcess(value) {
    this.sessionProcess_ = value;
  }

  get sessionProcess() {
    return this.sessionProcess_;
  }

  load(url) {
    // show the window
    this.window = this.createWindow(1400, 1024);
 
    // pass along the shared secret with every request
    const filter = {
      urls: [`${url}/*`]
    };
    session.defaultSession.webRequest.onBeforeSendHeaders(filter, (details, callback) => {
      details.requestHeaders['X-Shared-Secret'] = process.env.RS_SHARED_SECRET;
      callback({ requestHeaders: details.requestHeaders});
    });

    this.window.webContents.on('new-window',
      (event, url, frameName, disposition, options, additionalFeatures, referrer, postBody) => {

      event.preventDefault();

      // check if we have a satellite window waiting to come up
      if (this.pendingWindows.length) {
        const pendingWindow = this.pendingWindows.pop();
        const newWindow = this.createWindow(pendingWindow.width, pendingWindow.height);
        newWindow.loadURL(url);
        newWindow.webContents.openDevTools();
      }
    });

    this.window.loadURL(url);
    // this.window.webContents.openDevTools();
  }

  quit() {
    RCommandEvaluator.setMainWindow(null);
    this.quitConfirmed = true;
    this.window.close();
  }

  invokeCommand(cmdId) {
    this.window.webContents.executeJavaScript(`window.desktopHooks.invokeCommand("${cmdId}")`)
      .catch(() => {
        console.error(`Error: failed to execute desktopHooks.invokeCommand("${cmdId}")`);
      });
  }

  onWorkbenchInitialized() {
    this.workbenchInitialized_ = true;
    this.window.webContents.executeJavaScript('window.desktopHooks.getActiveProjectDir()')
      .then(projectDir => {
        if (projectDir.length > 0) {
          this.window.setTitle(`${projectDir} - RStudio`);
        } else {
          this.window.setTitle('RStudio');
        }
      })
      .catch(() => {
      });
  }

  collectPendingQuitRequest() {
    return this.desktopCallback.collectPendingQuitRequest();
  }

  get workbenchInitialized() {
    return this.workbenchInitialized_;
 
  }

  createWindow(width, height) {
    return new BrowserWindow({
      width: width,
      height: height,
      // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      backgroundColor: '#fff', 
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true,
        preload: path.join(__dirname, '../renderer/preload.js'),
      },
    });
  }

  prepareForWindow(pendingWindow) {
    this.pendingWindows.push(pendingWindow);
  }
};
