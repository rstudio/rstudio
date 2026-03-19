/*
 * menu-callback.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { assert } from 'chai';
import { ipcMain, MenuItemConstructorOptions } from 'electron';
import { describe } from 'mocha';
import sinon from 'sinon';
import { MenuCallback } from '../../../src/main/menu-callback';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

const separatorTemplate: MenuItemConstructorOptions = { type: 'separator' };

describe('MenuCallback', () => {
  let callback: MenuCallback;

  beforeEach(() => {
    callback = new MenuCallback();
    setApplication(new Application());
  });

  afterEach(() => {
    // MenuCallback is really intended to be a singleton, but we create a new one for
    // each unit test. This causes listeners to accumulate on the underlying ipcMain
    // which eventually triggers a warning about potential leaks. We could up the limit,
    // but opting to cleanup after each test, instead.
    ipcMain.removeAllListeners();
    callback.debounceUpdateMenuShort.cancel();
    clearApplicationSingleton();
  });

  it('can be constructed', () => {
    const menuCount = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu
    callback.beginMain();
    callback.menuBegin('&File');

    callback.updateMenus(); // empty menu will be removed
    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can add a command', () => {
    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('a_new_command', 'Test Command', '', 'Cmd+Shift+T', false, false, true);
    callback.updateMenus();

    assert.isObject(callback.getMenuItemById('a_new_command'));
  });

  it('can set initial visibility for command', () => {
    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('an_invisible_command', 'Invisible Command', '', 'Cmd+Shift+I', false, false, false);
    callback.addCommand('a_visible_command', 'Visible Command', '', 'Cmd+Shift+V', false, false, true);
    callback.updateMenus();

    const invisibleCommand = callback.getMenuItemById('an_invisible_command');
    const visibleCommand = callback.getMenuItemById('a_visible_command');

    assert.isObject(invisibleCommand);
    assert.isObject(visibleCommand);
    assert.isFalse(invisibleCommand?.visible, 'expected menu item to be invisible');
    assert.isTrue(visibleCommand?.visible, 'expected menu item to be visible');
  });

  it('can change label for a command', () => {
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('a_command', 'Command', '', '', false, false, true);

    const command = callback.getMenuItemById('a_command');
    assert.isObject(command);
    assert.strictEqual(command?.label, 'Command');

    callback.setCommandLabel('a_command', 'New Label');
    callback.updateMenus();

    const updatedCommand = callback.mainMenu.getMenuItemById('a_command');
    assert.strictEqual(updatedCommand?.label, 'New Label');
    assert.isTrue(updatedCommand?.visible);
    assert.isFalse(updatedCommand.checked);
  });

  it('can change visibility for a command', () => {
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu
    const menuCount = process.platform === 'darwin' ? 2 : 1;

    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('a_command', 'Command', '', '', false, false, true);
    callback.addCommand('a_hidden_command', 'Initially hidden', '', '', false, false, false);
    callback.addCommand('a_visible_command', 'Initially visible', '', '', false, false, true);

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
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_command', 'Command', '', '', false, false, true); // expected
    callback.addToCurrentMenu(separatorTemplate); // expected
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('another_command', 'Another Command', '', '', false, false, true); // expected
    callback.addToCurrentMenu(separatorTemplate);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 3);
  });

  it('can remove a separator that is before a hidden item', () => {
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_command', 'Command', '', '', false, false, true); // expected
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('a_hidden_command', 'Hidden Command', '', '', false, false, false);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 1);
  });

  it('can contain a submenu', () => {
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu
    const menuCount = process.platform === 'darwin' ? 2 : 1;

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuBegin('Recent Files');

    callback.addCommand('mru0', '', '', '', false, false, false);
    callback.addCommand('mru1', '', '', '', false, false, false);
    callback.addCommand('mru2', '', '', '', false, false, false);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('clear_recent', 'Clear recent', '', '', false, false, true);

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
    const menuCount = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuEnd();

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');

    callback.beginMain();
    callback.menuBegin('&File');
    callback.menuBegin('Recent Files');
    callback.menuEnd();

    callback.addCommand('mru0', '', '', '', false, false, false);
    callback.addCommand('mru1', '', '', '', false, false, false);
    callback.addCommand('mru2', '', '', '', false, false, false);

    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items.length, menuCount, 'expected correct top level menu count');
  });

  it('can change a command visibility that causes unnecessary separators', () => {
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&Build');

    callback.addCommand('buildAll', 'Build All', '', '', false, false, true);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('buildSourcePackage', 'Build Source Package', '', '', false, false, false);
    callback.addCommand('buildBinaryPackage', 'Build Binary Package', '', '', false, false, false);
    callback.addCommand('testPackage', 'Test Package', '', '', false, false, false);
    callback.addToCurrentMenu(separatorTemplate);
    callback.addCommand('configure_build', 'Configure Build Tools', '', '', false, false, true);

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
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addCommand('a_shortcut_cmd', 'Shortcut Command', '', 'Cmd+K', false, false, true);
    callback.updateMenus();
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items.length, 1, 'expected 1 menu item to start');
    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[0].accelerator, 'CommandOrControl+K');

    callback.setCommandShortcut('a_shortcut_cmd', 'Cmd+Shift+G');

    // setCommandShortcut calls this already but on a debounce timer so it's called immediately here for the test
    callback.updateMenus();

    assert.strictEqual(callback.mainMenu.items[menuIdx].submenu?.items[0].accelerator, 'CommandOrControl+Shift+G');
  });

  it('does not rebuild menu for enabled change', () => {
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('test_cmd', 'Test', '', '', false, false, true);
    callback.updateMenus();

    const clock = sinon.useFakeTimers();
    const updateMenusSpy = sinon.spy(callback, 'updateMenus');
    try {
      callback.setCommandEnabled('test_cmd', false);
      clock.tick(100);

      assert.isTrue(updateMenusSpy.notCalled, 'updateMenus should not be called for enabled change');
      assert.isFalse(callback.mainMenu.getMenuItemById('test_cmd')?.enabled, 'live MenuItem should be disabled');
      assert.isFalse(callback.getMenuItemById('test_cmd')?.enabled, 'template should be updated');
    } finally {
      clock.restore();
      updateMenusSpy.restore();
    }
  });

  it('does not rebuild menu for checked change', () => {
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('check_cmd', 'Checkable', '', '', true, false, true);
    callback.updateMenus();

    const clock = sinon.useFakeTimers();
    const updateMenusSpy = sinon.spy(callback, 'updateMenus');
    try {
      callback.setCommandChecked('check_cmd', true);
      clock.tick(100);

      assert.isTrue(updateMenusSpy.notCalled, 'updateMenus should not be called for checked change');
      assert.isTrue(callback.mainMenu.getMenuItemById('check_cmd')?.checked, 'live MenuItem should be checked');
      assert.isTrue(callback.getMenuItemById('check_cmd')?.checked, 'template should be updated');
    } finally {
      clock.restore();
      updateMenusSpy.restore();
    }
  });

  it('does not rebuild menu for label change', () => {
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('label_cmd', 'Old Label', '', '', false, false, true);
    callback.updateMenus();

    const clock = sinon.useFakeTimers();
    const updateMenusSpy = sinon.spy(callback, 'updateMenus');
    try {
      callback.setCommandLabel('label_cmd', 'New Label');
      clock.tick(100);

      assert.isTrue(updateMenusSpy.notCalled, 'updateMenus should not be called for label change');
      assert.strictEqual(callback.mainMenu.getMenuItemById('label_cmd')?.label, 'New Label');
      assert.strictEqual(callback.getMenuItemById('label_cmd')?.label, 'New Label');
    } finally {
      clock.restore();
      updateMenusSpy.restore();
    }
  });

  it('still rebuilds menu for visibility change', () => {
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('vis_cmd', 'Visible', '', '', false, false, true);
    callback.updateMenus();

    const clock = sinon.useFakeTimers();
    const updateMenusSpy = sinon.spy(callback, 'updateMenus');
    try {
      callback.setCommandVisibility('vis_cmd', false);
      clock.tick(100);

      assert.isTrue(updateMenusSpy.calledOnce, 'updateMenus should be called for visibility change');
    } finally {
      clock.restore();
      updateMenusSpy.restore();
    }
  });

  it('preserves in-place changes through modal state', () => {
    callback.beginMain();
    callback.menuBegin('&Edit');
    callback.addCommand('cutDummy', 'Cut', '', 'Cmd+C', false, false, true);
    callback.addCommand('modal_cmd', 'Modal Test', '', '', true, false, true);
    callback.updateMenus();

    // Enter modal state
    appState().modalTracker.setNumGwtModalsShowing(1);
    callback.setMainMenuEnabled(false);

    // Make in-place changes while modal is open
    callback.setCommandEnabled('modal_cmd', false);
    callback.setCommandChecked('modal_cmd', true);
    callback.setCommandLabel('modal_cmd', 'Updated During Modal');

    // Exit modal state — restores savedMenu
    appState().modalTracker.setNumGwtModalsShowing(0);
    callback.setMainMenuEnabled(true);

    // Verify restored menu reflects changes made during modal
    const item = callback.mainMenu.getMenuItemById('modal_cmd');
    assert.isNotNull(item, 'expected modal_cmd to exist in restored menu');
    assert.isFalse(item!.enabled, 'enabled should reflect change made during modal');
    assert.isTrue(item!.checked, 'checked should reflect change made during modal');
    assert.strictEqual(item!.label, 'Updated During Modal', 'label should reflect change made during modal');
  });

  it('does not re-enable commands on the disabled menu during a modal', () => {
    callback.beginMain();
    callback.menuBegin('&Edit');
    callback.addCommand('cutDummy', 'Cut', '', 'Cmd+C', false, false, true);
    callback.addCommand('guarded_cmd', 'Guarded', '', 'Cmd+G', false, false, true);
    callback.updateMenus();

    // Verify the command starts enabled
    assert.isTrue(callback.mainMenu.getMenuItemById('guarded_cmd')?.enabled);

    // Enter modal state — builds a disabled copy of the menu
    appState().modalTracker.setNumGwtModalsShowing(1);
    callback.setMainMenuEnabled(false);
    assert.isFalse(
      callback.mainMenu.getMenuItemById('guarded_cmd')?.enabled,
      'expected command to be disabled by modal',
    );

    // Backend fires setCommandEnabled(true) while modal is open
    callback.setCommandEnabled('guarded_cmd', true);

    // The visible (disabled) menu must NOT have the command re-enabled
    assert.isFalse(
      callback.mainMenu.getMenuItemById('guarded_cmd')?.enabled,
      'command must stay disabled on the modal menu',
    );

    // Close modal — restores savedMenu which should have the update
    appState().modalTracker.setNumGwtModalsShowing(0);
    callback.setMainMenuEnabled(true);
    assert.isTrue(
      callback.mainMenu.getMenuItemById('guarded_cmd')?.enabled,
      'command should be enabled after modal closes',
    );
  });

  it('can disable and enable application menu', () => {
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&Edit');
    callback.addCommand('cutDummy', 'Cut', '', 'Cmd+C', false, false, true);
    callback.addCommand('a_shortcut_cmd', 'Shortcut Command', '', 'Cmd+K', false, false, true);
    callback.updateMenus();

    assert.isTrue(
      callback.mainMenu.items[menuIdx].submenu?.items[0].enabled,
      'expected cut action to be enabled by default',
    );
    assert.isTrue(
      callback.mainMenu.items[menuIdx].submenu.items[1].enabled,
      'expected shortcut action to be enabled by default',
    );

    appState().modalTracker.setNumGwtModalsShowing(1);
    callback.setMainMenuEnabled(false);
    callback.updateMenus();

    assert.isTrue(callback.mainMenu.items[menuIdx].submenu.items[0].enabled, 'expected cut action to be enabled');
    assert.isFalse(
      callback.mainMenu.items[menuIdx].submenu.items[1].enabled,
      'expected shortcut action to be disabled',
    );

    appState().modalTracker.setNumGwtModalsShowing(0);
    callback.setMainMenuEnabled(true);
    callback.updateMenus();

    assert.isTrue(callback.mainMenu.items[menuIdx].submenu.items[0].enabled, 'expected cut action to be enabled');
    assert.isTrue(callback.mainMenu.items[menuIdx].submenu.items[1].enabled, 'expected shortcut action to be enabled');
  });
});
