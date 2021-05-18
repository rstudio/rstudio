/*
 * preload.ts
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
import { contextBridge } from 'electron';
import getMenuBridge from './menu-bridge';
import getDesktopBridge from './desktop-bridge';
import getDesktopInfoBridge from './desktop-info-bridge';

/**
 * The preload script is run in the renderer before our GWT code and enables
 * setting up a bridge between the main process and the renderer process via
 * the contextBridge mechanism.
 * 
 * Code in here has access to powerful node.js and Electron APIs even though
 * the renderer itself is configured with node disabled and context isolation.
 * 
 * Be careful to only expose the exact APIs desired; DO NOT expose general-purpose
 * IPC objects, etc.
 * 
 * Actual implementation happens in the main process, reached via ipcRenderer.
 */

contextBridge.exposeInMainWorld('desktop', getDesktopBridge());
contextBridge.exposeInMainWorld('desktopInfo', getDesktopInfoBridge());
contextBridge.exposeInMainWorld('desktopMenuCallback', getMenuBridge());

// RDP-only
//contextBridge.exposeInMainWorld('remoteDesktop', {});
