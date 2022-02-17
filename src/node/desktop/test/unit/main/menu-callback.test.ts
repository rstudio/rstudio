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
import { MenuItem } from 'electron';
import { describe } from 'mocha';
import { MenuCallback } from '../../../src/main/menu-callback';

describe('MenuCallback', () => {
  it('can be constructed', () => {
    const callback = new MenuCallback();
    assert.isObject(callback);
  });

  it('can add a command', () => {
    const callback = new MenuCallback();
    callback.addCommand('a_new_command', 'Test Command', '', 'Cmd+Shift+T', false, true);
    assert.isObject(callback.getMenuItemById('a_new_command'));
  });

  it('can set initial visibility for command', () => {
    const callback = new MenuCallback();

    callback.addCommand('an_invisible_command', 'Invisible Command', '', 'Cmd+Shift+I', false, false);
    callback.addCommand('a_visible_command', 'Visible Command', '', 'Cmd+Shift+V', false, true);

    const invisibleCommand = callback.getMenuItemById('an_invisible_command');
    const visibleCommand = callback.getMenuItemById('a_visible_command');

    assert.isObject(invisibleCommand);
    assert.isObject(visibleCommand);
    assert.isFalse(invisibleCommand?.visible, 'expected menu item to be invisible');
    assert.isTrue(visibleCommand?.visible, 'expected menu item to be visible');
  });

  it('can update command', () => {
    const callback = new MenuCallback();
    callback.beginMain();
    callback.menuBegin('&File');
    callback.addCommand('a_command', 'Command', '', '', false, true);

    const command = callback.getMenuItemById('a_command');
    assert.isObject(command);
    assert.strictEqual(command?.label, 'Command');

    callback.updateMenus([{ id: 'a_command', label: 'New Label', visible: false, checked: true }]);

    const updatedCommand = callback.getMenuItemById('a_command');
    assert.strictEqual(updatedCommand?.label, 'New Label');
    assert.isFalse(updatedCommand?.visible);
    assert.isTrue(updatedCommand?.checked);
  });

  it('can remove unnecessary separators', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(new MenuItem({ type: 'separator' }));
    callback.addCommand('a_command', 'Command', '', '', false, true); // expected
    callback.addToCurrentMenu(new MenuItem({ type: 'separator' })); // expected
    callback.addToCurrentMenu(new MenuItem({ type: 'separator' }));
    callback.addCommand('another_command', 'Another Command', '', '', false, true); // expected
    callback.addToCurrentMenu(new MenuItem({ type: 'separator' }));

    callback.updateMenus([]);

    assert.strictEqual(callback.mainMenu?.items[menuIdx].submenu?.items.length, 3);
  });

  it('can remove a separator that is before a hidden item', () => {
    const callback = new MenuCallback();
    const menuIdx = process.platform === 'darwin' ? 1 : 0; // adjust for MacOS app menu

    callback.beginMain();
    callback.menuBegin('&File');

    callback.addToCurrentMenu(new MenuItem({ type: 'separator' }));
    callback.addCommand('a_command', 'Command', '', '', false, true); // expected
    callback.addToCurrentMenu(new MenuItem({ type: 'separator' }));
    callback.addCommand('a_hidden_command', 'Hidden Command', '', '', false, false);

    callback.updateMenus([]);

    assert.strictEqual(callback.mainMenu?.items[menuIdx].submenu?.items.length, 1);
  });
});
