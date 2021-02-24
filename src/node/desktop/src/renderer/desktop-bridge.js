/*
 * desktop-bridge.js
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

const { ipcRenderer } = require('electron');

exports.getDesktopBridge = () => {
  return {
    browseUrl: (url) => {
      ipcRenderer.send('desktop_browse_url', url);
    },

    getOpenFileName: (caption,
      label,
      dir,
      filter,
      canChooseDirectories,
      focusOwner,
      callback) => {
      ipcRenderer
        .invoke('desktop_get_open_file_name', caption,
          label,
          dir,
          filter,
          canChooseDirectories,
          focusOwner)
        .then(result => {
          if (result.canceled) {
            callback('');
          } else {
            callback(result.filePaths[0]);
          }
        });
    },

    getSaveFileName: (caption,
      label,
      dir,
      defaultExtension,
      forceDefaultExtension,
      focusOwner,
      callback) => {
      ipcRenderer
        .invoke('desktop_get_save_file_name', caption,
          label,
          dir,
          defaultExtension,
          forceDefaultExtension,
          focusOwner)
        .then(filename => callback(filename));
    },

    getExistingDirectory: (caption,
      label,
      dir,
      focusOwner,
      callback) => {
      ipcRenderer
        .invoke('desktop_get_existing_directory', caption,
          label,
          dir,
          focusOwner)
        .then(directory => callback(directory));
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

    setClipboardText: (text) => {
      ipcRenderer.send('desktop_set_clipboard_text', text);
    },

    getClipboardText: (callback) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_text')
        .then(text => callback(text));
    },

    getClipboardUris: (callback) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_uris')
        .then(text => callback(text));
    },

    getClipboardImage: (callback) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_image')
        .then(text => callback(text));
    },

    setGlobalMouseSelection: (selection) => {
      ipcRenderer.send('desktop_set_global_mouse_selection', selection);
    },

    getGlobalMouseSelection: (callback) => {
      ipcRenderer
        .invoke('desktop_get_global_mouse_selection')
        .then(selection => callback(selection));
    },

    getCursorPosition: (callback) => {
      ipcRenderer
        .invoke('desktop_get_cursor_position')
        .then(position => callback(position));
    },

    doesWindowExistAtCursorPosition: (callback) => {
      ipcRenderer
        .invoke('desktop_does_window_exist_at_cursor_position')
        .then(exists => callback(exists));
    },

    onWorkbenchInitialized: (scratchPath) => {
      ipcRenderer.send('desktop_on_workbench_initialized', scratchPath);
    },

    showFolder: (path) => {
      ipcRenderer.send('desktop_show_folder', path);
    },

    showFile: (file) => {
      ipcRenderer.send('desktop_show_file', file);
    },

    showWordDoc: (wordDoc) => {
      ipcRenderer.send('desktop_show_word_doc', wordDoc);
    },

    showPptPresentation: (pptDoc) => {
      ipcRenderer.send('desktop_show_ppt_presentation', pptDoc);
    },

    showPDF: (path, pdfPage) => {
      ipcRenderer.send('desktop_show_pdf', path, pdfPage);
    },

    prepareShowWordDoc: () => {
      ipcRenderer.send('desktop_prepare_show_word_doc');
    },

    prepareShowPptPresentation: () => {
      ipcRenderer.send('desktop_prepare_show_ppt_presentation');
    },

    // R version selection currently Win32 only
    getRVersion: (callback) => {
      ipcRenderer
        .invoke('desktop_get_r_version')
        .then(rver => callback(rver));
    },

    chooseRVersion: (callback) => {
      ipcRenderer
        .invoke('desktop_choose_r_version')
        .then(rver => callback(rver));
    },

    devicePixelRatio: (callback) => {
      ipcRenderer
        .invoke('desktop_device_pixel_ratio')
        .then(ratio => callback(ratio));
    },

    openMinimalWindow: (name, url, width, height) => {
      ipcRenderer.send('desktop_open_minimal_window', name, url, width, height);
    },

    activateMinimalWindow: (name) => {
      ipcRenderer.send('desktop_activate_minimal_window', name);
    },

    activateSatelliteWindow: (name) => {
      ipcRenderer.send('desktop_activate_satellite_window', name);
    },

    prepareForSatelliteWindow: (name, x, y, width, height, callback) => {
      ipcRenderer.invoke('desktop_prepare_for_satellite_window', name, x, y, width, height)
        .then(() => callback());
    },

    prepareForNamedWindow: (name, allowExternalNavigate, showToolbar, callback) => {
      ipcRenderer.invoke('desktop_prepare_for_named_window', name, allowExternalNavigate, showToolbar)
        .then(() => callback());
    },

    closeNamedWindow: (name) => {
      ipcRenderer.send('desktop_close_named_window', name);
    },

    copyPageRegionToClipboard: (left, top, width, height) => {
      ipcRenderer.send('desktop_copy_page_region_to_clipboard', left, top, width, height);
    },

    exportPageRegionToFile: (targetPath, format, left, top, width, height) => {
      ipcRenderer.send('desktop_export_page_region_to_file', targetPath, format, left, top, width, height);
    },

    printText: (text) => {
      ipcRenderer.send('desktop_print_text', text);
    },

    paintPrintText: (printer) => {
      ipcRenderer.send('desktop_paint_print_text', printer);
    },

    printFinished: (result) => {
      ipcRenderer.send('desktop_print_finished', result);
    },

    supportsClipboardMetafile: (callback) => {
      ipcRenderer
        .invoke('desktop_supports_clipboard_metafile')
        .then(metafilesupport => callback(metafilesupport));
    },

    showMessageBox: (type,
      caption,
      message,
      buttons,
      defaultButton,
      cancelButton,
      callback) => {
      ipcRenderer
        .invoke('desktop_show_message_box', type, caption, message, buttons, defaultButton, cancelButton)
        .then(result => callback(result.response));
    },

    promptForText: (title,
      caption,
      defaultValue,
      type,
      rememberPasswordPrompt,
      rememberByDefault,
      selectionStart,
      selectionLength,
      okButtonCaption,
      callback) => {
      ipcRenderer
        .invoke('desktop_prompt_for_text', title, caption, defaultValue, type,
          rememberPasswordPrompt, rememberByDefault,
          selectionStart, selectionLength, okButtonCaption)
        .then(text => callback(text));
    },

    bringMainFrameToFront: () => {
      ipcRenderer.send('desktop_bring_main_frame_to_front');
    },

    bringMainFrameBehindActive: () => {
      ipcRenderer.send('desktop_bring_main_frame_behind_active');
    },

    desktopRenderingEngine: (callback) => {
      ipcRenderer
        .invoke('desktop_rendering_engine')
        .then(engine => callback(engine));
    },

    setDesktopRenderingEngine: (engine) => {
      ipcRenderer.send('desktop_set_desktop_rendering_engine', engine);
    },

    filterText: (text, callback) => {
      ipcRenderer
        .invoke('desktop_filter_text', text)
        .then(filtered => callback(text));
    },

    cleanClipboard: (stripHtml) => {
      ipcRenderer.send('desktop_clean_clipboard', stripHtml);
    },

    setPendingQuit: (pendingQuit) => {
      ipcRenderer.send('desktop_set_pending_quit', pendingQuit);
    },

    openProjectInNewWindow: (projectFilePath) => {
      ipcRenderer.send('desktop_open_project_in_new_window', projectFilePath);
    },

    openSessionInNewWindow: (workingDirectoryPath) => {
      ipcRenderer.send('desktop_open_session_in_new_window', workingDirectoryPath);
    },

    openTerminal: (terminalPath, workingDirectory, extraPathEntries, shellType) => {
      ipcRenderer.send('desktop_open_terminal', terminalPath, workingDirectory, extraPathEntries, shellType);
    },

    getFixedWidthFontList: (callback) => {
      ipcRenderer
        .invoke('desktop_get_fixed_width_font_list')
        .then(fonts => callback(fonts));
    },

    getFixedWidthFont: (callback) => {
      ipcRenderer
        .invoke('desktop_get_fixed_width_font')
        .then(font => callback(font));
    },

    setFixedWidthFont: (font) => {
      ipcRenderer.send('desktop_set_fixed_width_font', font);
    },

    getZoomLevels: (callback) => {
      ipcRenderer
        .invoke('desktop_get_zoom_levels')
        .then(levels => callback(levels));
    },

    getZoomLevel: (callback) => {
      ipcRenderer
        .invoke('desktop_get_zoom_level')
        .then(zoomLevel => callback(zoomLevel));
    },

    setZoomLevel: (zoomLevel) => {
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

    setBackgroundColor: (rgbColor) => {
      ipcRenderer.send('desktop_set_background_color', rgbColor);
    },

    changeTitleBarColor: (red, green, blue) => {
      ipcRenderer.send('desktop_change_title_bar_color', red, green, blue);
    },

    syncToEditorTheme: (isDark) => {
      ipcRenderer.send('desktop_sync_to_editor_theme', isDark);
    },

    getEnableAccessibility: (callback) => {
      ipcRenderer
        .invoke('desktop_get_enable_accessibility')
        .then(enabled => callback(enabled));
    },

    setEnableAccessibility: (enable) => {
      ipcRenderer.send('desktop_set_enable_accessibility', enable);
    },

    getClipboardMonitoring: (callback) => {
      ipcRenderer
        .invoke('desktop_get_clipboard_monitoring')
        .then(monitoring => callback(monitoring));
    },

    setClipboardMonitoring: (monitoring) => {
      ipcRenderer.send('desktop_set_clipboard_monitoring', monitoring);
    },

    getIgnoreGpuBlacklist: (callback) => {
      ipcRenderer
        .invoke('desktop_get_ignore_gpu_blacklist')
        .then(ignore => callback(ignore));
    },

    setIgnoreGpuBlacklist: (ignore) => {
      ipcRenderer.send('desktop_set_ignore_gpu_blacklist', ignore);
    },

    getDisableGpuDriverBugWorkarounds: (callback) => {
      ipcRenderer
        .invoke('desktop_get_disable_gpu_driver_bug_workarounds')
        .then(disabled => callback(disabled));
    },

    setDisableGpuDriverBugWorkarounds: (disable) => {
      ipcRenderer.send('desktop_set_disable_gpu_driver_bug_workarounds', disable);
    },

    showLicenseDialog: () => {
      ipcRenderer.send('desktop_show_license_dialog');
    },

    showSessionServerOptionsDialog: () => {
      ipcRenderer.send('desktop_show_session_server_options_dialog');
    },

    getInitMessages: (callback) => {
      ipcRenderer
        .invoke('desktop_get_init_messages')
        .then(messages => callback(messages));
    },

    getLicenseStatusMessage: (callback) => {
      ipcRenderer
        .invoke('desktop_get_license_status_message')
        .then(message => callback(message));
    },

    allowProductUsage: (callback) => {
      ipcRenderer
        .invoke('desktop_allow_product_usage')
        .then(allow => callback(allow));
    },

    getDesktopSynctexViewer: (callback) => {
      ipcRenderer
        .invoke('desktop_get_desktop_synctex_viewer')
        .then(viewer => callback(viewer));
    },

    externalSynctexPreview: (pdfPath, page) => {
      ipcRenderer.send('desktop_external_synctex_preview', pdfPath, page);
    },

    externalSynctexView: (pdfFile,
      srcFile,
      line,
      column) => {
      ipcRenderer.send('desktop_external_synctex_view', pdfFile, srcFile, line, column);
    },

    supportsFullscreenMode: (callback) => {
      ipcRenderer
        .invoke('desktop_supports_fullscreen_mode')
        .then(supportsFullScreen => callback(supportsFullScreen));
    },

    toggleFullscreenMode: () => {
      ipcRenderer.send('desktop_toggle_fullscreen_mode');
    },

    showKeyboardShortcutHelp: () => {
      ipcRenderer.send('desktop_show_keyboard_shortcut_help');
    },

    launchSession: (reload) => {
      ipcRenderer.send('desktop_launch_session', reload);
    },

    reloadZoomWindow: () => {
      ipcRenderer.send('desktop_reload_zoom_window');
    },

    setTutorialUrl: (url) => {
      ipcRenderer.send('desktop_set_tutorial_url', url);
    },

    setViewerUrl: (url) => {
      ipcRenderer.send('desktop_set_viewer_url', url);
    },

    reloadViewerZoomWindow: (url) => {
      ipcRenderer.send('desktop_reload_viewer_zoom_window', url);
    },

    setShinyDialogUrl: (url) => {
      ipcRenderer.send('desktop_set_shiny_dialog_url', url);
    },

    getScrollingCompensationType: (callback) => {
      ipcRenderer
        .invoke('desktop_get_scrolling_compensation_type')
        .then(compensationType => callback(compensationType));
    },

    isMacOS: (callback) => {
      ipcRenderer
        .invoke('desktop_is_macos')
        .then(isMac => callback(isMac));
    },

    isCentOS: (callback) => {
      ipcRenderer
        .invoke('desktop_is_centos')
        .then(isCentOS => callback(isCentOS));
    },

    setBusy: (busy) => {
      ipcRenderer.send('desktop_set_busy', busy);
    },

    setWindowTitle: (title) => {
      ipcRenderer.send('desktop_set_window_title', title);
    },

    installRtools: (version, installerPath) => {
      ipcRenderer.send('desktop_install_rtools', version, installerPath);
    },

    getDisplayDpi: (callback) => {
      ipcRenderer
        .invoke('desktop_get_display_dpi')
        .then(dpi => callback(dpi));
    },

    onSessionQuit: () => {
      ipcRenderer.send('desktop_on_session_quit');
    },

    getSessionServer: (callback) => {
      ipcRenderer
        .invoke('desktop_get_session_server'
          .then(server => callback(server)));
    },

    getSessionServers: (callback) => {
      ipcRenderer
        .invoke('desktop_get_session_servers')
        .then(servers => callback(servers));
    },

    reconnectToSessionServer: (sessionServerJson) => {
      ipcRenderer.send('desktop_reconnect_to_session_server', sessionServerJson);
    },

    setLauncherServer: (sessionServerJson, callback) => {
      ipcRenderer
        .invoke('desktop_set_launcher_server', sessionServerJson)
        .then(result => callback(result));
    },

    connectToLauncherServer: () => {
      ipcRenderer.send('desktop_connect_to_launcher_server');
    },

    getLauncherServer: (callback) => {
      ipcRenderer
        .invoke('desktop_get_launcher_server')
        .then(server => callback(server));
    },

    startLauncherJobStatusStream: (jobId) => {
      ipcRenderer.send('desktop_start_launcher_job_status_stream', jobId);
    },

    stopLauncherJobStatusStream: (jobId) => {
      ipcRenderer.send('desktop_stop_launcher_job_status_stream', jobId);
    },

    startLauncherJobOutputStream: (jobId) => {
      ipcRenderer.send('desktop_start_launcher_job_output_stream', jobId);
    },

    stopLauncherJobOutputStream: (jobId) => {
      ipcRenderer.send('desktop_stop_launcher_job_output_stream', jobId);
    },

    controlLauncherJob: (jobId, operation) => {
      ipcRenderer.send('desktop_control_launcher_job', jobId, operation);
    },

    submitLauncherJob: (job) => {
      ipcRenderer.send('desktop_submit_launcher_job', job);
    },

    getJobContainerUser: () => {
      ipcRenderer.send('desktop_get_job_container_user');
    },

    validateJobsConfig: () => {
      ipcRenderer.send('desktop_validate_jobs_config');
    },

    getProxyPortNumber: (callback) => {
      ipcRenderer
        .invoke('desktop_get_proxy_port_number')
        .then(port => callback(port));
    },

    signOut: () => {
      ipcRenderer.send('desktop_sign_out');
    },
  };
};
