/*
 * desktop-bridge.ts
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

import { ipcRenderer, OpenDialogReturnValue, SaveDialogReturnValue, webContents } from 'electron';
import { logString } from './logger-bridge';
import { createAliasedPath, normalizeSeparators } from '../ui/utils';
import { safeError } from '../core/err';

interface VoidCallback<Type> {
  (result: Type): void;
}

interface CursorPosition {
  x: number;
  y: number;
}

function reportIpcError(name: string, error: Error) {
  logString('err', `IpcError: ${name}: ${error.message}`);
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getDesktopBridge() {
  return {
    writeStdout: (output: string) => {
      ipcRenderer.send('desktop_write_stdout', output);
    },

    writeStderr: (output: string) => {
      ipcRenderer.send('desktop_write_stderr', output);
    },

    browseUrl: (url: string) => {
      ipcRenderer.send('desktop_browse_url', url);
    },

    getOpenFileName: async (
      caption: string,
      label: string,
      dir: string,
      filter: string,
      canChooseDirectories: boolean,
      focusOwner: boolean,
      callback: VoidCallback<string>,
    ) => {
      try {
        const result: OpenDialogReturnValue = await ipcRenderer.invoke(
          'desktop_get_open_file_name',
          caption,
          label,
          dir,
          filter,
          canChooseDirectories,
          focusOwner,
        );

        if (result.canceled || result.filePaths.length === 0) {
          return callback('');
        }

        const selectedPath = result.filePaths[0];
        const userHomePath: string = await ipcRenderer.invoke('desktop_get_user_home_path');
        const aliasedPath = createAliasedPath(selectedPath, userHomePath);
        return callback(aliasedPath);
      } catch (error: unknown) {
        reportIpcError('desktop_get_open_file_name', error as Error);
      }
    },

    getSaveFileName: (
      caption: string,
      label: string,
      dir: string,
      defaultExtension: string,
      forceDefaultExtension: boolean,
      focusOwner: boolean,
      callback: VoidCallback<string>,
    ) => {
      ipcRenderer
        .invoke('desktop_get_save_file_name', caption, label, dir, defaultExtension, forceDefaultExtension, focusOwner)
        .then((result: SaveDialogReturnValue) => {
          // if the result was canceled, bail early
          if (result.canceled as boolean) {
            return callback('');
          }

          // if we don't have a default extension, just invoke callback
          let filePath = result.filePath as string;
          if (defaultExtension.length === 0) {
            return callback(filePath);
          }

          // add default extension if necessary
          const dotIndex = filePath.lastIndexOf('.');
          const ext = dotIndex > 0 ? filePath.substring(dotIndex) : '';
          if (ext.length === 0 || (forceDefaultExtension && ext !== defaultExtension)) {
            const name = dotIndex > 0 ? filePath.substring(0, dotIndex) : filePath;
            filePath = name + defaultExtension;
          }

          if (process.platform === 'win32') {
            filePath = normalizeSeparators(filePath, '/');
          }

          ipcRenderer
            .invoke('desktop_get_user_home_path')
            .then((userHomePath: string) => {
              const expandedHomePath = normalizeSeparators(userHomePath.length > 0 ? userHomePath : '', '/');
              filePath = createAliasedPath(filePath, expandedHomePath);

              // invoke callback
              return callback(filePath);
            })
            .catch((error) => reportIpcError('getSaveFileName', error));
        })
        .catch((error) => reportIpcError('getSaveFileName', error));
    },

    getExistingDirectory: async (
      caption: string,
      label: string,
      dir: string,
      focusOwner: boolean,
      callback: VoidCallback<string>,
    ) => {
      try {
        const result: OpenDialogReturnValue = await ipcRenderer.invoke(
          'desktop_get_existing_directory',
          caption,
          label,
          dir,
          focusOwner,
        );

        if (result.canceled as boolean) {
          return callback('');
        }

        const selectedPath = result.filePaths[0];
        const userHomePath: string = await ipcRenderer.invoke('desktop_get_user_home_path');
        const aliasedPath = createAliasedPath(selectedPath, userHomePath);
        return callback(aliasedPath);
      } catch (error: unknown) {
        reportIpcError('getExistingDirectory', error as Error);
      }
    },

    getUserHomePath: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_user_home_path')
        .then((path) => callback(path))
        .catch((error) => reportIpcError('getUserHomePath', error));
    },

    onClipboardSelectionChanged: () => {
      ipcRenderer.send('desktop_on_clipboard_selection_changed');
    },

    undo: () => {
      ipcRenderer.send('desktop_undo');
    },

    redo: () => {
      ipcRenderer.send('desktop_redo');
    },

    clipboardCut: () => {
      ipcRenderer.send('desktop_clipboard_cut');
    },

    clipboardCopy: () => {
      ipcRenderer.send('desktop_clipboard_copy');
    },

    clipboardPaste: () => {
      ipcRenderer.send('desktop_clipboard_paste');
    },

    setClipboardText: (text: string) => {
      ipcRenderer.send('desktop_set_clipboard_text', text);
    },

    getClipboardText: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_text')
        .then((text) => callback(text))
        .catch((error) => reportIpcError('getClipboardText', error));
    },

    getClipboardUris: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_uris')
        .then((text) => callback(text))
        .catch((error) => reportIpcError('getClipboardUris', error));
    },

    getClipboardImage: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_image')
        .then((text) => callback(text))
        .catch((error) => reportIpcError('getClipboardImage', error));
    },

    setGlobalMouseSelection: (selection: string) => {
      ipcRenderer.send('desktop_set_global_mouse_selection', selection);
    },

    getGlobalMouseSelection: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_global_mouse_selection')
        .then((selection) => callback(selection))
        .catch((error) => reportIpcError('getGlobalMouseSelection', error));
    },

    getCursorPosition: (callback: VoidCallback<CursorPosition>) => {
      ipcRenderer
        .invoke('desktop_get_cursor_position')
        .then((position) => callback(position))
        .catch((error) => reportIpcError('getCursorPosition', error));
    },

    doesWindowExistAtCursorPosition: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_does_window_exist_at_cursor_position')
        .then((exists) => callback(exists))
        .catch((error) => reportIpcError('doesWindowExistAtCursorPosition', error));
    },

    onWorkbenchInitialized: (scratchPath: string) => {
      ipcRenderer.send('desktop_on_workbench_initialized', scratchPath);
    },

    showFolder: (path: string) => {
      ipcRenderer.send('desktop_show_folder', path);
    },

    showFile: (file: string) => {
      ipcRenderer.send('desktop_show_file', file);
    },

    showWordDoc: (wordDoc: string) => {
      ipcRenderer.send('desktop_show_word_doc', wordDoc);
    },

    showPptPresentation: (pptDoc: string) => {
      ipcRenderer.send('desktop_show_ppt_presentation', pptDoc);
    },

    showPDF: (path: string, pdfPage: string) => {
      ipcRenderer.send('desktop_show_pdf', path, pdfPage);
    },

    prepareShowWordDoc: () => {
      ipcRenderer.send('desktop_prepare_show_word_doc');
    },

    prepareShowPptPresentation: () => {
      ipcRenderer.send('desktop_prepare_show_ppt_presentation');
    },

    // R version selection currently Win32 only
    getRVersion: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_r_version')
        .then((rver) => callback(rver))
        .catch((error) => reportIpcError('getRVersion', error));
    },

    chooseRVersion: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_choose_r_version')
        .then((rver) => callback(rver))
        .catch((error) => reportIpcError('chooseRVersion', error));
    },

    openMinimalWindow: (name: string, url: string, width: number, height: number) => {
      ipcRenderer.send('desktop_open_minimal_window', name, url, width, height);
    },

    activateMinimalWindow: (name: string) => {
      ipcRenderer.send('desktop_activate_minimal_window', name);
    },

    activateSatelliteWindow: (name: string) => {
      ipcRenderer.send('desktop_activate_satellite_window', name);
    },

    prepareForSatelliteWindow: (
      name: string,
      x: number,
      y: number,
      width: number,
      height: number,
      callback: () => void,
    ) => {
      ipcRenderer
        .invoke('desktop_prepare_for_satellite_window', name, x, y, width, height)
        .then(() => callback())
        .catch((error) => reportIpcError('prepareForSatelliteWindow', error));
    },

    prepareForNamedWindow: (
      name: string,
      allowExternalNavigate: boolean,
      showToolbar: boolean,
      callback: () => void,
    ) => {
      ipcRenderer
        .invoke('desktop_prepare_for_named_window', name, allowExternalNavigate, showToolbar)
        .then(() => callback())
        .catch((error) => reportIpcError('prepareForNamedWindow', error));
    },

    setNumGwtModalsShowing: (gwtModalsShowing: number) => {
      ipcRenderer.send('desktop_set_gwt_num_modals_showing', gwtModalsShowing);
    },

    copyPageRegionToClipboard: (x: number, y: number, width: number, height: number, callback: () => void) => {
      ipcRenderer
        .invoke('desktop_copy_page_region_to_clipboard', x, y, width, height)
        .then(() => callback())
        .catch((error) => reportIpcError('desktop_copy_page_region_to_clipboard', error));
    },

    copyImageAtXYToClipboard: (x: number, y: number, callback: () => void) => {
      ipcRenderer
        .invoke('desktop_copy_image_at_xy_to_clipboard', x, y)
        .then(() => callback())
        .catch((error) => reportIpcError('desktop_copy_image_at_xy_to_clipboard', error));
    },

    exportPageRegionToFile: (
      targetPath: string,
      format: string,
      left: number,
      top: number,
      width: number,
      height: number,
      callback: () => void,
    ) => {
      ipcRenderer
        .invoke('desktop_export_page_region_to_file', targetPath, format, left, top, width, height)
        .then(() => callback())
        .catch((error) => reportIpcError('desktop_export_page_region_to_file', error));
    },

    supportsClipboardMetafile: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_supports_clipboard_metafile')
        .then((metafilesupport) => callback(metafilesupport))
        .catch((error) => reportIpcError('supportsClipboardMetafile', error));
    },

    showMessageBox: (
      type: number,
      caption: string,
      message: string,
      buttons: string,
      defaultButton: number,
      cancelButton: number,
      callback: VoidCallback<number>,
    ) => {
      ipcRenderer
        .invoke('desktop_show_message_box', type, caption, message, buttons, defaultButton, cancelButton)
        .then((result) => callback(result.response))
        .catch((error) => reportIpcError('showMessageBox', error));
    },

    bringMainFrameToFront: () => {
      ipcRenderer.send('desktop_bring_main_frame_to_front');
    },

    bringMainFrameBehindActive: () => {
      ipcRenderer.send('desktop_bring_main_frame_behind_active');
    },

    desktopRenderingEngine: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_rendering_engine')
        .then((engine) => callback(engine))
        .catch((error) => reportIpcError('desktopRenderingEngine', error));
    },

    setDesktopRenderingEngine: (engine: string) => {
      ipcRenderer.send('desktop_set_desktop_rendering_engine', engine);
    },

    cleanClipboard: (stripHtml: boolean) => {
      ipcRenderer.send('desktop_clean_clipboard', stripHtml);
    },

    setPendingQuit: (pendingQuit: number, callback: VoidCallback<unknown>) => {
      ipcRenderer
        .invoke('desktop_set_pending_quit', pendingQuit)
        .then((response) => callback(response))
        .catch((error) => reportIpcError('setPendingQuit', error));
    },

    openFile: async (path: string) => {
      if (!path) {
        return;
      }
      const webcontents = webContents.getAllWebContents();

      if (webcontents.length) {
        path = path.replaceAll('\\', '\\\\').replaceAll('"', '\\"').replaceAll('\n', '\\n');

        // use first window that has the desktop hooks (the splash screen, for example, does NOT have them)
        for (const webcontent of webcontents) {
          const hasHooks: boolean = await webcontent.executeJavaScript('!!window.desktopHooks');
          if (hasHooks) {
            try {
              await webcontent.executeJavaScript(`window.desktopHooks.openFile("${path}")`);
            } catch (error: unknown) {
              logString('err', safeError(error).message);
            }
            break;
          }
        }
      }
    },

    openProjectInNewWindow: (projectFilePath: string) => {
      ipcRenderer.send('desktop_open_project_in_new_window', projectFilePath);
    },

    openSessionInNewWindow: (workingDirectoryPath: string) => {
      ipcRenderer.send('desktop_open_session_in_new_window', workingDirectoryPath);
    },

    getFixedWidthFontList: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_fixed_width_font_list')
        .then((fonts) => callback(fonts))
        .catch((error) => reportIpcError('getFixedWidthFontList', error));
    },

    getFixedWidthFont: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_fixed_width_font')
        .then((font) => callback(font))
        .catch((error) => reportIpcError('getFixedWidthFont', error));
    },

    setFixedWidthFont: (font: string) => {
      ipcRenderer.send('desktop_set_fixed_width_font', font);
    },

    getZoomLevel: (callback: VoidCallback<number>) => {
      const zoomLevel = ipcRenderer.sendSync('desktop_get_zoom_level');
      callback(zoomLevel);
    },

    setZoomLevel: (zoomLevel: number) => {
      ipcRenderer.send('desktop_set_zoom_level', zoomLevel);
    },

    zoomIn: () => {
      ipcRenderer.send('desktop_zoom_in');
    },

    zoomOut: () => {
      ipcRenderer.send('desktop_zoom_out');
    },

    zoomActualSize: () => {
      ipcRenderer.send('desktop_zoom_actual_size');
    },

    setBackgroundColor: (rgbColor: Record<string, unknown>[]) => {
      ipcRenderer.send('desktop_set_background_color', rgbColor);
    },

    changeTitleBarColor: (red: number, green: number, blue: number) => {
      ipcRenderer.send('desktop_change_title_bar_color', red, green, blue);
    },

    syncToEditorTheme: (isDark: boolean) => {
      ipcRenderer.send('desktop_sync_to_editor_theme', isDark);
    },

    getEnableAccessibility: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_get_enable_accessibility')
        .then((enabled) => callback(enabled))
        .catch((error) => reportIpcError('getEnableAccessibility', error));
    },

    setEnableAccessibility: (enable: boolean) => {
      ipcRenderer.send('desktop_set_enable_accessibility', enable);
    },

    setAutohideMenubar: (enable: boolean) => {
      ipcRenderer.send('desktop_set_autohide_menubar', enable);
    },

    setDisableRendererAccessibility: (disable: boolean) => {
      ipcRenderer.send('desktop_set_disable_renderer_accessibility', disable);
    },

    getIgnoreGpuExclusionList: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_get_ignore_gpu_exclusion_list')
        .then((ignore) => callback(ignore))
        .catch((error) => reportIpcError('getIgnoreGpuExclusionList', error));
    },

    setIgnoreGpuExclusionList: (ignore: boolean) => {
      ipcRenderer.send('desktop_set_ignore_gpu_exclusion_list', ignore);
    },

    getDisableGpuDriverBugWorkarounds: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_get_disable_gpu_driver_bug_workarounds')
        .then((disabled) => callback(disabled))
        .catch((error) => reportIpcError('getDisableGpuDriverBugWorkarounds', error));
    },

    setDisableGpuDriverBugWorkarounds: (disable: boolean) => {
      ipcRenderer.send('desktop_set_disable_gpu_driver_bug_workarounds', disable);
    },

    getInitMessages: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_init_messages')
        .then((messages) => callback(messages))
        .catch((error) => reportIpcError('getInitMessages', error));
    },

    getDesktopSynctexViewer: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_desktop_synctex_viewer')
        .then((viewer) => callback(viewer))
        .catch((error) => reportIpcError('getDesktopSynctexViewer', error));
    },

    externalSynctexPreview: (pdfPath: string, page: number) => {
      ipcRenderer.send('desktop_external_synctex_preview', pdfPath, page);
    },

    externalSynctexView: (pdfFile: string, srcFile: string, line: number, column: number) => {
      ipcRenderer.send('desktop_external_synctex_view', pdfFile, srcFile, line, column);
    },

    supportsFullscreenMode: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_supports_fullscreen_mode')
        .then((supportsFullScreen) => callback(supportsFullScreen))
        .catch((error) => reportIpcError('supportsFullscreenMode', error));
    },

    toggleFullscreenMode: () => {
      ipcRenderer.send('desktop_toggle_fullscreen_mode');
    },

    showKeyboardShortcutHelp: () => {
      ipcRenderer.send('desktop_show_keyboard_shortcut_help');
    },

    launchSession: (reload: boolean) => {
      ipcRenderer.send('desktop_launch_session', reload);
    },

    reloadZoomWindow: () => {
      ipcRenderer.send('desktop_reload_zoom_window');
    },

    setTutorialUrl: (url: string) => {
      ipcRenderer.send('desktop_set_tutorial_url', url);
    },

    setViewerUrl: (url: string) => {
      ipcRenderer.send('desktop_set_viewer_url', url);
    },

    setPresentationUrl: (url: string) => {
      ipcRenderer.send('desktop_set_presentation_url', url);
    },

    reloadViewerZoomWindow: (url: string) => {
      ipcRenderer.send('desktop_reload_viewer_zoom_window', url);
    },

    setShinyDialogUrl: (url: string) => {
      ipcRenderer.send('desktop_set_shiny_dialog_url', url);
    },

    allowNavigation: (url: string, callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_allow_navigation', url)
        .then((isSafe) => callback(isSafe))
        .catch((error) => reportIpcError('desktop_allow_navigation', error));
    },

    isMacOS: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_is_macos')
        .then((isMac) => callback(isMac))
        .catch((error) => reportIpcError('isMacOS', error));
    },

    isCentOS: (callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_is_centos')
        .then((isCentOS) => callback(isCentOS))
        .catch((error) => reportIpcError('isCentOS', error));
    },

    setBusy: (busy: boolean) => {
      ipcRenderer.send('desktop_set_busy', busy);
    },

    setWindowTitle: (title: string) => {
      ipcRenderer.send('desktop_set_window_title', title);
    },

    installRtools: (version: string, installerPath: string) => {
      ipcRenderer.send('desktop_install_rtools', version, installerPath);
    },

    getDisplayDpi: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_display_dpi')
        .then((dpi) => callback(dpi))
        .catch((error) => reportIpcError('getDisplayDpi', error));
    },

    onSessionQuit: () => {
      ipcRenderer.send('desktop_on_session_quit');
    },

    crashDesktopApplication: () => {
      ipcRenderer.send('desktop_stop_main_thread');
    },

    getSessionServer: (callback: VoidCallback<Record<string, unknown>>) => {
      ipcRenderer
        .invoke('desktop_get_session_server')
        .then((server) => callback(server))
        .catch((error) => reportIpcError('getSessionServer', error));
    },

    getSessionServers: (callback: VoidCallback<Record<string, unknown>[]>) => {
      ipcRenderer
        .invoke('desktop_get_session_servers')
        .then((servers) => callback(servers))
        .catch((error) => reportIpcError('getSessionServers', error));
    },

    reconnectToSessionServer: (sessionServerJson: Record<string, unknown>) => {
      ipcRenderer.send('desktop_reconnect_to_session_server', sessionServerJson);
    },

    setLauncherServer: (sessionServerJson: Record<string, unknown>, callback: VoidCallback<boolean>) => {
      ipcRenderer
        .invoke('desktop_set_launcher_server', sessionServerJson)
        .then((result) => callback(result))
        .catch((error) => reportIpcError('setLauncherServer', error));
    },

    connectToLauncherServer: () => {
      ipcRenderer.send('desktop_connect_to_launcher_server');
    },

    getLauncherServer: (callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_get_launcher_server')
        .then((server) => callback(server))
        .catch((error) => reportIpcError('getLauncherServer', error));
    },

    startLauncherJobStatusStream: (jobId: string) => {
      ipcRenderer.send('desktop_start_launcher_job_status_stream', jobId);
    },

    stopLauncherJobStatusStream: (jobId: string) => {
      ipcRenderer.send('desktop_stop_launcher_job_status_stream', jobId);
    },

    startLauncherJobOutputStream: (jobId: string) => {
      ipcRenderer.send('desktop_start_launcher_job_output_stream', jobId);
    },

    stopLauncherJobOutputStream: (jobId: string) => {
      ipcRenderer.send('desktop_stop_launcher_job_output_stream', jobId);
    },

    controlLauncherJob: (jobId: string, operation: string) => {
      ipcRenderer.send('desktop_control_launcher_job', jobId, operation);
    },

    submitLauncherJob: (job: Record<string, unknown>) => {
      ipcRenderer.send('desktop_submit_launcher_job', job);
    },

    getJobContainerUser: () => {
      ipcRenderer.send('desktop_get_job_container_user');
    },

    validateJobsConfig: () => {
      ipcRenderer.send('desktop_validate_jobs_config');
    },

    getProxyPortNumber: (callback: VoidCallback<number>) => {
      ipcRenderer
        .invoke('desktop_get_proxy_port_number')
        .then((port) => callback(port))
        .catch((error) => reportIpcError('getProxyPortNumber', error));
    },

    signOut: () => {
      ipcRenderer.send('desktop_sign_out');
    },

    getStartupErrorInfo: (varName: string, callback: VoidCallback<string>) => {
      ipcRenderer
        .invoke('desktop_startup_error_info', varName)
        .then((info) => callback(info))
        .catch((error) => reportIpcError('getStartupErrorInfo', error));
    },

    showSplashScreen: () => {
      ipcRenderer.send('desktop_show_splash_screen');
    },

    detectRosetta: () => {
      ipcRenderer.send('desktop_detect_rosetta');
    },

    consoleLog: (output: string) => {
      ipcRenderer.send('desktop_console_log', output);
    },

    // pro-only start

    // pro-only end
  };
}
