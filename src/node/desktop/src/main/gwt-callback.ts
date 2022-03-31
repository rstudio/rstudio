/*
 * gwt-callback.ts
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

// TODO clean this up

import {
  app,
  BrowserWindow,
  clipboard,
  dialog,
  ipcMain,
  Rectangle,
  screen,
  shell,
  webContents,
  webFrameMain,
} from 'electron';
import { IpcMainEvent, MessageBoxOptions, OpenDialogOptions, SaveDialogOptions } from 'electron/main';
import EventEmitter from 'events';
import { mkdtempSync, writeFileSync } from 'fs';
import i18next from 'i18next';
import { findFontsSync } from 'node-system-fonts';
import path from 'path';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { isCentOS } from '../core/system';
import { resolveTemplateVar } from '../core/template-filter';
import desktop from '../native/desktop.node';
import { appState } from './app-state';
import { GwtWindow } from './gwt-window';
import { MainWindow } from './main-window';
import { openMinimalWindow } from './minimal-window';
import { defaultFonts, ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { filterFromQFileDialogFilter, resolveAliasedPath } from './utils';
import { activateWindow } from './window-utils';

export enum PendingQuit {
  PendingQuitNone,
  PendingQuitAndExit,
  PendingQuitAndRestart,
  PendingQuitRestartAndReload,
}

// The documentation for getFocusedWebContents() has:
//
// /**
//  * The web contents that is focused in this application, otherwise returns `null`.
//  */
//
//  static getFocusedWebContents(): WebContents;
//
// and so the documentation appears to state that the return value may be 'null',
// but the type definition doesn't propagate that reality. Hence, this wrapper function.
function focusedWebContents(): Electron.WebContents | null {
  return webContents.getFocusedWebContents();
}

/**
 * This is the main-process side of the GwtCallbacks; dispatched from renderer processes
 * via the ContextBridge.
 */
export class GwtCallback extends EventEmitter {
  static WORKBENCH_INITIALIZED = 'gwt-callback-workbench_initialized';
  static SESSION_QUIT = 'gwt-callback-session_quit';

  pendingQuit: number = PendingQuit.PendingQuitNone;
  private owners = new Set<GwtWindow>();

  // Info used by the "session failed to load" error page (error.html)
  errorPageData = new Map<string, string>();

  // https://github.com/foliojs/font-manager/issues/15
  // the fork did not correct usage of Fontconfig
  // getAvailableFontsSync() incorrectly sets the monospace property
  monospaceFonts = [
    ...new Set<string>(
      findFontsSync({ monospace: true }).map((fd) => {
        return process.platform === 'darwin' ? fd.postscriptName : fd.family;
      }),
    ),
  ].sort((a, b) => a.localeCompare(b));
  proportionalFonts = [...new Set<string>(findFontsSync({ monospace: false }).map((fd) => fd.family))].sort((a, b) =>
    a.localeCompare(b),
  );

  constructor(public mainWindow: MainWindow, public isRemoteDesktop: boolean) {
    super();
    this.owners.add(mainWindow);

    ipcMain.on('desktop_browse_url', (event, url: string) => {
      // TODO: review if we need additional validation of URL
      void shell.openExternal(url);
    });

    ipcMain.handle(
      'desktop_get_open_file_name',
      async (
        event,
        caption: string,
        label: string,
        dir: string,
        filter: string,
        canChooseDirectories: boolean,
        focusOwner: boolean,
      ) => {
        const openDialogOptions: OpenDialogOptions = {
          title: caption,
          defaultPath: resolveAliasedPath(dir),
          buttonLabel: label,
        };
        openDialogOptions.properties = ['openFile'];

        // FileOpen dialog can't be both a file opener and a directory opener on Windows
        // and Linux; so prefer the file opener (selecting a directory will just navigate into it
        // without selecting it.
        if (canChooseDirectories && process.platform === 'darwin') {
          openDialogOptions.properties.push('openDirectory');
        }

        if (filter) {
          openDialogOptions.filters = filterFromQFileDialogFilter(filter);
        }

        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }
        if (focusedWindow) {
          return dialog.showOpenDialog(focusedWindow, openDialogOptions);
        } else {
          return dialog.showOpenDialog(openDialogOptions);
        }
      },
    );

