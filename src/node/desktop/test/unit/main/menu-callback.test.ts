/*
 * menu-callback.test.ts
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

import { assert } from 'chai';
import { MenuItemConstructorOptions } from 'electron';
import { describe } from 'mocha';
import { MenuCallback } from '../../../src/main/menu-callback';

const separatorTemplate: MenuItemConstructorOptions = { type: 'separator' };

describe('MenuCallback', () => {
  it('can be constructed', () => {
    const callback = new MenuCallback();
    const menuCount = process.platform === 'darwin' ? 2 : 1; // adjust for MacOS app menu
    callback.beginMain();
    callback.menuBegin('&File');

    callback.updateMenus();
    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can add a command', () => {
    const callback = new MenuCallback();
    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('a_new_command', 'Test Command', '', 'Cmd+Shift+T', false, true);
    callback.updateMenus();

    assert.isObject(callback.getMenuItemById('a_new_command'));
  });

  it('can set initial visibility for command', () => {
    const callback = new MenuCallback();
    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('an_invisible_command', 'Invisible Command', '', 'Cmd+Shift+I', false, false);
    callback.addCommand('a_visible_command', 'Visible Command', '', 'Cmd+Shift+V', false, true);
    callback.updateMenus();

    const invisibleCommand = callback.getMenuItemById('an_invisible_command');
    const visibleCommand = callback.getMenuItemById('a_visible_command');

    assert.isObject(invisibleCommand);
    assert.isObject(visibleCommand);
    assert.isFalse(invisibleCommand?.visible, 'expected menu item to be invisible');
    assert.isTrue(visibleCommand?.visible, 'expected menu item to be visible');
  });

  it('can change label for a command', () => {
    const callback = new MenuCallback();
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('a_command', 'Command', '', '', false, true);

    const command = callback.getMenuItemById('a_command');
    assert.isObject(command);
    assert.strictEqual(command?.label, 'Command');

    callback.setCommandLabel('a_command', 'New Label');
    callback.updateMenus();

    const updatedCommand = callback.mainMenu.getMenuItemById('a_command');
    assert.strictEqual(updatedCommand?.label, 'New Label');
    assert.isTrue(updatedCommand?.visible);
    assert.isFalse(updatedCommand?.checked);
  });

  it('can change visibility for a command', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu
    const menuCount = process.platform === 'darwin' ? 2 : 1;

    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('a_command', 'Command', '', '', false, true);
    callback.addCommand('a_hidden_command', 'Initially hidden', '', '', false, false);
    callback.addCommand('a_visible_command', 'Initially visible', '', '', false, true);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 2);

    callback.setCommandVisibility('a_hidden_command', true);
    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 3);

    const updatedCommand = callback.mainMenu.getMenuItemById('a_hidden_command');
    assert.isTrue(updatedCommand?.visible);
    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can remove unnecessary separators', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_command', 'Command', '', '', false, true); // expected
    callback.addToCurrentMenu(separatorTemplate); // expected
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('another_command', 'Another Command', '', '', false, true); // expected
    callback.addToCurrentMenu(separatorTemplate);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 3);
  });

  it('can remove a separator that is before a hidden item', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_command', 'Command', '', '', false, true); // expected
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_hidden_command', 'Hidden Command', '', '', false, false);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 1);
  });

  it('can contain a submenu', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu
    const menuCount = process.platform === 'darwin' ? 2 : 1;

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuBegin('Recent Files');

    callback.addCommand('mru0', '', '', '', false, false);
    callback.addCommand('mru1', '', '', '', false, false);
    callback.addCommand('mru2', '', '', '', false, false);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('clear_recent', 'Clear recent', '', '', false, true);

    callback.updateMenus();
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 1, 'expected "Recent files" menu');
    assert.strictEqual(
      callback.mainMenu.items[menuIdx].submenu?.items[0].submenu?.items.length,
      1,
      'expected "Clear recent" menu item',
    );
    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can rebuild the main menu', () => {
    const callback = new MenuCallback();
    const menuCount = process.platform === 'darwin' ? 2 : 1; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuEnd();

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuBegin('Recent Files');
    callback.menuEnd();

    callback.addCommand('mru0', '', '', '', false, false);
    callback.addCommand('mru1', '', '', '', false, false);
    callback.addCommand('mru2', '', '', '', false, false);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can change a command visibility that causes unnecessary separators', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&Build');

    callback.addCommand('buildAll', 'Build All', '', '', false, true);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('buildSourcePackage', 'Build Source Package', '', '', false, false);
    callback.addCommand('buildBinaryPackage', 'Build Binary Package', '', '', false, false);
    callback.addCommand('testPackage', 'Test Package', '', '', false, false);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('configure_build', 'Configure Build Tools', '', '', false, true);

    callback.updateMenus();
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 3, 'expected 3 menu items to start');

    callback.setCommandVisibility('buildAll', false);
    callback.setCommandVisibility('buildSourcePackage', true);
    callback.setCommandVisibility('buildBinaryPackage', true);
    callback.setCommandVisibility('testPackage', true);

    callback.updateMenus();

    assert.strictEqual(
      callback.mainMenu.items[menuIdx].submenu?.items.length,
      5,
      'expected 5 menu items after changing text',
    );
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[0].id, 'buildSourcePackage');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[1].id, 'buildBinaryPackage');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[2].id, 'testPackage');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[3].type, 'separator');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[4].id, 'configure_build');
  });

  it('can update a command shortcut', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('a_shortcut_cmd', 'Shortcut Command', '', 'Cmd+K', false, true);
    callback.updateMenus();
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 1, 'expected 1 menu item to start');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[0].accelerator, 'CommandOrControl+K');

    callback.setCommandShortcut('a_shortcut_cmd', 'Cmd+Shift+G');

    // setCommandShortcut calls this already but on a debounce timer so it's called immediately here for the test
    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[0].accelerator, 'CommandOrControl+Shift+G');
  });
});
