/*
 * whats-new-window.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import { BrowserWindow, ipcMain, shell } from 'electron';

import { logger } from '../core/logger';

declare const WHATS_NEW_WEBPACK_ENTRY: string;
declare const WHATS_NEW_PRELOAD_WEBPACK_ENTRY: string;

export interface WhatsNewWindowOptions {
  releaseSlug: string;
  releaseName: string;
  version: string;
  parent?: BrowserWindow;
  onClose?: () => void;
}

let activeWindow: BrowserWindow | null = null;
let windowReady = false;

/**
 * Show the What's New window. If one is already open, focus it instead
 * of creating a duplicate. If the window is still loading from a
 * previous call, return it without forcing it visible.
 */
export function showWhatsNewWindow(options: WhatsNewWindowOptions): BrowserWindow {
  if (activeWindow && !activeWindow.isDestroyed()) {
    if (windowReady) {
      if (activeWindow.isMinimized()) {
        activeWindow.restore();
      }
      activeWindow.show();
      activeWindow.focus();
    }
    return activeWindow;
  }

  const win = new BrowserWindow({
    width: 900,
    height: 650,
    minWidth: 500,
    minHeight: 400,
    center: true,
    title: "What's New in RStudio",
    frame: false,
    show: false,
    parent: options.parent,
    modal: !!options.parent,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      preload: WHATS_NEW_PRELOAD_WEBPACK_ENTRY,
    },
  });

  // Remove menu bar on Windows/Linux
  win.setMenuBarVisibility(false);

  // In production the host page is file://, in dev mode it's http://localhost:PORT.
  // file:// URLs have opaque origin ("null") so origin comparison doesn't work —
  // use protocol check for file:// and same-origin for http(s) dev URLs.
  const hostUrl = new URL(WHATS_NEW_WEBPACK_ENTRY);
  const isFileMode = hostUrl.protocol === 'file:';

  const isLocalUrl = (targetUrl: string): boolean => {
    try {
      const target = new URL(targetUrl);
      if (isFileMode) {
        return target.protocol === 'file:';
      }
      return target.origin === hostUrl.origin;
    } catch {
      return false;
    }
  };

  // Only open http(s) URLs externally — block custom URI schemes that
  // could launch local applications
  const openExternalSafely = (targetUrl: string): void => {
    try {
      const protocol = new URL(targetUrl).protocol;
      if (protocol === 'https:' || protocol === 'http:') {
        void shell.openExternal(targetUrl);
      }
    } catch {
      // Invalid URL — ignore
    }
  };

  // Open external links in system browser
  win.webContents.setWindowOpenHandler((details) => {
    openExternalSafely(details.url);
    return { action: 'deny' };
  });

  // Intercept main frame navigation (e.g. top-level link clicks)
  win.webContents.on('will-navigate', (event, navUrl) => {
    if (isLocalUrl(navUrl)) {
      return;
    }
    event.preventDefault();
    openExternalSafely(navUrl);
  });

  // Intercept iframe subframe navigation (links clicked inside release content).
  // Skip main frame — already handled by will-navigate above.
  win.webContents.on('will-frame-navigate', (details) => {
    if (details.isMainFrame || isLocalUrl(details.url)) {
      return;
    }
    details.preventDefault();
    openExternalSafely(details.url);
  });

  // Close on Escape key — uses before-input-event so it works even
  // when focus is inside the iframe
  win.webContents.on('before-input-event', (event, input) => {
    if (input.type === 'keyDown' && input.key === 'Escape') {
      win.close();
    }
  });

  // Track whether the window was closed abnormally so onClose (which
  // marks the release as seen) is skipped for crashes/hangs
  let abnormalClose = false;

  // If the renderer becomes unresponsive, destroy the window so it
  // doesn't permanently block the modal parent
  win.on('unresponsive', () => {
    logger().logWarning("What's New window became unresponsive, closing");
    abnormalClose = true;
    win.destroy();
  });

  // If the renderer process crashes, destroy the window
  win.webContents.on('render-process-gone', () => {
    logger().logWarning("What's New renderer process crashed, closing");
    abnormalClose = true;
    if (!win.isDestroyed()) {
      win.destroy();
    }
  });

  // Handle IPC from renderer
  const closeHandler = () => {
    win.close();
  };
  const openExternalHandler = (event: Electron.IpcMainEvent, url: string) => {
    if (event.sender !== win.webContents) {
      return;
    }
    openExternalSafely(url);
  };
  ipcMain.on('whats-new-close', closeHandler);
  ipcMain.on('whats-new-open-external', openExternalHandler);

  win.on('closed', () => {
    ipcMain.removeListener('whats-new-close', closeHandler);
    ipcMain.removeListener('whats-new-open-external', openExternalHandler);
    if (activeWindow === win) {
      activeWindow = null;
      windowReady = false;
    }
    if (!abnormalClose) {
      options.onClose?.();
    }
  });

  activeWindow = win;
  windowReady = false;

  // Build URL with query parameters
  const separator = WHATS_NEW_WEBPACK_ENTRY.includes('?') ? '&' : '?';
  const params = new URLSearchParams({
    release: options.releaseSlug,
    releaseName: options.releaseName,
    version: options.version,
  });
  const url = `${WHATS_NEW_WEBPACK_ENTRY}${separator}${params.toString()}`;

  win.loadURL(url)
    .then(() => {
      if (activeWindow === win && !win.isDestroyed()) {
        windowReady = true;
        win.show();
      }
    })
    .catch((err: unknown) => {
      logger().logError(err);
      if (activeWindow === win) {
        activeWindow = null;
        windowReady = false;
      }
      if (!win.isDestroyed()) {
        win.destroy();
      }
    });

  return win;
}
