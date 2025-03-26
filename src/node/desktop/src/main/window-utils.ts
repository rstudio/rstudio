/*
 * window-utils.ts
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

import { screen, BrowserWindow, webContents, WebContents, Rectangle } from 'electron';
import { appState } from './app-state';
import { PendingSatelliteWindow, PendingSecondaryWindow } from './pending-window';
import { SatelliteWindow } from './satellite-window';
import { SecondaryWindow } from './secondary-window';
import { getDpiZoomScaling, raiseAndActivateWindow } from './utils';
import { logger } from '../core/logger';
import { safeError } from '../core/err';

export function configureSatelliteWindow(
  pendingSatellite: PendingSatelliteWindow,
  newWindow: BrowserWindow,
  owner: WebContents,
): void {
  // get width and height, and adjust for high DPI
  const dpiZoomScaling = getDpiZoomScaling();
  const width = pendingSatellite.width * dpiZoomScaling;
  const height = pendingSatellite.height * dpiZoomScaling;
  const x = pendingSatellite.screenX;
  const y = pendingSatellite.screenY;

  const window = new SatelliteWindow(pendingSatellite.mainWindow, pendingSatellite.name, owner, newWindow);
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
    const moveX = currentX + currentWidth / 2 - width / 2;

    // perform move
    window.window.setPosition(Math.trunc(moveX), Math.trunc(moveY));
  }

  // if we have a name set, start tracking this window
  if (pendingSatellite.name) {
    appState().windowTracker.addWindow(pendingSatellite.name, window);
  }
}

export function configureSecondaryWindow(
  pendingSecondary: PendingSecondaryWindow,
  newWindow: BrowserWindow,
  owner: WebContents,
  baseUrl?: string,
): void {
  const window = new SecondaryWindow(
    pendingSecondary.showToolbar,
    pendingSecondary.name,
    baseUrl,
    undefined,
    owner,
    pendingSecondary.allowExternalNavigate,
    newWindow,
  );
  window.mainWindow = pendingSecondary.mainWindow;

  // if we have a name set, start tracking this window
  if (pendingSecondary.name) {
    appState().windowTracker.addWindow(pendingSecondary.name, window);
  }
}

export function activateWindow(name: string): void {
  const allWindows = BrowserWindow.getAllWindows();
  for (const win of allWindows) {
    if (win.webContents.mainFrame.name === name) {
      raiseAndActivateWindow(win);
      return;
    }
  }
}

// The documentation for getFocusedWebContents() has:
//
// /**
//  * The web contents that is focused in this application, otherwise returns `null`.
//  */
//
//  static getFocusedWebContents(): WebContents;
//
// and so the documentation appears to state that the return value may be 'null',
// but the type definition doesn't propagate that reality. Hence, this wrapper function.
export function focusedWebContents(): Electron.WebContents | null {
  return webContents.getFocusedWebContents();
}

/**
 * Window geometry
 */
export interface WindowBounds {
  height: number;
  width: number;
  x: number;
  y: number;
  maximized: boolean;
}

/**
 * Check if two Rectangles intersect.
 *
 * @param one First rectangle
 * @param two Second rectangle
 *
 * @returns True if at least one pixel is within both rectangles.
 */
export function intersects(first: Rectangle, second: Rectangle): boolean {
  const firstRight = first.x + first.width;
  const firstBottom = first.y + first.height;
  const secondRight = second.x + second.width;
  const secondBottom = second.y + second.height;

  if (first.x >= secondRight || firstRight <= second.x) {
    // Don't overlap horizontally
    return false;
  }

  if (first.y >= secondBottom || firstBottom <= second.y) {
    // Don't overlap vertically
    return false;
  }

  return true;
}

/**
 * Resize and reposition a window to requested location, and ensure it is visible and not too
 * small (e.g. if monitor configuration has changed).
 *
 * @param window window to reposition and resize
 * @param requestedBounds requested size and position
 * @param defaultWidth width to use if width must be changed
 * @param defaultHeight height to use if height must be changed
 */
export function positionAndEnsureVisible(
  window: BrowserWindow,
  requestedBounds: Rectangle,
  defaultWidth: number,
  defaultHeight: number) {

  try {
    const minWidth = 300;
    const minHeight = 200;
    
    // Shrink the window rectangle a bit just to capture cases like RStudio
    // too close to edge of window and hardly showing at all.
    const checkRect = {
      height: requestedBounds.height - 5,
      width: requestedBounds.width - 5,
      x: requestedBounds.x + 5,
      y: requestedBounds.y + 5,
    };
    
    // check for intersection
    const goodDisplays = screen.getAllDisplays().find((display) => {
      return intersects(checkRect, display.workArea);
    });
    
    // Restore it to requested location if possible, or center of primary display otherwise
    if (goodDisplays) {
      window.setBounds(requestedBounds);
    } else {
      const primaryBounds = screen.getPrimaryDisplay().bounds;
      const newSize = {
        width: Math.min(defaultWidth, primaryBounds.width),
        height: Math.min(defaultHeight, primaryBounds.height),
      };
      
      window.setSize(newSize.width, newSize.height);
      
      // window.center() doesn't consistently pick the primary display,
      // so manually calculating the center of the primary display
      window.setPosition(
        primaryBounds.x + (primaryBounds.width - newSize.width) / 2,
        primaryBounds.y + (primaryBounds.height - newSize.height) / 2,
        false
      );
    }
      
    // ensure a minimum size for the window on restore
    const currSize = window.getSize();
    window.setSize(Math.max(minWidth, currSize[0]), Math.max(minHeight, currSize[1]));

  } catch (err: unknown) {
    logger().logError(safeError(err));
  }
}