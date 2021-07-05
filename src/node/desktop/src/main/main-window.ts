/*
 * main-window.ts
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

import { BrowserWindow, session } from 'electron';
import path from 'path';
import { ChildProcess } from 'child_process';

import { logger } from '../core/logger';

import { GwtCallback, PendingQuit } from './gwt-callback';
import { MenuCallback, showPlaceholderMenu } from './menu-callback';
import { PendingWindow } from './pending-window';
import { RCommandEvaluator } from './r-command-evaluator';
import { SessionLauncher } from './session-launcher';
import { ApplicationLaunch } from './application-launch';
import { GwtWindow } from './gwt-window';
import { appState } from './app-state';

export class MainWindow extends GwtWindow {
  sessionLauncher?: SessionLauncher;
  sessionProcess?: ChildProcess;
  appLauncher?: ApplicationLaunch;
  menuCallback: MenuCallback;
  quitConfirmed = false;
  workbenchInitialized = false;
  pendingWindows = new Array<PendingWindow>();

  // TODO
  //#ifdef _WIN32
  // HWINEVENTHOOK eventHook_ = nullptr;
  //#endif

  constructor(url: string, public isRemoteDesktop: boolean) {
    super(false, false, '', url, undefined, undefined, isRemoteDesktop, ['desktop', 'desktopMenuCallback']);

    appState().gwtCallback = new GwtCallback(this, isRemoteDesktop);
    this.menuCallback = new MenuCallback(this);

    RCommandEvaluator.setMainWindow(this);

    if (this.isRemoteDesktop) {
      // TODO - determine if we need to replicate this
      // since the object registration is asynchronous, during the GWT setup code
      // there is a race condition where the initialization can happen before the
      // remoteDesktop object is registered, making the GWT application think that
      // it should use regular desktop objects - to circumvent this, we use a custom
      // user agent string that the GWT code can detect with 100% success rate to
      // get around this race condition
      // QString userAgent = webPage()->profile()->httpUserAgent().append(QStringLiteral("; RStudio Remote Desktop"));
      // webPage()->profile()->setHttpUserAgent(userAgent);
      // channel->registerObject(QStringLiteral("remoteDesktop"), &gwtCallback_);
    }

    showPlaceholderMenu();
  }

  loadUrl(url: string): void {
    // pass along the shared secret with every request
    const filter = {
      urls: [`${url}/*`]
    };
    session.defaultSession.webRequest.onBeforeSendHeaders(filter, (details, callback) => {
      details.requestHeaders['X-Shared-Secret'] = process.env.RS_SHARED_SECRET ?? '';
      callback({ requestHeaders: details.requestHeaders});
    });

    this.window.webContents.on('new-window',
      (event, url /*, frameName, disposition, options, additionalFeatures, referrer, postBody*/) => {

        event.preventDefault();

        // check if we have a satellite window waiting to come up
        const pendingWindow = this.pendingWindows.pop();
        if (pendingWindow) {
          const newWindow = this.createWindow(pendingWindow.width, pendingWindow.height);
          newWindow.loadURL(url);
          newWindow.webContents.openDevTools();
        }
      });

    this.window.loadURL(url);
  }

  quit(): void {
    RCommandEvaluator.setMainWindow(null);
    this.quitConfirmed = true;
    this.window.close();
  }

  invokeCommand(cmdId: string): void {
    this.executeJavaScript(`window.desktopHooks.invokeCommand("${cmdId}")`)
      .catch((error) => {
        logger().logError(error);
      });
  }

  onWorkbenchInitialized(): void {
    this.workbenchInitialized = true;
    this.executeJavaScript('window.desktopHooks.getActiveProjectDir()')
      .then(projectDir => {
        if (projectDir.length > 0) {
          this.window.setTitle(`${projectDir} - RStudio`);
        } else {
          this.window.setTitle('RStudio');
        }
        this.avoidMoveCursorIfNecessary();
      })
      .catch((error) => {
        logger().logError(error);
      });
  }

  collectPendingQuitRequest(): number {
    return appState().gwtCallback?.collectPendingQuitRequest() ?? PendingQuit.PendingQuitNone;
  }

  createWindow(width: number, height: number): BrowserWindow {
    return new BrowserWindow({
      width: width,
      height: height,
      // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      backgroundColor: '#fff', 
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true,
        additionalArguments: ['desktop|desktopInfo'],
        preload: path.join(__dirname, '../renderer/preload.js'),
      },
    });
  }

  prepareForWindow(pendingWindow: PendingWindow): void {
    this.pendingWindows.push(pendingWindow);
  }

  onActivated(): void {
    // intentionally left blank
  }
}