/*
 * menu-callback.ts
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
import { ipcMain, Menu, MenuItem } from 'electron';
import { MenuItemConstructorOptions } from 'electron/main';
import MainWindow from './main-window';

export default class MenuCallback {
  mainMenu?: Menu|null = null;
  menuStack: Menu[] = [];
  actions = new Map();

  lastWasTools = false;
  lastWasDiagnostics = false;

  constructor(public mainWindow: MainWindow) {

    ipcMain.on('menu_begin_main', (event) => {
      this.mainMenu = new Menu();
      if (process.platform === 'darwin') {
        this.mainMenu.append(new MenuItem({role: 'appMenu'}));
      }
    });

    ipcMain.on('menu_begin', (event, label) => {
      let subMenu = new Menu();
      let opts: MenuItemConstructorOptions = {submenu: subMenu, label: label};
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
        this.mainMenu?.append(menuItem);
      } else {
        this.addToCurrentMenu(menuItem);
      }
      this.menuStack.push(subMenu);
    });

    ipcMain.on('menu_add_command', (event, cmdId, label, tooltip, shortcut, checkable) => {
      let menuItemOpts: MenuItemConstructorOptions = {label: label, id: cmdId, click: (menuItem, browserWindow, event) => {
        this.actionInvoked(menuItem.id);
      }};

      if (checkable) {
        menuItemOpts.checked = false;
      }
      if (shortcut.length > 0) {
        menuItemOpts.accelerator = this.convertShortcut(shortcut);
      }

      // some shortcuts (namely, the Edit shortcuts) don't have bindings on the client side.
      // populate those here when discovered
       if (cmdId === 'zoomActualSize') {
        menuItemOpts.role = 'resetZoom';
      } else if (cmdId === 'zoomIn') {
        menuItemOpts.role = 'zoomIn';
      } else if (cmdId === 'zoomOut') {
        menuItemOpts.role = 'zoomOut';
      } else if (cmdId === 'cutDummy') {
        menuItemOpts.role = 'cut';
      } else if (cmdId === 'copyDummy') {
        menuItemOpts.role = 'copy';
      } else if (cmdId === 'pasteDummy') {
        menuItemOpts.role = 'paste';
      } else if (cmdId === 'pasteWithIndentDummy') {
        menuItemOpts.role = 'pasteAndMatchStyle';
      } else if (cmdId === 'undoDummy') {
        menuItemOpts.role = 'undo';
      } else if (cmdId === 'redoDummy') {
        menuItemOpts.role = 'redo';
      }

      let menuItem = new MenuItem(menuItemOpts);
      this.actions.set(cmdId, menuItem);
      this.addToCurrentMenu(menuItem);
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
          this.mainMenu?.append(new MenuItem({role: 'windowMenu'}));
        }
      }
    });

    ipcMain.on('menu_end_main', (event) => {
      if (this.mainMenu)
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

  addToCurrentMenu(menuItem: MenuItem) {
    if (this.menuStack.length > 0) {
      this.menuStack[this.menuStack.length - 1].append(menuItem);
    }
  }

  getMenuItemById(id: string) {
    return this.actions.get(id);
  }

  /**
   * Convert RStudio shortcut string to Electron Accelerator
   */
  convertShortcut(shortcut: string) {
    return shortcut.split('+').map(key => {
      if (key === 'Cmd') {
        return 'CommandOrControl';
      } else if (key === 'Meta') {
        return 'Command';
      } else {
        return key;
      }
    }).join('+');
  }

  actionInvoked(commandId: string) {
    this.mainWindow.invokeCommand(commandId);
  }
};
