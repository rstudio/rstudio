/*
 * desktop-callback.ts
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

import { ipcMain, dialog, BrowserWindow } from 'electron';

import PendingWindow from './pending-window';
import MainWindow from './main-window';
import { MessageBoxOptions, OpenDialogOptions } from 'electron/main';

export const PendingQuit = {
  'PendingQuitNone': 0,
  'PendingQuitAndExit': 1,
  'PendingQuitAndRestart': 2,
  'PendingQuitRestartAndReload': 3
};

export class DesktopCallback {
  pendingQuit: number = PendingQuit.PendingQuitNone;

  constructor(public mainWindow: MainWindow,
              public ownerWindow: MainWindow,
              public isRemoteDesktop: boolean) {

    ipcMain.on('desktop_browse_url', (event, url) => {
      DesktopCallback.unimpl('desktop_browser_url');
    });

    ipcMain.handle('desktop_get_open_file_name', (event,
                                                  caption,
                                                  label,
                                                  dir,
                                                  filter,
                                                  canChooseDirectories,
                                                  focusOwner) => {

      // TODO: apply filter
      const openDialogOptions: OpenDialogOptions =
      {
        properties: [canChooseDirectories ? 'openDirectory' : 'openFile'],
        title: caption,
        defaultPath: dir,
        buttonLabel: label,
      };

      const focusedWindow = BrowserWindow.getFocusedWindow();
      if (focusedWindow) {
        return dialog.showOpenDialog(focusedWindow, openDialogOptions);
      } else {
        return dialog.showOpenDialog(openDialogOptions);
      }
    });

    ipcMain.handle('desktop_get_save_file_name', (event,
                                                  caption,
                                                  label,
                                                  dir,
                                                  defaultExtension,
                                                  forceDefaultExtension,
                                                  focusOwner) => {
      DesktopCallback.unimpl('desktop_get_save_file_name');
      return '';
    });

    ipcMain.handle('desktop_get_existing_directory', (caption, label, dir, focusOwner) => {
      DesktopCallback.unimpl('desktop_get_existing_directory');
      return '';
    });

    ipcMain.on('desktop_on_clipboard_selection_changed', (event) => {
      DesktopCallback.unimpl('desktop_on_clipboard_selection_changed');
    });

    ipcMain.on('desktop_undo', (event) => {
      DesktopCallback.unimpl('desktop_undo');
    });

    ipcMain.on('desktop_redo', (event) => {
      DesktopCallback.unimpl('desktop_redo');
    });

    ipcMain.on('desktop_clipboard_cut', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_cut');
    });

    ipcMain.on('desktop_clipboard_copy', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_copy');
    });

    ipcMain.on('desktop_clipboard_paste', (event) => {
      DesktopCallback.unimpl('desktop_clipboard_paste');
    });

    ipcMain.on('desktop_set_clipboard_text', (event, text) => {
      DesktopCallback.unimpl('desktop_set_clipboard_text');
    });

    ipcMain.handle('desktop_get_clipboard_text', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_text');
      return '';
    });

    ipcMain.handle('desktop_get_clipboard_uris', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_uris');
      return '';
    });

   ipcMain.handle('desktop_get_clipboard_image', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_image');
      return '';
    });

    ipcMain.on('desktop_set_global_mouse_selection', (event, selection) => {
      DesktopCallback.unimpl('desktop_set_global_mouse_selection');
    });

   ipcMain.handle('desktop_get_global_mouse_selection', (event) => {
      DesktopCallback.unimpl('desktop_get_global_mouse_selection');
      return '';
    });

   ipcMain.handle('desktop_get_cursor_position', (event) => {
      DesktopCallback.unimpl('desktop_get_cursor_position');
      return {x: 20, y: 20};
    });

    ipcMain.handle('desktop_does_window_exist_at_cursor_position', (event) => {
      return false;
    });

    ipcMain.on('desktop_on_workbench_initialized', (event, scratchPath) => {
      this.mainWindow.onWorkbenchInitialized();
    });

    ipcMain.on('desktop_show_folder', (event, path) => {
      DesktopCallback.unimpl('desktop_show_folder');
    });

    ipcMain.on('desktop_show_file', (event, file) => {
      DesktopCallback.unimpl('desktop_show_file');
    });

    ipcMain.on('desktop_show_word_doc', (event, wordDoc) => {
      DesktopCallback.unimpl('desktop_show_word_doc');
    });

    ipcMain.on('desktop_show_ppt_presentation', (event, pptDoc) => {
      DesktopCallback.unimpl('desktop_show_ppt_presentation');
    });

    ipcMain.on('desktop_show_pdf', (event, path, pdfPage) => {
      DesktopCallback.unimpl('desktop_show_pdf');
    });

    ipcMain.on('desktop_prepare_show_word_doc', (event) => {
      DesktopCallback.unimpl('desktop_prepare_show_word_doc');
    });

    ipcMain.on('desktop_prepare_show_ppt_presentation', (event) => {
      DesktopCallback.unimpl('desktop_prepare_show_ppt_presentation');
    });

    ipcMain.handle('desktop_get_r_version', (event) => {
      return '';
    });

    ipcMain.handle('desktop_choose_r_version', (event) => {
      DesktopCallback.unimpl('desktop_choose_r_version');
      return '';
    });

    ipcMain.handle('desktop_device_pixel_ratio', (event) => {
      DesktopCallback.unimpl('desktop_device_pixel_ratio');
      return 1.0;
    });

    ipcMain.on('desktop_open_minimal_window', (event, name, url, width, height) => {
      DesktopCallback.unimpl('desktop_open_minimal_window');
    });

    ipcMain.on('desktop_activate_minimal_window', (event, name) => {
      DesktopCallback.unimpl('desktop_activate_minimal_window');
    });

    ipcMain.on('desktop_activate_satellite_window', (event, name) => {
      console.log(`activate_satellite_window ${name}`);
    });

    ipcMain.handle('desktop_prepare_for_satellite_window', (event, name, x, y, width, height) => {
      this.mainWindow.prepareForWindow(new PendingWindow(name, x, y, width, height));
    });

    ipcMain.handle('desktop_prepare_for_named_window', (event, name, allowExternalNavigate, showToolbar) => {
      console.log(`prepare_for_named_window ${name}`);
    });

    ipcMain.on('desktop_close_named_window', (event, name) => {
      DesktopCallback.unimpl('desktop_close_named_window');
    });

    ipcMain.on('desktop_copy_page_region_to_clipboard', (event, left, top, width, height) => {
      DesktopCallback.unimpl('desktop_copy_page_region_to_clipboard');
    });

    ipcMain.on('desktop_export_page_region_to_file', (event, targetPath, format, left, top, width, height) => {
      DesktopCallback.unimpl('desktop_export_page_region_to_file');
    });

    ipcMain.on('desktop_print_text', (event, text) => {
      DesktopCallback.unimpl('desktop_print_text');
    });

    ipcMain.on('desktop_paint_print_text', (event, printer) => {
      DesktopCallback.unimpl('desktop_paint_print_text');
    });

    ipcMain.on('desktop_print_finished', (event, result) => {
      DesktopCallback.unimpl('desktop_print_finished');
    });

    ipcMain.handle('desktop_supports_clipboard_metafile', (event) => {
      return false;
    });

    ipcMain.handle('desktop_show_message_box', (event, type, caption, message, buttons, defaultButton, cancelButton) => {
      const openDialogOptions: MessageBoxOptions =
      {
        type: this.convertMessageBoxType(type),
        title: caption,
        message: message,
        buttons: this.convertButtons(buttons),
      };

      const focusedWindow = BrowserWindow.getFocusedWindow();
      if (focusedWindow) {
        return dialog.showMessageBox(focusedWindow, openDialogOptions);
      } else {
        return dialog.showMessageBox(openDialogOptions);
      }
    });

    ipcMain.handle('desktop_prompt_for_text', (event, title, caption, defaultValue, type, 
                                               rememberPasswordPrompt, rememberByDefault,
                                               selectionStart, selectionLength, okButtonCaption) => {
      DesktopCallback.unimpl('desktop_prompt_for_text');
      return ''; 
    });

    ipcMain.on('desktop_bring_main_frame_to_front', (event) => {
    });

    ipcMain.on('desktop_bring_main_frame_behind_active', (event) => {
      DesktopCallback.unimpl('desktop_bring_main_frame_behind_active');
    });

    ipcMain.handle('desktop_rendering_engine', (event) => {
      return '';
    });

    ipcMain.on('desktop_set_desktop_rendering_engine', (event, engine) => {
      DesktopCallback.unimpl('desktop_set_desktop_rendering_engine');
    });

    ipcMain.handle('desktop_filter_text', (event, text) => {
      DesktopCallback.unimpl('desktop_filter_text');
      return text;
    });

    ipcMain.on('desktop_clean_clipboard', (event, stripHtml) => {
      DesktopCallback.unimpl('desktop_clean_clipboard');
    });

    ipcMain.on('desktop_set_pending_quit', (event, pendingQuit) => {
      this.pendingQuit = pendingQuit;
    });

    ipcMain.on('desktop_open_project_in_new_window', (event, projectFilePath) => {
      DesktopCallback.unimpl('desktop_open_project_in_new_window');
    });

    ipcMain.on('desktop_open_session_in_new_window', (event, workingDirectoryPath) => {
      DesktopCallback.unimpl('desktop_open_session_in_new_window');
    });

   ipcMain.on('desktop_open_terminal', (event, terminalPath, workingDirectory, extraPathEntries, shellType) => {
      DesktopCallback.unimpl('desktop_open_terminal');
    });

    ipcMain.handle('desktop_get_fixed_width_font_list', (event) => {
      DesktopCallback.unimpl('desktop_get_fixed_width_font_list');
      return '';
    });

    ipcMain.handle('desktop_get_fixed_width_font', (event) => {
      DesktopCallback.unimpl('desktop_get_fixed_width_font');
      return '';
    });

    ipcMain.on('desktop_set_fixed_width_font', (event, font) => {
      DesktopCallback.unimpl('desktop_set_fixed_width_font');
    });

    ipcMain.handle('desktop_get_zoom_levels', (event) => {
      DesktopCallback.unimpl('desktop_get_zoom_levels');
      return '';
    });

    ipcMain.handle('desktop_get_zoom_level', (event) => {
      DesktopCallback.unimpl('desktop_get_zoom_level');
      return 1.0;
    });

    ipcMain.on('desktop_set_zoom_level', (event, zoomLevel) => {
      DesktopCallback.unimpl('desktop_set_zoom_level');
    });

    ipcMain.on('desktop_zoom_in', (event) => {
      DesktopCallback.unimpl('desktop_zoom_in');
    });

    ipcMain.on('desktop_zoom_out', (event) => {
      DesktopCallback.unimpl('desktop_zoom_out');
    });

    ipcMain.on('desktop_zoom_actual_size', (event) => {
      DesktopCallback.unimpl('desktop_zoom_actual_size');
    });

    ipcMain.on('desktop_set_background_color', (event, rgbColor) => {
    });

    ipcMain.on('desktop_change_title_bar_color', (event, red, green, blue) => {
    });

    ipcMain.on('desktop_sync_to_editor_theme', (event, isDark) => {
    });

    ipcMain.handle('desktop_get_enable_accessibility', (event) => {
      DesktopCallback.unimpl('desktop_get_enable_accessibility');
      return true;
    });

    ipcMain.on('desktop_set_enable_accessibility', (event, enable) => {
      DesktopCallback.unimpl('desktop_set_enable_accessibility');
    });

    ipcMain.handle('desktop_get_clipboard_monitoring', (event) => {
      DesktopCallback.unimpl('desktop_get_clipboard_monitoring');
      return false;
    });

    ipcMain.on('desktop_set_clipboard_monitoring', (event, monitoring) => {
      DesktopCallback.unimpl('desktop_set_clipboard_monitoring');
    });

    ipcMain.handle('desktop_get_ignore_gpu_blacklist', (event, ignore) => {
      return true;
    });

    ipcMain.on('desktop_set_ignore_gpu_blacklist', (event, ignore) => {
      DesktopCallback.unimpl('desktop_set_ignore_gpu_blacklist');
    });

    ipcMain.handle('desktop_get_disable_gpu_driver_bug_workarounds', (event) => {
      return false;
    });

    ipcMain.on('desktop_set_disable_gpu_driver_bug_workarounds', (event, disable) => {
      DesktopCallback.unimpl('desktop_set_disable_gpu_driver_bug_workarounds');
    });

    ipcMain.on('desktop_show_license_dialog', (event) => {
      DesktopCallback.unimpl('desktop_show_license_dialog');
    });

    ipcMain.on('desktop_show_session_server_options_dialog', (event) => {
      DesktopCallback.unimpl('desktop_show_session_server_options_dialog');
    });

    ipcMain.handle('desktop_get_init_messages', (event) => {
      return '';
    });

    ipcMain.handle('desktop_get_license_status_message', (event) => {
      DesktopCallback.unimpl('desktop_get_license_status_messages');
      return '';
    });

    ipcMain.handle('desktop_allow_product_usage', (event) => {
      DesktopCallback.unimpl('desktop_allow_product_usage');
      return true;
    });

    ipcMain.handle('desktop_get_desktop_synctex_viewer', (event) => {
      DesktopCallback.unimpl('desktop_get_desktop_synctex_viewer');
      return '';
    });

    ipcMain.on('desktop_external_synctex_preview', (event, pdfPath, page) => {
      DesktopCallback.unimpl('desktop_external_synctex_preview');
    });

    ipcMain.on('desktop_external_synctex_view', (event, pdfFile, srcFile, line, column) => {
      DesktopCallback.unimpl('desktop_external_synctex_view');
    });

    ipcMain.handle('desktop_supports_fullscreen_mode', (event) => {
      DesktopCallback.unimpl('desktop_supports_fullscreen_mode');
      return true;
    });

    ipcMain.on('desktop_toggle_fullscreen_mode', (event) => {
      DesktopCallback.unimpl('desktop_toggle_fullscreen_mode');
    });

    ipcMain.on('desktop_show_keyboard_shortcut_help', (event) => {
      DesktopCallback.unimpl('desktop_show_keyboard_shortcut_help');
    });

    ipcMain.on('desktop_launch_session', (event, reload) => {
      DesktopCallback.unimpl('desktop_launch)_session');
    });

    ipcMain.on('desktop_reload_zoom_window', (event) => {
    });

    ipcMain.on('desktop_set_tutorial_url', (event, url) => {
      DesktopCallback.unimpl('desktop_set_tutorial_url');
    });
  
    ipcMain.on('desktop_set_viewer_url', (event, url) => {
      DesktopCallback.unimpl('desktop_set_viewer_url');
    });

    ipcMain.on('desktop_reload_viewer_zoom_window', (event, url) => {
      DesktopCallback.unimpl('desktop_reload_viewer_zoom_window');
    });

    ipcMain.on('desktop_set_shiny_dialog_url', (event, url) => {
      DesktopCallback.unimpl('desktop_set_shiny_dialog_url');
    });

    ipcMain.handle('desktop_get_scrolling_compensation_type', (event) => {
      DesktopCallback.unimpl('desktop_get_scrolling_compensation_type');
      return '';
    });

    ipcMain.handle('desktop_is_macos', (event) => {
      return true;
    });

    ipcMain.handle('desktop_is_centos', (event) => {
      return false;
    });

    ipcMain.on('desktop_set_busy', (event, busy) => {
    });

    ipcMain.on('desktop_set_window_title', (event, title) => {
      this.mainWindow.window?.setTitle(`${title} - RStudio`);
    });

    ipcMain.on('desktop_install_rtools', (event, version, installerPath) => {
      DesktopCallback.unimpl('desktop_install_rtools');
    });

    ipcMain.handle('desktop_get_display_dpi', (event) => {
      return '72';
    });

    ipcMain.on('desktop_on_session_quit', (event) => {
      DesktopCallback.unimpl('desktop_on_session_quit');
    });

    ipcMain.handle('desktop_get_session_server', (event) => {
      DesktopCallback.unimpl('desktop_get_session_server');
      return {};
    });

    ipcMain.handle('desktop_get_session_servers', (event) => {
      return [];
    });

    ipcMain.on('desktop_reconnect_to_session_server', (event, sessionServerJson) => {
      DesktopCallback.unimpl('desktop_reconnect_to_session_server');
    });

    ipcMain.handle('desktop_set_launcher_server', (event, sessionServerJson) => {
      DesktopCallback.unimpl('desktop_set_launcher_server');
      return false;
    });

    ipcMain.on('desktop_connect_to_launcher_server', (event) => {
      DesktopCallback.unimpl('desktop_connect_to_launcher_server');
    });

    ipcMain.handle('desktop_get_launcher_server', (event) => {
      DesktopCallback.unimpl('desktop_get_launcher_server');
      return {};
    });

    ipcMain.on('desktop_start_launcher_job_status_stream', (event, jobId) => {
      DesktopCallback.unimpl('desktop_start_launcher_job_status_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_status_stream', (event, jobId) => {
      DesktopCallback.unimpl('desktop_stop_launcher_job_status_stream');
    });

    ipcMain.on('desktop_start_launcher_job_output_stream', (event, jobId) => {
      DesktopCallback.unimpl('desktop_start_launcher_job_output_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_output_stream', (event, jobId) => {
      DesktopCallback.unimpl('desktop_stop_launcher_job_output_stream');
    });

    ipcMain.on('desktop_control_launcher_job', (event, jobId, operation) => {
      DesktopCallback.unimpl('desktop_control_launcher_job');
    });

    ipcMain.on('desktop_submit_launcher_job', (event, job) => {
      DesktopCallback.unimpl('desktop_submit_launcher_job');
    });

    ipcMain.on('desktop_get_job_container_user', (event) => {
      DesktopCallback.unimpl('desktop_get_job_container_user');
    });

    ipcMain.on('desktop_validate_jobs_config', (event) => {
      DesktopCallback.unimpl('desktop_validate_jobs_config');
    });

    ipcMain.handle('desktop_get_proxy_port_number', (event) => {
      DesktopCallback.unimpl('desktop_get_proxy_port_number');
      return -1;
    });
  }

  static unimpl(ipcName: string) {

    const focusedWindow = BrowserWindow.getFocusedWindow();
    if (focusedWindow) {
      dialog.showMessageBox(focusedWindow, {
        title: 'Unimplemented',
        message: `${ipcName} callback NYI`
      });
    } else {
      dialog.showMessageBox({
        title: 'Unimplemented',
        message: `${ipcName} callback NYI`
      });
    }
  }

  collectPendingQuitRequest() {
    if (this.pendingQuit != PendingQuit.PendingQuitNone) {
      let currentPendingQuit = this.pendingQuit;
      this.pendingQuit = PendingQuit.PendingQuitNone;
      return currentPendingQuit;
    } else {
      return PendingQuit.PendingQuitNone;
    }
  }

  convertMessageBoxType(type: number) {
    // map QMessageBox types to Electron values
    switch (type) {
      case 1:
        return 'info';
      case 2:
        return 'warning';
      case 3:
        return 'error';
      default:
      case 4:
        return 'question';
    }
  }

  convertButtons(buttons: string) {
    return buttons.split('|');
  }
}

module.exports = {PendingQuit, DesktopCallback};
