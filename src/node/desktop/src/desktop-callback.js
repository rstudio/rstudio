/*
 * desktop-callback.js
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

 const { ipcMain, dialog, BrowserWindow } = require('electron');

module.exports = class DesktopCallback {
  constructor(mainWindow, ownerWindow, isRemoteDesktop) {
    this.mainWindow = mainWindow;
    this.ownerWindow = ownerWindow;
    this.isRemoteDesktop = isRemoteDesktop;

    ipcMain.on('desktop_browse_url', (event, url) => {
      DesktopCallback.unimpl('desktop_browser_url');
    })

    ipcMain.handle('desktop_get_open_file_name', (event, caption, label, dir, filter, canChooseDirectories, focusOwner) => {
      DesktopCallback.unimpl('desktop_get_open_file_name');
      return '';
    })

    ipcMain.handle('desktop_get_save_file_name', (event, caption, label, dir, defaultExtension, forceDefaultExtension, focusOwner) => {
      DesktopCallback.unimpl('desktop_get_save_file_name');
      return '';
    })

    ipcMain.handle('desktop_get_existing_directory', (caption, label, dir, focusOwner) => {
      DesktopCallback.unimpl('desktop_get_existing_directory');
      return '';
    })

    ipcMain.on('desktop_on_clipboard_selection_changed', (event) => {
      DesktopCallback.unimpl('desktop_on_clipboard_selection_changed');
    })

    ipcMain.on('desktop_undo', (event) => {
      DesktopCallback.unimpl('desktop_undo');
    })

    ipcMain.on('desktop_redo', (event) => {
      DesktopCallback.unimpl('desktop_redo');
    })

    ipcMain.on('desktop_clipboard_cut', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_cut');
    })

    ipcMain.on('desktop_clipboard_copy', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_copy');
    })

    ipcMain.on('desktop_clipboard_paste', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_paste');
    })

    ipcMain.on('desktop_set_clipboard_text', (event, text) => {
      DesktopCallback.unimpl('desktop_set_clipboard_text');
    })

    ipcMain.handle('desktop_get_clipboard_text', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_text');
      return '';
    })

    ipcMain.handle('desktop_get_clipboard_uris', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_uris');
      return '';
    })

   ipcMain.handle('desktop_get_clipboard_image', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_image');
      return '';
    })

    ipcMain.on('desktop_set_global_mouse_selection', (event, selection) => {
      DesktopCallback.unimpl('desktop_set_global_mouse_selection');
    })

   ipcMain.handle('desktop_get_global_mouse_selection', (event) => {
      DesktopCallback.unimpl('desktop_get_global_mouse_selection');
      return '';
    })

   ipcMain.handle('desktop_get_cursor_position', (event) => {
      DesktopCallback.unimpl('desktop_get_cursor_position');
      return {x: 20, y: 20};
    })

    ipcMain.handle('desktop_get_display_dpi', (event, arg) => {
      console.log('warning: desktop_get_display_dpi returning hardcoded value of 72');
      return '72';
    })
  }

  static unimpl(ipcName) {
    dialog.showMessageBox(BrowserWindow.getFocusedWindow(), {
      title: 'Unimplemented',
      message: `${ipcName} callback NYI`
    });
    }
}
