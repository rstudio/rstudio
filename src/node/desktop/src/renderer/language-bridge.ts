/*
 * language-bridge.ts
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

import { contextBridge, ipcRenderer } from 'electron';

import { kGetLanguageChannel } from '../core/desktop-language';

declare global {
  interface Window {
    // set by exposeDesktopLanguage() below, in the preload of each desktop page
    desktopLanguage: string;
  }
}

/**
 * Exposes the UI language as `window.desktopLanguage`. Preload runs on every page load, so
 * each page sees the language in effect when it loaded.
 */
export function exposeDesktopLanguage(): void {
  contextBridge.exposeInMainWorld('desktopLanguage', ipcRenderer.sendSync(kGetLanguageChannel));
}
