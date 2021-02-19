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
    this.lastWasTools = false;
    this.lastWasDiagnostics = false;

    ipcMain.on('menu_begin_main', (event) => {
      this.mainMenu = new Menu();
      if (process.platform === 'darwin') {
        this.mainMenu.append(new MenuItem({role: 'appMenu'}));
      }
    });

    ipcMain.on('menu_begin', (event, label) => {
      let subMenu = new Menu();
      let opts = {submenu: subMenu, label: label};
      if (label === '&File') {
        opts.role = 'fileMenu';
      } else if (label === '&Edit') {
        opts.role = 'editMenu';
      } else if (label === '&View') {
        opts.role = 'viewMenu';
      } else if (label === '&Help') {
        opts.role = 'help';
      } else if (label === '&Tools') {
        this.lastWasTools = true;
      } else if (label === 'Dia&gnostics') {
        this.lastWasDiagnostics = true;
      }

      let menuItem = new MenuItem(opts);
      if (this.menuStack.length == 0) {
        this.mainMenu.append(menuItem);
      } else {
        this.addToCurrentMenu(menuItem);
      }
      this.menuStack.push(subMenu);
    });

    ipcMain.on('menu_add_command', (event, cmdId, label, tooltip, shortcut, checkable) => {
      console.log(shortcut);
      let menuItemOpts = {label: label, id: cmdId};
      if (checkable) {
        menuItemOpts.checked = false;
      }
      if (label === 'Actual &Size') {
        menuItemOpts.role = 'resetZoom';
      } else if (label === '&Zoom In') {
        menuItemOpts.role = 'zoomIn';
      } else if (label === 'Zoom O&ut') {
        menuItemOpts.role = 'zoomOut';
      }
      this.addToCurrentMenu(new MenuItem(menuItemOpts));
    });

    ipcMain.on('menu_add_separator', (event) => {
      let separator = new MenuItem({type: 'separator'});
      this.addToCurrentMenu(separator);
    });

    ipcMain.on('menu_end', (event) => {
      if (this.lastWasDiagnostics) {
        this.lastWasDiagnostics = false;
        this.addToCurrentMenu(new MenuItem({role: 'toggleDevTools'}));
      }

      this.menuStack.pop();

      if (this.lastWasTools) {
        this.lastWasTools = false;

        // add the Window menu on mac
        if (process.platform === 'darwin') {
          this.mainMenu.append(new MenuItem({role: 'windowMenu'}));
        }
      }
    });

    ipcMain.on('menu_end_main', (event) => {
      Menu.setApplicationMenu(this.mainMenu);
    });

    ipcMain.on('menu_set_command_visible', (event, commandId, visible) => {
      let item = this.getMenuItemById(commandId);
      if (item) {
        item.visible = visible;
      }
    });

    ipcMain.on('menu_set_command_enabled', (event, commandId, enabled) => {
      let item = this.getMenuItemById(commandId);
      if (item) {
        item.enabled = enabled;
      }
    });

    ipcMain.on('menu_set_command_checked', (event, commandId, checked) => {
      let item = this.getMenuItemById(commandId);
      if (item) {
        item.checked = checked;
      }
    });

    ipcMain.on('menu_set_main_menu_enabled', (event, enabled) => {
    });

    ipcMain.on('menu_set_command_label', (event, commandId, label) => {
      let item = this.getMenuItemById(commandId);
      if (item) {
        item.label = label;
      }
    });
  }

  addToCurrentMenu(menuItem) {
    if (this.menuStack.length > 0) {
      this.menuStack[this.menuStack.length - 1].append(menuItem);
    }
  }

  getMenuItemById(id) {
    return this.mainMenu ? this.mainMenu.getMenuItemById(id) : null;
  }

  convertShortcut(shortcut) {

  }
};
