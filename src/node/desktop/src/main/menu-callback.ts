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

  mainMenu: Menu = new Menu();
  menuStack: MenuItemConstructorOptions[][] = [];

  // keep a list of templates referenced by id so we can re-build the menus with them
  private menuItemTemplates = new Map<string, MenuItemConstructorOptions>();

  // keep a list of templates referenced by menu so we can re-build the menu
  private mainMenuTemplate: MenuItemConstructorOptions[] = new Array<MenuItemConstructorOptions>();

  lastWasTools = false;
  lastWasDiagnostics = false;

  private setShortcutDebounceId?: NodeJS.Timeout;

  constructor() {
    super();
    ipcMain.on('menu_begin_main', () => {
      this.beginMain();
    });

    ipcMain.on('menu_begin', (_event, label: string) => {
      this.menuBegin(label);
    });

    ipcMain.on(
      'menu_add_command',
      (
        _event,
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
      const separatorTemplate: MenuItemConstructorOptions = { type: 'separator' };
      this.addToCurrentMenu(separatorTemplate);
    });

    ipcMain.on('menu_end', () => {
      this.menuEnd();
    });

    ipcMain.on('menu_end_main', () => {
      this.emit(MenuCallback.MENUBAR_COMPLETED, this.mainMenu);
    });

    // for all events that modify commands, their templates must also be modified!

    ipcMain.on('menu_set_command_visible', (_event, commandId: string, visible: boolean) => {
      this.setCommandVisibility(commandId, visible);
    });

    ipcMain.on('menu_set_command_enabled', (_event, commandId: string, enabled: boolean) => {
      this.setCommandEnabled(commandId, enabled);
    });

    ipcMain.on('menu_set_command_checked', (_event, commandId: string, checked: boolean) => {
      this.setCommandChecked(commandId, checked);
    });

    ipcMain.on('menu_set_main_menu_enabled', () => {
      /* do nothing */
    });

    ipcMain.on('menu_set_command_label', (_event, commandId: string, label: string) => {
      this.setCommandLabel(commandId, label);
    });

    ipcMain.on('menu_set_command_shortcut', (_event, commandId: string, shortcut: string | null) => {
      this.setCommandShortcut(commandId, shortcut);
    });

    ipcMain.on('menu_commit_command_shortcuts', () => {
      if (this.setShortcutDebounceId) clearTimeout(this.setShortcutDebounceId);

      this.updateMenus();
    });
  }

  beginMain(): void {
    // Create a new menu and clear the template
    // GWT redefines and rebuilds the entire menu in some cases
    this.mainMenu = new Menu();
    this.mainMenuTemplate = new Array<MenuItemConstructorOptions>();

    if (process.platform === 'darwin') {
      const appMenu: MenuItemConstructorOptions = { role: 'appMenu', visible: true };
      this.addToCurrentMenu(appMenu);
    }
  }

  menuBegin(label: string): void {
    const subMenu = new Array<MenuItemConstructorOptions>();
    const opts: MenuItemConstructorOptions = {
      submenu: subMenu,
      label: label,
      id: menuIdFromLabel(label),
      visible: true,
    };
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

    if (opts.id) {
      this.menuItemTemplates.set(opts.id, opts);
    }
    this.addToCurrentMenu(opts);
    this.menuStack.push(subMenu);
  }

  menuEnd(): void {
    if (this.lastWasDiagnostics) {
      this.lastWasDiagnostics = false;
      const template: MenuItemConstructorOptions = { role: 'toggleDevTools' };
      this.addToCurrentMenu(template);
    }

    this.menuStack.pop();

    if (this.lastWasTools) {
      this.lastWasTools = false;

      // add the Window menu on mac
      if (process.platform === 'darwin') {
        this.mainMenu.append(new MenuItem({ role: 'windowMenu' }));
      }
    }
  }

  setCommandShortcut(id: string, shortcut: string | null) {
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
    const template = this.menuItemTemplates.get(id);
    if (!template) return;

    const accelerator = this.convertShortcut(shortcut);
    template.accelerator = accelerator;

    if (this.setShortcutDebounceId) clearTimeout(this.setShortcutDebounceId);

    this.setShortcutDebounceId = setTimeout(() => {
      this.updateMenus();
    }, 5000);
  }

  setCommandVisibility(id: string, newVisibility: boolean) {
    const template = this.menuItemTemplates.get(id);
    if (template) template.visible = newVisibility;
  }

  setCommandEnabled(id: string, newEnablement: boolean) {
    const template = this.menuItemTemplates.get(id);
    if (template) template.enabled = newEnablement;
  }

  setCommandChecked(id: string, newChecked: boolean) {
    const template = this.menuItemTemplates.get(id);
    if (template) template.checked = newChecked;
  }

  setCommandLabel(id: string, newLabel: string) {
    const template = this.menuItemTemplates.get(id);
    if (template) template.label = newLabel;
  }

  /**
   * Reconstructs the entire app menu using the _mainMenuTemplate_. Do not update the menu item.
   *
   * This function performs will also clean up unnecessary menu items such as hidden items or
   * unnecessary separators. It is important that this updates the menu templates since hidden items
   * and unnecessary separators are not added to menus. Hidden items are not added so that the
   * separator logic can remove unnecessary separators. Unnecessary separators and items are removed
   * because it is easier than figuring out which ones to hide.
   *
   * Use caution when calling this because this it is an expensive call.
   *
   */
  updateMenus(): void {
    /**
     * Builds the final list of menu items using the given menu template
     *
     * @param menuItemTemplates a list of menu item templates representing the full menu (may include hidden items)
     * @returns the final template list after removing unnecessary separators and hidden items
     */
    const recursiveCopy = (menuItemTemplates: MenuItemConstructorOptions[]) => {
      const newMenuTemplate = new Array<MenuItemConstructorOptions>();

      for (const menuItemTemplate of menuItemTemplates) {
        let referenceMenuItemTemplate;
        if (menuItemTemplate.id) {
          referenceMenuItemTemplate = this.menuItemTemplates.get(menuItemTemplate.id) ?? {};
        }
        // the new menu item is based on foundMenuItemTemplate then the referenceMenuItemTemplate
        // menuItemTemplate is the current state (i.e. checkbox state)
        const newMenuItemTemplate = { ...menuItemTemplate, ...referenceMenuItemTemplate };

        if (newMenuItemTemplate.type === 'separator') {
          newMenuTemplate.push(newMenuItemTemplate);
          continue;
        }

        if (newMenuItemTemplate.id) {
          this.menuItemTemplates.set(newMenuItemTemplate.id, newMenuItemTemplate);
        }

        if (!newMenuItemTemplate.visible && !newMenuItemTemplate.role) continue;

        const { submenu } = menuItemTemplate;
        if (submenu instanceof Array) {
          newMenuItemTemplate.submenu = recursiveCopy(submenu as Array<MenuItemConstructorOptions>);
        }

        newMenuTemplate.push(newMenuItemTemplate);
      }

      return newMenuTemplate.filter((item, idx, arr) => {
        if (item.type !== 'separator') {
          return true;
        }
        const prevItem = arr[idx - 1];
        return idx !== 0 && prevItem.type !== 'separator' && idx != arr.length - 1;
      });
    };

    const newMainMenuTemplate = recursiveCopy(this.mainMenuTemplate);
    this.mainMenu = Menu.buildFromTemplate(newMainMenuTemplate);

    Menu.setApplicationMenu(this.mainMenu);
  }

  addCommand(
    cmdId: string,
    label: string,
    _tooltip: string,
    shortcut: string,
    checkable: boolean,
    visible: boolean,
  ): void {
    const menuItemOpts: MenuItemConstructorOptions = {
      label: label,
      id: cmdId,
      click: (menuItem, _browserWindow, _event) => {
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
      menuItemOpts.accelerator = 'CommandOrControl+=';
    } else if (cmdId === 'zoomOut') {
      menuItemOpts.accelerator = 'CommandOrControl+-';
    } else if (cmdId === 'cutDummy') {
      menuItemOpts.role = 'cut';
    } else if (cmdId === 'copyDummy') {
      menuItemOpts.role = 'copy';
    } else if (cmdId === 'pasteDummy') {
      menuItemOpts.role = 'paste';
    } else if (cmdId === 'pasteWithIndentDummy') {
      menuItemOpts.role = 'pasteAndMatchStyle';
    } else if (cmdId === 'undoDummy') {
      menuItemOpts.accelerator = 'CommandOrControl+Z';
    } else if (cmdId === 'redoDummy') {
      menuItemOpts.accelerator = 'CommandOrControl+Shift+Z';
    }

    const menuItem = new MenuItem(menuItemOpts);
    this.menuItemTemplates.set(menuItem.id, menuItemOpts);
    this.addToCurrentMenu(menuItemOpts);
  }

  addToCurrentMenu(itemTemplate: MenuItemConstructorOptions): void {
    let menu: MenuItemConstructorOptions[] | undefined;
    if (this.menuStack.length > 0) {
      menu = this.menuStack[this.menuStack.length - 1];
    } else {
      menu = new Array<MenuItemConstructorOptions>();
      this.mainMenuTemplate.push(itemTemplate);
    }
    menu.push(itemTemplate);
  }

  getMenuItemById(id: string): MenuItemConstructorOptions | undefined {
    return this.menuItemTemplates.get(id);
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
