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

    ipcMain.handle('desktop_get_open_file_name', (event,
                                                  caption,
                                                  label,
                                                  dir,
                                                  filter,
                                                  canChooseDirectories,
                                                  focusOwner) => {
      DesktopCallback.unimpl('desktop_get_open_file_name');
      return '';
    })

    ipcMain.handle('desktop_get_save_file_name', (event,
                                                  caption,
                                                  label,
                                                  dir,
                                                  defaultExtension,
                                                  forceDefaultExtension,
                                                  focusOwner) => {
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

    ipcMain.handle('desktop_does_window_exist_at_cursor_position', (event) => {
      return false;
    })

    ipcMain.on('desktop_on_workbench_initialized', (event, scratchPath) => {
    })

    ipcMain.on('desktop_show_folder', (event, path) => {
      DesktopCallback.unimpl('desktop_show_folder');
    })

    ipcMain.on('desktop_show_file', (event, file) => {
      DesktopCallback.unimpl('desktop_show_file');
    })

    ipcMain.on('desktop_show_word_doc', (event, wordDoc) => {
      DesktopCallback.unimpl('desktop_show_word_doc');
    })

    ipcMain.on('desktop_show_ppt_presentation', (event, pptDoc) => {
      DesktopCallback.unimpl('desktop_show_ppt_presentation');
    })

    ipcMain.on('desktop_show_pdf', (event, path, pdfPage) => {
      DesktopCallback.unimpl('desktop_show_pdf');
    })

    ipcMain.on('desktop_prepare_show_word_doc', (event) => {
      DesktopCallback.unimpl('desktop_prepare_show_word_doc');
    })

    ipcMain.on('desktop_prepare_show_ppt_presentation', (event) => {
      DesktopCallback.unimpl('desktop_prepare_show_ppt_presentation');
    })

    ipcMain.handle('desktop_get_r_version', (event) => {
      return '';
    })

    ipcMain.handle('desktop_choose_r_version', (event) => {
      DesktopCallback.unimpl('desktop_choose_r_version');
      return '';
    })

    ipcMain.handle('desktop_device_pixel_ratio', (event) => {
      DesktopCallback.unimpl('desktop_device_pixel_ratio');
      return 1.0;
    })

    ipcMain.on('desktop_open_minimal_window', (event, name, url, width, height) => {
      DesktopCallback.unimpl('desktop_open_minimal_window');
    })

    ipcMain.on('desktop_activate_minimal_window', (event, name) => {
      DesktopCallback.unimpl('desktop_activate_minimal_window');
    })

    ipcMain.on('desktop_activate_satellite_window', (event, name) => {
      DesktopCallback.unimpl('desktop_activate_satellite_window');
    })

    ipcMain.on('desktop_prepare_for_satellite_window', (event, name, x, y, width, height) => {
      DesktopCallback.unimpl('desktop_prepare_for_satellite_window');
    })

    ipcMain.on('desktop_prepare_for_named_window', (event, name, allowExternalNavigate, showToolbar) => {
      DesktopCallback.unimpl('desktop_prepare_for_named_window');
    })

    ipcMain.on('desktop_close_named_window', (event, name) => {
      DesktopCallback.unimpl('desktop_close_named_window');
    })

    ipcMain.on('desktop_copy_page_region_to_clipboard', (event, left, top, width, height) => {
      DesktopCallback.unimpl('desktop_copy_page_region_to_clipboard');
    })

    ipcMain.on('desktop_export_page_region_to_file', (event, targetPath, format, left, top, width, height) => {
      DesktopCallback.unimpl('desktop_export_page_region_to_file');
    })

    ipcMain.on('desktop_print_text', (event, text) => {
      DesktopCallback.unimpl('desktop_print_text');
    })

    ipcMain.on('desktop_paint_print_text', (event, printer) => {
      DesktopCallback.unimpl('desktop_paint_print_text');
    })

    ipcMain.on('desktop_print_finished', (event, result) => {
      DesktopCallback.unimpl('desktop_print_finished');
    })

    ipcMain.handle('desktop_supports_clipboard_metafile', (event) => {
      return false;
    })

    ipcMain.handle('desktop_show_message_box', (event, type, caption, message, buttons, defaultButton, cancelButton) => {
      DesktopCallback.unimpl('desktop_show_message_box');
      return 1.0;
    })

    ipcMain.handle('desktop_prompt_for_text', (event, title, caption, defaultValue, type, 
                                               rememberPasswordPrompt, rememberByDefault,
                                               selectionStart, selectionLength, okButtonCaption) => {
      DesktopCallback.unimpl('desktop_prompt_for_text');
      return ''; 
    })

    ipcMain.on('desktop_bring_main_frame_to_front', (event) => {
      DesktopCallback.unimpl('desktop_bring_main_frame_to_front');
    })

    ipcMain.on('desktop_bring_main_frame_behind_active', (event) => {
      DesktopCallback.unimpl('desktop_bring_main_frame_behind_active');
    })

    ipcMain.handle('desktop_rendering_engine', (event) => {
      DesktopCallback.unimpl('desktop_rendering_engine');
      return '';
    })

    ipcMain.on('desktop_set_desktop_rendering_engine', (event, engine) => {
      DesktopCallback.unimpl('desktop_set_desktop_rendering_engine');
    })

    ipcMain.handle('desktop_filter_text', (event, text) => {
      DesktopCallback.unimpl('desktop_filter_text');
      return text;
    })


    ipcMain.handle('desktop_get_display_dpi', (event, arg) => {
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
