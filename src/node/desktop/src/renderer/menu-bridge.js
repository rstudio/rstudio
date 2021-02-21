/*
 * menu-bridge.js
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

const { ipcRenderer } = require('electron');

exports.getMenuBridge = () => {
  return {
    beginMainMenu: () => {
      ipcRenderer.send('menu_begin_main');
    },

    beginMenu: (label) => {
      ipcRenderer.send('menu_begin', label);
    },

    addCommand: (cmdId, label, tooltip, shortcut, isChecked) => {
      ipcRenderer.send('menu_add_command', cmdId, label, tooltip, shortcut, isChecked);
    },

    addSeparator: () => {
      ipcRenderer.send('menu_add_separator');
    },

    endMenu: () => {
      ipcRenderer.send('menu_end');
    },

    endMainMenu: () => {
      ipcRenderer.send('menu_end_main');
    },

    setCommandVisible: (commandId, visible) => {
      ipcRenderer.send('menu_set_command_visible', commandId, visible);
    },

    setCommandEnabled: (commandId, enabled) => {
      ipcRenderer.send('menu_set_command_enabled', commandId, enabled);
    },

    setCommandChecked: (commandId, checked) => {
      ipcRenderer.send('menu_set_command_checked', commandId, checked);
    },

    setMainMenuEnabled: (enabled) => {
      ipcRenderer.send('menu_set_main_menu_enabled', enabled);
    },

    setCommandLabel: (commandId, label) => {
      ipcRenderer.send('menu_set_command_label', commandId, label);
    },
  };
};
