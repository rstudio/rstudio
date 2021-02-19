/*
 * menu-callback.js
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
const { ipcMain, Menu, MenuItem } = require('electron');

module.exports = class MenuCallback {
  constructor() {

    this.mainMenu = null;
    this.menuStack = [];

    ipcMain.on('menu_begin_main', (event) => {
      this.mainMenu = new Menu();
      if (process.platform === 'darwin') {
        let appMenu = new MenuItem({role: "appMenu"});
        this.mainMenu.append(appMenu);
      }
    });

    ipcMain.on('menu_begin', (event, label) => {
      let subMenu = new Menu();
      let menuItem = new MenuItem({submenu: subMenu, label: label});
      if (this.menuStack.length == 0) {
        this.mainMenu.append(menuItem);
      } else {
        this.menuStack[this.menuStack.length - 1].append(menuItem);
      }
      this.menuStack.push(subMenu);
    });

    ipcMain.on('menu_add_command', (event, cmdId, label, tooltip, shortcut, isChecked) => {
    });

    ipcMain.on('menu_add_separator', (event) => {
      if (this.menuStack.length > 0) {
        let separator = new MenuItem({type: "separator"});
        this.menuStack[this.menuStack.length - 1].append(separator);
      }
    });

    ipcMain.on('menu_end', (event) => {
      this.menuStack.pop();
    });

    ipcMain.on('menu_end_main', (event) => {
      Menu.setApplicationMenu(this.mainMenu);
    });

    ipcMain.on('menu_set_command_visible', (event, commandId, visible) => {
    });

    ipcMain.on('menu_set_command_enabled', (event, commandId, enabled) => {
    });

    ipcMain.on('menu_set_command_checked', (event, commandId, checked) => {
    });

    ipcMain.on('menu_set_main_menu_enabled', (event, enabled) => {
    });

    ipcMain.on('menu_set_command_label', (event, commandId, label) => {
    });
  }
}
