/*
 * menu-bridge.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { ipcRenderer } from 'electron';

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getMenuBridge() {
  return {
    beginMainMenu: () => {
      ipcRenderer.send('menu_begin_main');
    },

    beginMenu: (label: string) => {
      ipcRenderer.send('menu_begin', label);
    },

    addCommand: (
      cmdId: string,
      label: string,
      tooltip: string,
      shortcut: string,
      isCheckable: boolean,
      isRadio: boolean,
      isVisible: boolean,
    ) => {
      ipcRenderer.send('menu_add_command', cmdId, label, tooltip, shortcut, isCheckable, isRadio, isVisible);
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

    setCommandVisible: (commandId: string, visible: boolean) => {
      ipcRenderer.send('menu_set_command_visible', commandId, visible);
    },

    setCommandEnabled: (commandId: string, enabled: boolean) => {
      ipcRenderer.send('menu_set_command_enabled', commandId, enabled);
    },

    setCommandChecked: (commandId: string, checked: boolean) => {
      ipcRenderer.send('menu_set_command_checked', commandId, checked);
    },

    setMainMenuEnabled: (enabled: boolean) => {
      ipcRenderer.send('menu_set_main_menu_enabled', enabled);
    },

    setCommandLabel: (commandId: string, label: string) => {
      ipcRenderer.send('menu_set_command_label', commandId, label);
    },

    setCommandShortcut: (commandId: string, shortcut: string) => {
      ipcRenderer.send('menu_set_command_shortcut', commandId, shortcut);
    },

    commitCommandShortcuts: () => {
      ipcRenderer.send('menu_commit_command_shortcuts');
    },
  };
}
