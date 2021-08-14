/*
 * window-utils.ts
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

import { WebContents } from 'electron';
import { logger } from '../core/logger';
import { appState } from './app-state';
import { PendingSatelliteWindow, PendingSecondaryWindow } from './pending-window';
import { SatelliteWindow } from './satellite-window';
import { SecondaryWindow } from './secondary-window';
import { getDpiZoomScaling } from './utils';

export function createSatelliteWindow(
  webContents: WebContents,
  pendingSatellite: PendingSatelliteWindow,
  details: Electron.HandlerDetails): void {

  // get width and height, and adjust for high DPI
  const dpiZoomScaling = getDpiZoomScaling();
  const width = pendingSatellite.width * dpiZoomScaling;
  const height = pendingSatellite.height * dpiZoomScaling;
  const x = pendingSatellite.screenX;
  const y = pendingSatellite.screenY;

  // create and size
  const window = new SatelliteWindow(pendingSatellite.mainWindow, pendingSatellite.name, webContents);
  window.window.setSize(width, height);

  if (x >= 0 && y >= 0) {
    // if the window specified its location, use it
    window.window.setPosition(x, y);
  } else if (pendingSatellite.name !== 'pdf') {

    // window location was left for us to determine; try to tile the window
    // (but leave pdf window alone since it is so large)

    // calculate location to move to

    // y always attempts to be 25 pixels above then faults back
    // to 25 pixels below if that would be offscreen
    const OVERLAP = 25;
    const [currentX, currentY] = pendingSatellite.mainWindow.window.getPosition();
    const [currentWidth] = pendingSatellite.mainWindow.window.getSize();
    let moveY = currentY - OVERLAP;
    if (moveY < 0) {
      moveY = currentY + OVERLAP;
    }

    // x is based on centering over main window
    const moveX = currentX + (currentWidth / 2) - (width / 2);

    // perform move
    window.window.setPosition(Math.trunc(moveX), Math.trunc(moveY));
  }

  // if we have a name set, start tracking this window
  if (pendingSatellite.name) {
    appState().windowTracker.addWindow(pendingSatellite.name, window);
  }

  window.window.loadURL(details.url)
    .then(() => {
      window.window.show();
    })
    .catch((reason) => {
      logger().logErrorMessage(reason);
    });
}

export function createSecondaryWindow(
  webContents: WebContents,
  pendingSecondary: PendingSecondaryWindow,
  details: Electron.HandlerDetails,
  baseUrl?: string): void {

  const window = new SecondaryWindow(
    pendingSecondary.showToolbar,
    pendingSecondary.name,
    baseUrl,
    undefined,
    webContents,
    pendingSecondary.allowExternalNavigate);

  // TODO
  // allow for Ctrl + W to close window (NOTE: Ctrl means Meta on macOS)
  // QAction* action = new QAction(pWindow);
  // action->setShortcut(Qt::CTRL + Qt::Key_W);
  // pWindow->addAction(action);
  // QObject::connect(action, &QAction::triggered,
  //              pWindow, &BrowserWindow::close);
  // if we have a name set, start tracking this window

  if (pendingSecondary.name) {
    appState().windowTracker.addWindow(pendingSecondary.name, window);
  }

  window.window.loadURL(details.url)
    .then(() => {
      window.window.show();
    })
    .catch((reason) => {
      logger().logErrorMessage(reason);
    });
}
