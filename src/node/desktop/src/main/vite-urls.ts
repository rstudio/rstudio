/*
 * vite-urls.ts
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

import path from 'path';
import { pathToFileURL } from 'url';

declare const MAIN_WINDOW_VITE_DEV_SERVER_URL: string;
declare const MAIN_WINDOW_VITE_NAME: string;

/**
 * Resolve a renderer HTML page to a URL suitable for BrowserWindow.loadURL().
 * In development, returns the Vite dev server URL.
 * In production, returns a file:// URL pointing to the built asset.
 */
export function getRendererUrl(pagePath: string): string {
  if (MAIN_WINDOW_VITE_DEV_SERVER_URL) {
    return `${MAIN_WINDOW_VITE_DEV_SERVER_URL}/${pagePath}`;
  }
  const filePath = path.join(__dirname, '..', 'renderer', MAIN_WINDOW_VITE_NAME, pagePath);
  return pathToFileURL(filePath).href;
}

/**
 * Resolve a preload script path. Preload scripts are built alongside
 * main.js in the .vite/build/ directory.
 */
export function getPreloadPath(fileName: string): string {
  return path.join(__dirname, fileName);
}
