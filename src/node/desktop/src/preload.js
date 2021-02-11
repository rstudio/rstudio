/*
 * preload.js
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
const { contextBridge } = require('electron')

// see DesktopGwtCallback in QtWebEngine version, attached in DesktopMainWindow constructor
contextBridge.exposeInMainWorld('desktop', {
});

// RDP-only
contextBridge.exposeInMainWorld('remoteDesktop', {
});

contextBridge.exposeInMainWorld('desktopInfo', {
  chromiumDevtoolsPort: () => {console.log('chromiumDevToolsPort'); return 123; }
});

contextBridge.exposeInMainWorld('desktopMenuCallback', {
  beginMainMenu: () => {console.log('beginMainMenu')},
  beginMenu: (label) => {},
  addCommand: (cmdId, label, tooltip, shortcut, isChecked) => {},
  addSeparator: () => {},
  endMenu: () => {},
  endMainMenu: () => {console.log('endMainMenu')},
  setCommandVisible: (commandId, visible) => {},
  setCommandEnabled: (commandId, enabled) => {},
  setCommandChecked: (commandId, checked) => {},
  setMainMenuEnabled: (enabled) => {},
  setCommandLabel: (commandId, label) => {},
});
