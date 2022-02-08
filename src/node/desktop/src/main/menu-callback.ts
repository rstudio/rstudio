/*
 * menu-callback.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import EventEmitter from 'events';

/**
 * Show dummy menu bar to deal with the fact that the real menu bar isn't ready until well
 * after startup.
 */
export function showPlaceholderMenu(): void {
  const addPlaceholderMenuItem = function (mainMenu: Menu, label: string): void {
    mainMenu.append(new MenuItem({ submenu: new Menu(), label: label }));
  };

  const mainMenuStub = new Menu();
  if (process.platform === 'darwin') {
    mainMenuStub.append(new MenuItem({ role: 'appMenu' }));
  }
  addPlaceholderMenuItem(mainMenuStub, 'File');
  addPlaceholderMenuItem(mainMenuStub, 'Edit');
  addPlaceholderMenuItem(mainMenuStub, 'Code');
  addPlaceholderMenuItem(mainMenuStub, 'View');
  addPlaceholderMenuItem(mainMenuStub, 'Plots');
  addPlaceholderMenuItem(mainMenuStub, 'Session');
  addPlaceholderMenuItem(mainMenuStub, 'Build');
  addPlaceholderMenuItem(mainMenuStub, 'Debug');
  addPlaceholderMenuItem(mainMenuStub, 'Profile');
  addPlaceholderMenuItem(mainMenuStub, 'Tools');
  if (process.platform === 'darwin') {
    addPlaceholderMenuItem(mainMenuStub, 'Window');
  }
  addPlaceholderMenuItem(mainMenuStub, 'Help');
  Menu.setApplicationMenu(mainMenuStub);
}

function menuIdFromLabel(label: string): string {
  return label.replace('&', '');
}

/**
 * Callbacks from renderer to create application menu.
 */
export class MenuCallback extends EventEmitter {
  static MENUBAR_COMPLETED = 'menu-callback-menubar_completed';
  static MANAGE_COMMAND = 'menu-callback-manage_command';
  static COMMAND_INVOKED = 'menu-callback-command_invoked';
  static ZOOM_ACTUAL_SIZE = 'menu-callback-zoom_actual_size';
  static ZOOM_IN = 'menu-callback-zoom_in';
  static ZOOM_OUT = 'menu-callback-zoom_out';

  mainMenu: Menu | null = null;
  menuStack: Menu[] = [];
  actions = new Map<string, MenuItem>();

  // keep a list of templates around so we can re-build the menus with them
  private menuItemTemplates = new Map<MenuItem, MenuItemConstructorOptions>();

  lastWasTools = false;
  lastWasDiagnostics = false;

  private setShortcutDebounceId?: NodeJS.Timeout;
  private setShortcutQueue = new Set<MenuItemConstructorOptions>();

  constructor() {
    super();
    ipcMain.on('menu_begin_main', () => {
      this.beginMain();
    });

    ipcMain.on('menu_begin', (event, label: string) => {
      this.menuBegin(label);
    });

    ipcMain.on(
      'menu_add_command',
      (
        event,
        cmdId: string,
        label: string,
        tooltip: string,
        shortcut: string,
        checkable: boolean,
        visible: boolean,
      ) => {
        this.addCommand(cmdId, label, tooltip, shortcut, checkable, visible);
      },
    );

    ipcMain.on('menu_add_separator', () => {
      const separator = new MenuItem({ type: 'separator' });
      this.addToCurrentMenu(separator);
    });

    ipcMain.on('menu_end', () => {
      if (this.lastWasDiagnostics) {
        this.lastWasDiagnostics = false;
        this.addToCurrentMenu(new MenuItem({ role: 'toggleDevTools' }));
      }

      this.menuStack.pop();

      if (this.lastWasTools) {
        this.lastWasTools = false;

        // add the Window menu on mac
        if (process.platform === 'darwin') {
          this.mainMenu?.append(new MenuItem({ role: 'windowMenu' }));
        }
      }
    });

    ipcMain.on('menu_end_main', () => {
      this.emit(MenuCallback.MENUBAR_COMPLETED, this.mainMenu);
    });

    // for all events that modify commands, their templates must also be modified!

    ipcMain.on('menu_set_command_visible', (event, commandId: string, visible: boolean) => {
      const item = this.getMenuItemById(commandId);
      if (item) {
        item.visible = visible;
        const template = this.menuItemTemplates.get(item);
        if (template) template.visible = visible;
      }
    });

    ipcMain.on('menu_set_command_enabled', (event, commandId: string, enabled: boolean) => {
      const item = this.getMenuItemById(commandId);
      if (item) {
        item.enabled = enabled;
        const template = this.menuItemTemplates.get(item);
        if (template) template.enabled = enabled;
      }
    });

    ipcMain.on('menu_set_command_checked', (event, commandId: string, checked: boolean) => {
      const item = this.getMenuItemById(commandId);
      if (item) {
        item.checked = checked;
        const template = this.menuItemTemplates.get(item);
        if (template) template.checked = checked;
      }
    });

    ipcMain.on('menu_set_main_menu_enabled', () => {
      /* do nothing */
    });

    ipcMain.on('menu_set_command_label', (event, commandId: string, label: string) => {
      const item = this.getMenuItemById(commandId);
      if (item) {
        item.label = label;
        const template = this.menuItemTemplates.get(item);
        if (template) template.label = label;
      }
    });

    ipcMain.on('menu_set_command_shortcut', (event, commandId: string, shortcut: string | null) => {
      // Electron doesn't support modifying shortcut of an existing MenuItem;
      // We have to recreate the application menus whenever* we
      // get this call. For more on this Electron limitation:
      // https://github.com/electron/electron/issues/528
      //
      // You'd think you could remove the old menu item and replace it with an updated
      // version, but no such thing as menu.remove:
      // https://github.com/electron/electron/issues/527
      //
      // * this call automatically commits changes after 5 seconds
      //   if the "commit" event is not called before then
      const item = this.getMenuItemById(commandId);
      if (item) {
        const template = this.menuItemTemplates.get(item);
        if (!template) return;

        const accelerator = this.convertShortcut(shortcut);
        template.accelerator = accelerator;
        this.setShortcutQueue.add({ ...template, accelerator });

        if (this.setShortcutDebounceId) clearTimeout(this.setShortcutDebounceId);

        this.setShortcutDebounceId = setTimeout(() => {
          this.updateMenus(Array.from(this.setShortcutQueue.values()));
          this.setShortcutQueue.clear();
        }, 5000);
      }
    });

    ipcMain.on('menu_commit_command_shortcuts', () => {
      if (this.setShortcutDebounceId) clearTimeout(this.setShortcutDebounceId);

      this.updateMenus(Array.from(this.setShortcutQueue.values()));
      this.setShortcutQueue.clear();
    });
  }

