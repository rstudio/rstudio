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
const { ipcMain } = require('electron');

module.exports = class MenuCallback {
  constructor() {

    ipcMain.on('menu_begin_main', (event) => {
    });

    ipcMain.on('menu_begin', (event, label) => {
    });

    ipcMain.on('menu_add_command', (event, cmdId, label, tooltip, shortcut, isChecked) => {

    });

    ipcMain.on('menu_add_separator', (event) => {

    });

    ipcMain.on('menu_end', (event) => {

    });

    ipcMain.on('menu_end_main', (event) => {

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
