/*
 * preload.ts
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
import { contextBridge, ipcRenderer } from 'electron';

import { removeDups } from '../core/string-utils';

import { getDesktopInfoBridge } from './desktop-info-bridge';
import { getMenuBridge } from './menu-bridge';
import { getDesktopBridge } from './desktop-bridge';
import { firstStartingWith } from '../core/array-utils';
import { getDesktopLoggerBridge, logString } from './logger-bridge';

/**
 * The preload script is run in the renderer before our GWT code and enables
 * setting up a bridge between the main process and the renderer process via
 * the contextBridge mechanism.
 *
 * Preload code has access to powerful node.js and Electron APIs even though
 * the renderer itself is configured with node disabled and context isolation.
 *
 * Be careful to only expose the exact APIs desired; DO NOT expose general-purpose
 * IPC objects, etc.
 *
 * Actual implementation happens in the main process, reached via ipcRenderer.
 */

contextBridge.exposeInMainWorld('desktopLogger', getDesktopLoggerBridge());

const apiKeys = removeDups(firstStartingWith(process.argv, '--api-keys=').split('|'));
let desktopApiConnected = false;
for (const apiKey of apiKeys) {
  switch (apiKey) {
    case 'desktop':
      logString('debug', '[preload] connecting desktop hooks');
      contextBridge.exposeInMainWorld(apiKey, getDesktopBridge());
      desktopApiConnected = true;
      break;
    case 'desktopInfo':
      logString('debug', '[preload] connecting desktopInfo hooks');
      contextBridge.exposeInMainWorld(apiKey, getDesktopInfoBridge());
      break;
    case 'desktopMenuCallback':
      logString('debug', '[preload] connecting desktopMenuCallback hooks');
      contextBridge.exposeInMainWorld(apiKey, getMenuBridge());
      break;
    default:
      logString('debug', `[preload] ignoring unsupported apiKey: '${apiKey}'`);
  }
}

// Set up Ctrl/Cmd+Mousewheel zoom support
if (desktopApiConnected) {
  let lastZoomTime = 0;
  let mousewheelZoomEnabled = false; // Default to disabled
  let zoomDebounceDurationMs = 100; // Default debounce time

  // Listen for preference updates from the main process
  ipcRenderer.on('desktop_set_mousewheel_zoom_enabled', (_event, enabled: boolean) => {
    mousewheelZoomEnabled = enabled;
    logString('debug', `[preload] mousewheel zoom ${enabled ? 'enabled' : 'disabled'}`);
  });
  ipcRenderer.on('desktop_set_mousewheel_zoom_debounce', (_event, zoomDebounceMs: number) => {
    zoomDebounceDurationMs = zoomDebounceMs;
    logString('debug', `[preload] mousewheel zoom debounce ${zoomDebounceDurationMs} ms`);
  });

  window.addEventListener('DOMContentLoaded', () => {
    logString('debug', '[preload] setting up mousewheel zoom handler');

    window.addEventListener(
      'wheel',
      (event) => {
        // Only process if the feature is enabled
        if (!mousewheelZoomEnabled) {
          return;
        }

        // Check if Ctrl (Windows/Linux) or Cmd (macOS) is pressed
        const isModifierPressed = process.platform === 'darwin' ? event.metaKey : event.ctrlKey;

        if (isModifierPressed) {
          // Prevent default scrolling behavior
          event.preventDefault();

          // Throttle zoom events
          const currentTime = Date.now();
          if (currentTime - lastZoomTime < zoomDebounceDurationMs) {
            return;
          }
          lastZoomTime = currentTime;

          // Determine zoom direction based on wheel delta
          // Negative deltaY means scrolling up (zoom in), positive means scrolling down (zoom out)
          if (event.deltaY < 0) {
            ipcRenderer.send('desktop_zoom_in');
          } else if (event.deltaY > 0) {
            ipcRenderer.send('desktop_zoom_out');
          }
        }
      },
      { passive: false },
    ); // passive: false allows us to call preventDefault
  });
}