  beginMain(): void {
    this.mainMenu = new Menu();
    if (process.platform === 'darwin') {
      this.mainMenu.append(new MenuItem({ role: 'appMenu' }));
    }
  }

  menuBegin(label: string): void {
    const subMenu = new Menu();
    const opts: MenuItemConstructorOptions = { submenu: subMenu, label: label, id: menuIdFromLabel(label) };
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

    const menuItem = new MenuItem(opts);
    this.menuItemTemplates.set(menuItem, opts);
    if (this.menuStack.length == 0) {
      this.mainMenu?.append(menuItem);
    } else {
      this.addToCurrentMenu(menuItem);
    }
    this.menuStack.push(subMenu);
  }

  /**
   * Uses a list of templates to update existing menu items by reconstructing the entire app menu
   *
   * this function performs updates ONLY -- it does not add or remove anything
   * @param items a list of MenuItemConstructorOptions that will overwrite existing menu items
   */
  updateMenus(items: MenuItemConstructorOptions[]): void {
    const mainMenu = this.mainMenu;
    if (!mainMenu) return;

    const newMenu = new Menu();

    const recursiveCopy = (currentMenu: Menu, targetMenu: Menu) => {
      for (const item of currentMenu.items) {
        const itemTemplate = this.menuItemTemplates.get(item);
        if (!itemTemplate) {
          // no itemTemplate found (separators, for example)
          targetMenu.append(item);
          continue;
        }

        const foundTemplate = items.find((item) => item.id === itemTemplate.id);
        const newItemTemplate = { ...itemTemplate, ...foundTemplate };

        // remove the current entry, as it will be replaced later
        this.menuItemTemplates.delete(item);

        // itemTemplate can have a submenu, but that needs to be copied through iteration
        // to avoid copying by reference
        const { submenu } = newItemTemplate;

        if (submenu instanceof Menu) {
          const newSubmenu = new Menu();
          recursiveCopy(submenu as Menu, newSubmenu);
          newItemTemplate.submenu = newSubmenu;
        }

        const newMenuItem = new MenuItem(newItemTemplate);

        // replace the existing item with the new item in the actions list
        if (this.actions.delete(item.id)) {
          this.actions.set(item.id, newMenuItem);
        }

        this.menuItemTemplates.set(newMenuItem, newItemTemplate);
        targetMenu.append(newMenuItem);
      }
    };

    recursiveCopy(mainMenu, newMenu);
    this.mainMenu = newMenu;

    Menu.setApplicationMenu(this.mainMenu);
  }

  addCommand(
    cmdId: string,
    label: string,
    tooltip: string,
    shortcut: string,
    checkable: boolean,
    visible: boolean,
  ): void {
    const menuItemOpts: MenuItemConstructorOptions = {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      label: label,
      id: cmdId,
      click: (menuItem, browserWindow, event) => {
        this.emit(MenuCallback.COMMAND_INVOKED, menuItem.id);
      },
    };

    if (checkable) {
      menuItemOpts.checked = false;
    }
    if (shortcut.length > 0) {
      menuItemOpts.accelerator = this.convertShortcut(shortcut);
    }
    menuItemOpts.visible = visible;

    // some shortcuts (namely, the Edit shortcuts) don't have bindings on the client side.
    // populate those here when discovered

    // TODO: probably need to not use the roles here, and instead follow pattern in the C++
    // code where we assign shortcuts to these commands and let them flow through
    // our regular command handling
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

    const menuItem = new MenuItem(menuItemOpts);
    this.actions.set(cmdId, menuItem);
    this.menuItemTemplates.set(menuItem, menuItemOpts);
    this.addToCurrentMenu(menuItem);
  }

  addToCurrentMenu(menuItem: MenuItem): void {
    if (this.menuStack.length > 0) {
      this.menuStack[this.menuStack.length - 1].append(menuItem);
    }
  }

  getMenuItemById(id: string): MenuItem | undefined {
    return this.actions.get(id);
  }

  cleanUpActions(): void {
    // TODO
  }

  /**
   * Convert RStudio shortcut string to Electron Accelerator
   */
  convertShortcut(shortcut: string | null): string {
    if (!shortcut) return '';

    return shortcut
      .split('+')
      .map((key) => {
        if (key === 'Cmd') {
          return 'CommandOrControl';
        } else if (key === 'Meta') {
          return 'Command';
        } else {
          return key;
        }
      })
      .join('+');
  }
}
