/*
 * preload.js
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
const { contextBridge } = require('electron');
const { ipcRenderer } = require('electron')

/**
 * The preload script is run in the renderer before our GWT code and enables
 * setting up a bridge between the main process and the renderer process via
 * the contextBridge mechanism.
 * 
 * Code in here has access to powerful node.js and Electron APIs even though
 * the renderer itself is configured with node disabled and context isolation.
 * 
 * Be careful to only expose the exact APIs desired; DO NOT expose general-purpose
 * IPC objects, etc.
 * 
 * Actual implementation happens in the main process, reached via ipcRenderer,
 * and implemented in DesktopCallback class.
 */

contextBridge.exposeInMainWorld('desktop',  {
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
      .then(filename => callback(filename));
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

  showWordDoc: (path) => {
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

  prepareForSatelliteWindow: (name, x, y, width, height) => {
    ipcRenderer.send('desktop_prepare_for_satellite_window', name, x, y, width, height);
  },

  prepareForNamedWindow: (name, allowExternalNavigate, showToolbar) => {
    ipcRenderer.send('desktop_prepare_for_named_window', name, allowExternalNavigate, showToolbar);
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
      .then(result => callback(result));
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

  },

  setPendingQuit: (pendingQuit) => {

  },

  openProjectInNewWindow: (projectFilePath) => {
    window.alert('Not implemented');
  },

  openSessionInNewWindow: (workingDirectoryPath) => {

  },

  openTerminal: (terminalPath, workingDirectory, extraPathEntries, shellType) => {

  },

  getFixedWidthFontList: () => {
    return '';
  },

  getFixedWidthFont: () => {
    return '';
  },

  setFixedWidthFont: (font) => {
    return '';
  },

  getZoomLevels: () => {
    return '';
  },

  getZoomLevel: () => {
    return 1.0;
   },

  setZoomLevel: (zoomLevel) => {

  },
  
  zoomIn: () => {

  },

  zoomOut: () => {

  },

  zoomActualSize: () => {

  },
  
  setBackgroundColor: (rgbColor) => {

  },

  changeTitleBarColor: (red, green, blue)  => {

  },

  syncToEditorTheme: (isDark) => {

  },

  getEnableAccessibility: (callback) => { 
    callback(false); 
  },

  setEnableAccessibility: (enable) => {

  },

  getClipboardMonitoring: (callback) => {
    callback(false);
  },

  setClipboardMonitoring: (monitoring) => {

  },

  getIgnoreGpuBlacklist: () => {
    return true;
  },

  setIgnoreGpuBlacklist: (ignore) => {

  },

  getDisableGpuDriverBugWorkarounds: (callback) => {
    callback(true); 
  },

  setDisableGpuDriverBugWorkarounds: (disable) => {

  },

  showLicenseDialog: () => {

  },

  showSessionServerOptionsDialog: () => {

  },

  getInitMessages: (callback) => {
    callback('');
  },

  getLicenseStatusMessage: (callback) => {
    callback('');
  },

  allowProductUsage: (callback) => { 
    callback(true); 
  },

  getDesktopSynctexViewer: () => {
    return ''
  },

  externalSynctexPreview: (pdfPath, page) => {

  },

  externalSynctexView: (pdfFile,
                        srcFile,
                        line,
                        column) => {

  },

  supportsFullscreenMode: () => {
    return true;
   },

  toggleFullscreenMode: () => {

  },

  showKeyboardShortcutHelp: () => {

  },

  launchSession: (reload) => {

  },

  reloadZoomWindow: () => {

  },

  setTutorialUrl: (url) => {

  },
  
  setViewerUrl: (url) => {

  },

  reloadViewerZoomWindow: (url) => {

  },

  setShinyDialogUrl: (url) => {

  },

  getScrollingCompensationType: () => {
    return '';
  },

  isMacOS: () => {
    return true;
  },

  isCentOS: () => {
    return false;
  },

  setBusy: (busy) => {

  },

  setWindowTitle: (title) => {

  },

  installRtools: (version, installerPath) => {

  },

  getDisplayDpi: (callback) => {
    ipcRenderer
      .invoke('desktop_get_display_dpi')
      .then(dpi => callback(dpi));
  },

  onSessionQuit: () => {
    return ''
  },

  getSessionServer: (callback) => {
    callback({});
  },

  getSessionServers: (callback) => {
    callback([]);
  },

  reconnectToSessionServer: (sessionServerJson) => {

  },

  setLauncherServer: (sessionServerJson, callback) => {
    callback(false);
  },

  connectToLauncherServer: () => {

  },

  getLauncherServer: (callback) => {
    callback({});
  },

  startLauncherJobStatusStream: (jobId) => {

  },

  stopLauncherJobStatusStream: (jobId) => {

  },

  startLauncherJobOutputStream: (jobId) => {

  },

  stopLauncherJobOutputStream: (jobId) => {

  },

  controlLauncherJob: (jobId, operation) => {

  },

  submitLauncherJob: (job) => {

  },

  getJobContainerUser: () => {

  },

  validateJobsConfig: () => {

  },

  getProxyPortNumber: (callback) => {
    callback(-1); 
  },

  signOut: () => {

  },
});

// RDP-only
//contextBridge.exposeInMainWorld('remoteDesktop', {});

contextBridge.exposeInMainWorld('desktopInfo', {
  chromiumDevtoolsPort: () => { return 0; }
});

contextBridge.exposeInMainWorld('desktopMenuCallback', {
  beginMainMenu: () => {
    ipcRenderer.send('menu_begin_main');
  },

  beginMenu: (label) => {
    ipcRenderer.send('menu_begin', label);
  },

  addCommand: (cmdId, label, tooltip, shortcut, isChecked) => {},
  addSeparator: () => {},
  endMenu: () => {},
  endMainMenu: () => {},
  setCommandVisible: (commandId, visible) => {},
  setCommandEnabled: (commandId, enabled) => {},
  setCommandChecked: (commandId, checked) => {},
  setMainMenuEnabled: (enabled) => {},
  setCommandLabel: (commandId, label) => {},
});