    ipcMain.handle(
      'desktop_get_save_file_name',
      async (
        event,
        caption: string,
        label: string,
        dir: string,
        defaultExtension: string,
        forceDefaultExtension: boolean,
        focusOwner: boolean,
      ) => {
        const saveDialogOptions: SaveDialogOptions = {
          title: caption,
          defaultPath: resolveAliasedPath(dir),
          buttonLabel: label,
        };

        if (defaultExtension) {
          saveDialogOptions['filters'] = [{ name: '', extensions: [defaultExtension.replace('.', '')] }];
        }

        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }
        if (focusedWindow) {
          return dialog.showSaveDialog(focusedWindow, saveDialogOptions);
        } else {
          return dialog.showSaveDialog(saveDialogOptions);
        }
      },
    );

    ipcMain.handle(
      'desktop_get_existing_directory',
      async (event, caption: string, label: string, dir: string, focusOwner: boolean) => {
        const openDialogOptions: OpenDialogOptions = {
          title: caption,
          defaultPath: resolveAliasedPath(dir),
          buttonLabel: label,
          properties: ['openDirectory', 'createDirectory', 'promptToCreate'],
        };

        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }

        if (focusedWindow) {
          return dialog.showOpenDialog(focusedWindow, openDialogOptions);
        } else {
          return dialog.showOpenDialog(openDialogOptions);
        }
      },
    );

    ipcMain.on('desktop_on_clipboard_selection_changed', () => {
      // This was previously used for Ace-specific workarounds on Qt
      // Desktop. Those workarounds no longer appear necessary.
    });

    ipcMain.on('desktop_undo', () => {
      // unless the active element is the ACE editor, the web page will handle it
      focusedWebContents()?.undo();
    });

    ipcMain.on('desktop_redo', () => {
      // unless the active element is the ACE editor, the web page will handle it
      focusedWebContents()?.redo();
    });

    ipcMain.on('desktop_clipboard_cut', () => {
      focusedWebContents()?.cut();
    });

    ipcMain.on('desktop_clipboard_copy', () => {
      focusedWebContents()?.copy();
    });

    ipcMain.on('desktop_clipboard_paste', () => {
      focusedWebContents()?.paste();
    });

    ipcMain.on('desktop_set_clipboard_text', (event, text: string) => {
      clipboard.writeText(text, 'clipboard');
    });

    ipcMain.handle('desktop_get_clipboard_text', () => {
      const text = clipboard.readText('clipboard');
      return text;
    });

    ipcMain.handle('desktop_get_clipboard_uris', () => {
      // if we don't have a URI list, nothing to do
      if (!clipboard.has('text/uri-list')) {
        return [];
      }

      // return uri list as array
      const data = clipboard.read('text/uri-list');
      const parts = data.split('\n');

      // strip off file prefix, if any
      const filePrefix = process.platform === 'win32' ? 'file:///' : 'file://';
      const trimmed = parts.map((x) => {
        if (x.startsWith(filePrefix)) {
          x = x.substring(filePrefix.length);
        }
        return x;
      });

      return trimmed;
    });

    // Check for an image on the clipboard; if one exists,
    // write it to file in the temporary directory and
    // return the path to that file.
    ipcMain.handle('desktop_get_clipboard_image', () => {
      // if we don't have any image, bail
      if (!clipboard.has('image/png')) {
        return '';
      }

      // read image from clipboard
      const image = clipboard.readImage('clipboard');
      const pngData = image.toPNG();

      // write to file
      const scratchDir = appState().scratchTempDir(new FilePath('/tmp'));
      const prefix = path.join(scratchDir.getAbsolutePath(), 'rstudio-clipboard', 'image-');
      const tempdir = mkdtempSync(prefix);
      const pngPath = path.join(tempdir, 'image.png');
      writeFileSync(pngPath, pngData);

      // return file path
      return pngPath;
    });

    ipcMain.on('desktop_set_global_mouse_selection', (event, selection: string) => {
      clipboard.writeText(selection, 'clipboard');
    });

    ipcMain.handle('desktop_get_global_mouse_selection', () => {
      const selection = clipboard.readText('selection');
      return selection;
    });

    ipcMain.handle('desktop_get_cursor_position', () => {
      const cursorPos = screen.getCursorScreenPoint();
      return { x: cursorPos.x, y: cursorPos.y };
    });

    ipcMain.handle('desktop_does_window_exist_at_cursor_position', () => {
      const cursorPos = screen.getCursorScreenPoint();
      const windows = BrowserWindow.getAllWindows();
      for (const window of windows) {
        if (window.isVisible()) {
          const windowPos = window.getBounds();
          if (
            cursorPos.x >= windowPos.x &&
            cursorPos.x <= windowPos.x + windowPos.width &&
            cursorPos.y >= windowPos.y &&
            cursorPos.y <= windowPos.y + windowPos.height
          ) {
            return true;
          }
        }
      }
      return false;
    });

    ipcMain.on('desktop_on_workbench_initialized', (event, scratchPath: string) => {
      this.emit(GwtCallback.WORKBENCH_INITIALIZED);
      appState().setScratchTempDir(new FilePath(scratchPath));
    });

    ipcMain.on('desktop_show_folder', (event, path: string) => {
      shell.openPath(path).catch((value) => {
        logger().logErrorMessage(value);
      });
    });

    ipcMain.on('desktop_show_file', (event, file: string) => {
      shell.showItemInFolder(file);
    });

    ipcMain.on('desktop_show_word_doc', (event, wordDoc: string) => {
      GwtCallback.unimpl('desktop_show_word_doc');
    });

    ipcMain.on('desktop_show_ppt_presentation', (event, pptDoc: string) => {
      GwtCallback.unimpl('desktop_show_ppt_presentation');
    });

    ipcMain.on('desktop_show_pdf', (event, path: string, pdfPage: string) => {
      GwtCallback.unimpl('desktop_show_pdf');
    });

    ipcMain.on('desktop_prepare_show_word_doc', () => {
      GwtCallback.unimpl('desktop_prepare_show_word_doc');
    });

    ipcMain.on('desktop_prepare_show_ppt_presentation', () => {
      GwtCallback.unimpl('desktop_prepare_show_ppt_presentation');
    });

    ipcMain.handle('desktop_get_r_version', () => {
      return '';
    });

    ipcMain.handle('desktop_choose_r_version', () => {
      GwtCallback.unimpl('desktop_choose_r_version');
      return '';
    });

    ipcMain.handle('desktop_device_pixel_ratio', () => {
      GwtCallback.unimpl('desktop_device_pixel_ratio');
      return 1.0;
    });

    ipcMain.on(
      'desktop_open_minimal_window',
      (event: IpcMainEvent, name: string, url: string, width: number, height: number) => {
        // handle chrome://gpu specially
        if (url === 'chrome://gpu') {
          const window = new BrowserWindow();
          return window.loadURL('chrome://gpu');
        }

        // regular path for other windows
        const sender = this.getSender('desktop_open_minimal_window', event.processId, event.frameId);
        const minimalWindow = openMinimalWindow(sender, name, url, width, height);
        minimalWindow.window.once('ready-to-show', () => {
          minimalWindow.window.show();
        });
      },
    );

    ipcMain.on('desktop_activate_minimal_window', (event, name: string) => {
      // we can only activate named windows
      if (name && name !== '_blank') {
        activateWindow(name);
      }
    });

    ipcMain.on('desktop_activate_satellite_window', (event, name: string) => {
      activateWindow(name);
    });

    ipcMain.handle(
      'desktop_prepare_for_satellite_window',
      (event, name: string, x: number, y: number, width: number, height: number) => {
        appState().prepareForWindow({
          type: 'satellite',
          name: name,
          mainWindow: this.mainWindow,
          screenX: x,
          screenY: y,
          width: width,
          height: height,
          allowExternalNavigate: this.mainWindow.isRemoteDesktop,
        });
      },
    );

    ipcMain.handle(
      'desktop_prepare_for_named_window',
      (event, name: string, allowExternalNavigate: boolean, showToolbar: boolean) => {
        appState().prepareForWindow({
          type: 'secondary',
          name: name,
          allowExternalNavigate: allowExternalNavigate,
          showToolbar: showToolbar,
        });
      },
    );

    ipcMain.on('desktop_close_named_window', (event, name: string) => {
      GwtCallback.unimpl('desktop_close_named_window');
    });

    ipcMain.handle(
      'desktop_copy_page_region_to_clipboard',
      (_event, x: number, y: number, width: number, height: number) => {
        const rect: Rectangle = { x, y, width, height };
        this.mainWindow.window
          .capturePage(rect)
          .then((image) => {
            clipboard.writeImage(image);
          })
          .catch((error) => {
            logger().logError(error);
          });
      },
    );

    ipcMain.on('desktop_export_page_region_to_file', (event, targetPath, format, left, top, width, height) => {
      GwtCallback.unimpl('desktop_export_page_region_to_file');
    });

    ipcMain.on('desktop_print_text', (event, text) => {
      GwtCallback.unimpl('desktop_print_text');
    });

    ipcMain.on('desktop_paint_print_text', (event, printer) => {
      GwtCallback.unimpl('desktop_paint_print_text');
    });

    ipcMain.on('desktop_print_finished', (event, result) => {
      GwtCallback.unimpl('desktop_print_finished');
    });

    ipcMain.handle('desktop_supports_clipboard_metafile', () => {
      return process.platform === 'win32';
    });

    ipcMain.handle(
      'desktop_show_message_box',
      async (event, type, caption, message, buttons, defaultButton, cancelButton) => {
        const openDialogOptions: MessageBoxOptions = {
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
      },
    );

    ipcMain.on('desktop_bring_main_frame_to_front', () => {});

    ipcMain.on('desktop_bring_main_frame_behind_active', () => {
      GwtCallback.unimpl('desktop_bring_main_frame_behind_active');
    });

    ipcMain.handle('desktop_rendering_engine', () => {
      return ElectronDesktopOptions().renderingEngine();
    });

    ipcMain.on('desktop_set_desktop_rendering_engine', (event, engine) => {
      ElectronDesktopOptions().setRenderingEngine(engine);
    });

    ipcMain.handle('desktop_filter_text', (event, text: string) => {
      GwtCallback.unimpl('desktop_filter_text');
      return text;
    });

    ipcMain.on('desktop_clean_clipboard', (event, stripHtml) => {
      desktop.cleanClipboard(stripHtml);
    });

    ipcMain.on('desktop_set_pending_quit', (event, pendingQuit: number) => {
      this.pendingQuit = pendingQuit;
    });

    ipcMain.on('desktop_open_project_in_new_window', (event, projectFilePath) => {
      if (!this.isRemoteDesktop) {
        this.mainWindow.launchRStudio({
          projectFilePath: resolveAliasedPath(projectFilePath),
        });
      } else {
        // start new Remote Desktop RStudio process with the session URL
        this.mainWindow.launchRemoteRStudioProject(projectFilePath);
      }
    });

    ipcMain.on('desktop_open_session_in_new_window', (event, workingDirectoryPath) => {
      if (!this.isRemoteDesktop) {
        this.mainWindow.launchRStudio({
          workingDirectory: resolveAliasedPath(workingDirectoryPath),
        });
      } else {
        // start the new session on the currently connected server
        this.mainWindow.launchRemoteRStudio();
      }
    });

    ipcMain.on('desktop_open_terminal', (event, terminalPath, workingDirectory, extraPathEntries, shellType) => {
      GwtCallback.unimpl('desktop_open_terminal');
    });

    ipcMain.on('desktop_get_fixed_width_font_list', (event) => {
      event.returnValue = this.monospaceFonts.join('\n');
    });

    ipcMain.on('desktop_get_fixed_width_font', (event) => {
      let fixedWidthFont = ElectronDesktopOptions().fixedWidthFont();

      if (!fixedWidthFont) {
        fixedWidthFont = defaultFonts[0];

        for (const font of defaultFonts) {
          if (this.monospaceFonts.includes(font)) {
            fixedWidthFont = font;
            break;
          }
        }
      }

      event.returnValue = `"${fixedWidthFont}"`;
    });

    ipcMain.on('desktop_get_proportional_font', (event) => {
      let defaultFonts: string[];
      if (process.platform === 'darwin') {
        defaultFonts = ['Lucida Grande', 'Lucida Sans', 'DejaVu Sans', 'Segoe UI', 'Verdana', 'Helvetica'];
      } else if (process.platform === 'win32') {
        defaultFonts = ['Segoe UI', 'Verdana', 'Lucida Sans', 'DejaVu Sans', 'Lucida Grande', 'Helvetica'];
      } else {
        defaultFonts = ['Lucida Sans', 'DejaVu Sans', 'Lucida Grande', 'Segoe UI', 'Verdana', 'Helvetica'];
      }

      let proportionalFont = defaultFonts[0];
      for (const font of defaultFonts) {
        if (this.proportionalFonts.includes(font)) {
          proportionalFont = font;
          break;
        }
      }
      event.returnValue = `"${proportionalFont}"`;
    });

    ipcMain.on('desktop_set_fixed_width_font', (_event, font) => {
      if (font !== undefined) {
        ElectronDesktopOptions().setFixedWidthFont(font);
      }
    });

    ipcMain.handle('desktop_get_zoom_levels', () => {
      GwtCallback.unimpl('desktop_get_zoom_levels');
      return '';
    });

    ipcMain.on('desktop_get_zoom_level', (event) => {
      event.returnValue = ElectronDesktopOptions().zoomLevel();
    });

    ipcMain.on('desktop_set_zoom_level', (event, zoomLevel) => {
      this.getSender('desktop_zoom_actual_size', event.processId, event.frameId).setZoomLevel(zoomLevel);
    });

    ipcMain.on('desktop_zoom_in', (event) => {
      this.getSender('desktop_zoom_in', event.processId, event.frameId).zoomIn();
    });

    ipcMain.on('desktop_zoom_out', (event) => {
      this.getSender('desktop_zoom_out', event.processId, event.frameId).zoomOut();
    });

    ipcMain.on('desktop_zoom_actual_size', (event) => {
      this.getSender('desktop_zoom_actual_size', event.processId, event.frameId).zoomActualSize();
    });

    ipcMain.on('desktop_set_background_color', (event, rgbColor) => {});

    ipcMain.on('desktop_change_title_bar_color', (event, red, green, blue) => {});

    ipcMain.on('desktop_sync_to_editor_theme', (event, isDark) => {});

    ipcMain.handle('desktop_get_enable_accessibility', () => {
      GwtCallback.unimpl('desktop_get_enable_accessibility');
      return true;
    });

    ipcMain.on('desktop_set_enable_accessibility', (event, enable) => {
      GwtCallback.unimpl('desktop_set_enable_accessibility');
    });

    ipcMain.handle('desktop_get_clipboard_monitoring', () => {
      GwtCallback.unimpl('desktop_get_clipboard_monitoring');
      return false;
    });

    ipcMain.on('desktop_set_clipboard_monitoring', (event, monitoring) => {
      GwtCallback.unimpl('desktop_set_clipboard_monitoring');
    });

    ipcMain.handle('desktop_get_ignore_gpu_exclusion_list', (event, ignore) => {
      return !ElectronDesktopOptions().useGpuExclusionList();
    });

    ipcMain.on('desktop_set_ignore_gpu_exclusion_list', (event, ignore: boolean) => {
      ElectronDesktopOptions().setUseGpuExclusionList(!ignore);
    });

    ipcMain.handle('desktop_get_disable_gpu_driver_bug_workarounds', () => {
      return !ElectronDesktopOptions().useGpuDriverBugWorkarounds();
    });

    ipcMain.on('desktop_set_disable_gpu_driver_bug_workarounds', (event, disable: boolean) => {
      ElectronDesktopOptions().setUseGpuDriverBugWorkarounds(!disable);
    });

    ipcMain.on('desktop_show_license_dialog', () => {
      GwtCallback.unimpl('desktop_show_license_dialog');
    });

    ipcMain.on('desktop_show_session_server_options_dialog', () => {
      GwtCallback.unimpl('desktop_show_session_server_options_dialog');
    });

    ipcMain.handle('desktop_get_init_messages', () => {
      return '';
    });

    ipcMain.handle('desktop_get_license_status_message', () => {
      GwtCallback.unimpl('desktop_get_license_status_messages');
      return '';
    });

    ipcMain.handle('desktop_allow_product_usage', () => {
      GwtCallback.unimpl('desktop_allow_product_usage');
      return true;
    });

    ipcMain.handle('desktop_get_desktop_synctex_viewer', () => {
      GwtCallback.unimpl('desktop_get_desktop_synctex_viewer');
      return '';
    });

    ipcMain.on('desktop_external_synctex_preview', (event, pdfPath, page) => {
      GwtCallback.unimpl('desktop_external_synctex_preview');
    });

    ipcMain.on('desktop_external_synctex_view', (event, pdfFile, srcFile, line, column) => {
      GwtCallback.unimpl('desktop_external_synctex_view');
    });

    ipcMain.handle('desktop_supports_fullscreen_mode', () => {
      return process.platform === 'darwin';
    });

    ipcMain.on('desktop_toggle_fullscreen_mode', () => {
      if (process.platform === 'darwin') {
        this.mainWindow.window.fullScreen = !this.mainWindow.window.fullScreen;
      }
    });

    ipcMain.on('desktop_show_keyboard_shortcut_help', () => {
      GwtCallback.unimpl('desktop_show_keyboard_shortcut_help');
    });

    ipcMain.on('desktop_launch_session', (event, reload) => {
      this.mainWindow.launchSession(reload);
    });

    ipcMain.on('desktop_reload_zoom_window', () => {
      const browser = appState().windowTracker.getWindow('_rstudio_zoom');
      if (browser) {
        browser.window.webContents.reload();
      }
    });

    ipcMain.on('desktop_set_tutorial_url', (event, url) => {
      GwtCallback.unimpl('desktop_set_tutorial_url');
    });

    ipcMain.on('desktop_set_viewer_url', (event, url) => {
      this.getSender('desktop_set_viewer_url', event.processId, event.frameId).setViewerUrl(url);
    });

    ipcMain.on('desktop_reload_viewer_zoom_window', (event, url) => {
      const browser = appState().windowTracker.getWindow('_rstudio_viewer_zoom');
      if (browser) {
        void browser.window.webContents.loadURL(url);
      }
    });

    ipcMain.on('desktop_set_shiny_dialog_url', (event, url) => {
      GwtCallback.unimpl('desktop_set_shiny_dialog_url');
    });

    ipcMain.handle('desktop_get_scrolling_compensation_type', () => {
      GwtCallback.unimpl('desktop_get_scrolling_compensation_type');
      return '';
    });

    ipcMain.handle('desktop_is_macos', () => {
      return process.platform === 'darwin';
    });

    ipcMain.handle('desktop_is_centos', () => {
      return isCentOS();
    });

    ipcMain.on('desktop_set_busy', (event, busy) => {});

    ipcMain.on('desktop_set_window_title', (event, title: string) => {
      this.mainWindow.window.setTitle(`${title} - RStudio`);
    });

    ipcMain.on('desktop_install_rtools', (event, version, installerPath) => {
      GwtCallback.unimpl('desktop_install_rtools');
    });

    ipcMain.handle('desktop_get_display_dpi', () => {
      return '72';
    });

    ipcMain.on('desktop_on_session_quit', () => {
      this.emit(GwtCallback.SESSION_QUIT);
    });

    ipcMain.handle('desktop_get_session_server', () => {
      GwtCallback.unimpl('desktop_get_session_server');
      return {};
    });

    ipcMain.handle('desktop_get_session_servers', () => {
      return [];
    });

    ipcMain.on('desktop_reconnect_to_session_server', (event, sessionServerJson) => {
      GwtCallback.unimpl('desktop_reconnect_to_session_server');
    });

    ipcMain.handle('desktop_set_launcher_server', (event, sessionServerJson) => {
      GwtCallback.unimpl('desktop_set_launcher_server');
      return false;
    });

    ipcMain.on('desktop_connect_to_launcher_server', () => {
      GwtCallback.unimpl('desktop_connect_to_launcher_server');
    });

    ipcMain.handle('desktop_get_launcher_server', () => {
      GwtCallback.unimpl('desktop_get_launcher_server');
      return {};
    });

    ipcMain.on('desktop_start_launcher_job_status_stream', (event, jobId) => {
      GwtCallback.unimpl('desktop_start_launcher_job_status_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_status_stream', (event, jobId) => {
      GwtCallback.unimpl('desktop_stop_launcher_job_status_stream');
    });

    ipcMain.on('desktop_start_launcher_job_output_stream', (event, jobId) => {
      GwtCallback.unimpl('desktop_start_launcher_job_output_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_output_stream', (event, jobId) => {
      GwtCallback.unimpl('desktop_stop_launcher_job_output_stream');
    });

    ipcMain.on('desktop_control_launcher_job', (event, jobId, operation) => {
      GwtCallback.unimpl('desktop_control_launcher_job');
    });

    ipcMain.on('desktop_submit_launcher_job', (event, job) => {
      GwtCallback.unimpl('desktop_submit_launcher_job');
    });

    ipcMain.on('desktop_get_job_container_user', () => {
      GwtCallback.unimpl('desktop_get_job_container_user');
    });

    ipcMain.on('desktop_validate_jobs_config', () => {
      GwtCallback.unimpl('desktop_validate_jobs_config');
    });

    ipcMain.handle('desktop_get_proxy_port_number', () => {
      GwtCallback.unimpl('desktop_get_proxy_port_number');
      return -1;
    });

    ipcMain.handle('desktop_startup_error_info', async (event, varName: string) => {
      return resolveTemplateVar(varName, this.errorPageData);
    });
  }

  setRemoteDesktop(isRemoteDesktop: boolean): void {
    this.isRemoteDesktop = isRemoteDesktop;
  }

  static unimpl(ipcName: string): void {
    if (app.isPackaged) {
      return;
    }

    const focusedWindow = BrowserWindow.getFocusedWindow();

    const dialogOptions = {
      title: i18next.t('gwtCallbackTs.unimplemented'),
      message: i18next.t('gwtCallbackTs.callbackNyiLowercase', { ipcName }),
    };

    if (focusedWindow) {
      void dialog.showMessageBox(focusedWindow, dialogOptions);
    } else {
      void dialog.showMessageBox(dialogOptions);
    }
  }

  collectPendingQuitRequest(): PendingQuit {
    if (this.pendingQuit != PendingQuit.PendingQuitNone) {
      const currentPendingQuit = this.pendingQuit;
      this.pendingQuit = PendingQuit.PendingQuitNone;
      return currentPendingQuit;
    } else {
      return PendingQuit.PendingQuitNone;
    }
  }

  convertMessageBoxType(type: number): string {
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

  convertButtons(buttons: string): string[] {
    return buttons.split('|');
  }

  /**
   * Register a GwtWindow as a possible GwtCallback recipient
   *
   * @param owner GwtWindow to register for GwtCallbacks
   */
  registerOwner(owner: GwtWindow): void {
    this.owners.add(owner);
  }

  /**
   * Unregister a GwtWindow from receiving GwtCallbacks
   *
   * @param owner A GwtWindow that previously registered for GwtCallbacks
   */
  unregisterOwner(owner: GwtWindow): void {
    this.owners.delete(owner);
  }

  /**
   * @param event
   * @returns Registered GwtWindow that sent the event (throws if not found)
   */
  getSender(message: string, processId: number, frameId: number): GwtWindow {
    const frame = webFrameMain.fromId(processId, frameId);
    if (frame) {
      for (const win of this.owners) {
        if (win.window.webContents.mainFrame === frame) {
          return win;
        }
      }
    }
    const err = new Error(`Received callback ${message} from unknown window`);
    logger().logError(err);
    throw err;
  }

  setErrorPageInfo(info: Map<string, string>): void {
    this.errorPageData = info;
  }
}
