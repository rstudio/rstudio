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
    ipcRenderer.invoke('desktop_get_existing_directory', caption,
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
    callback(false);
  },

  onWorkbenchInitialized: (scratchPath) => {

  },

  showFolder: (path) => {

  },

  showFile: (path) => {

  },

  showWordDoc: (path) => {

  },

  showPptPresentation: (path) => {},

  showPDF: (path, pdfPage) => {},

  prepareShowWordDoc: () => {},

  prepareShowPptPresentation: () => {},

  // R version selection currently Win32 only
  getRVersion: (callback) => { callback(''); },

  chooseRVersion: (callback) => { callback(''); },

  devicePixelRatio: () => 1.0,

  openMinimalWindow: (name, url, width, height) => {window.alert('Not implemented'); },

  activateMinimalWindow: (name) => {},

  activateSatelliteWindow: (name) => {},

  prepareForSatelliteWindow: (name, x, y, width, height) => {},

  prepareForNamedWindow: (name, allowExternalNavigate, showToolbar) => {},

  closeNamedWindow: (name) => {},

  copyPageRegionToClipboard: (left, top, width, height) => {},

  exportPageRegionToFile: (targetPath, format, left, top, width, height) => {},

  printText: (text) => {},

  paintPrintText: (printer) => {},

  printFinished: (result) => {},

  supportsClipboardMetafile: (callback) => { callback(false); },

  showMessageBox: (type, caption, message, buttons, defaultButton, cancelButton, callback) => {
    window.alert(message);
    callback(1.0);
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
                    window.alert('Not implemented');
                    callback('');
                  },

  bringMainFrameToFront: () => {},
  bringMainFrameBehindActive: () => {},

  desktopRenderingEngine: (callback) => { callback(''); },

  setDesktopRenderingEngine: (engine) => {},

  filterText: (text) => text,

  cleanClipboard: (stripHtml) => {},

  setPendingQuit: (pendingQuit) => {},

  openProjectInNewWindow: (projectFilePath) => { window.alert('Not implemented'); },

  openSessionInNewWindow: (workingDirectoryPath) => {},

  openTerminal: (terminalPath, workingDirectory, extraPathEntries, shellType) => {},

  getFixedWidthFontList: () => '',

  getFixedWidthFont: () => '',

  setFixedWidthFont: (font) => '',

  getZoomLevels: () => '',

  getZoomLevel: () => 1.0,

  setZoomLevel: (zoomLevel) => {},
  
  zoomIn: () => {},

  zoomOut: () => {},

  zoomActualSize: () => {},
  
  setBackgroundColor: (rgbColor) => {},

  changeTitleBarColor: (red, green, blue)  => {},

  syncToEditorTheme: (isDark) => {},

  getEnableAccessibility: (callback) => { callback(false); },

  setEnableAccessibility: (enable) => {},

  getClipboardMonitoring: (callback) => { callback(false); },

  setClipboardMonitoring: (monitoring) => {},

  getIgnoreGpuBlacklist: () => true,

  setIgnoreGpuBlacklist: (ignore) => {},

  getDisableGpuDriverBugWorkarounds: (callback) => { callback(true); },

  setDisableGpuDriverBugWorkarounds: (disable) => {},

  showLicenseDialog: () => {},

  showSessionServerOptionsDialog: () => {},

  getInitMessages: (callback) => { callback(''); },

  getLicenseStatusMessage: (callback) => { callback(''); },

  allowProductUsage: (callback) => { callback(true); },

  getDesktopSynctexViewer: () => '',

  externalSynctexPreview: (pdfPath, page) => {},

  externalSynctexView: (pdfFile,
                        srcFile,
                        line,
                        column) => {},

  supportsFullscreenMode: () => true,

  toggleFullscreenMode: () => {},

  showKeyboardShortcutHelp: () => {},

  launchSession: (reload) => {},

  reloadZoomWindow: () => {},

  setTutorialUrl: (url) => {},
  
  setViewerUrl: (url) => {},

  reloadViewerZoomWindow: (url) => {},

  setShinyDialogUrl: (url) => {},

  getScrollingCompensationType: () => '',

  isMacOS: () => true,

  isCentOS: () => false,

  setBusy: (busy) => {},

  setWindowTitle: (title) => {},

  installRtools: (version, installerPath) => {},

  getDisplayDpi: (callback) => {
    ipcRenderer.invoke('desktop_get_display_dpi').then(dpi => callback(dpi));
  },

  onSessionQuit: () => '',

  getSessionServer: (callback) => { callback({}); },

  getSessionServers: (callback) => { callback([]); },

  reconnectToSessionServer: (sessionServerJson) => {},

  setLauncherServer: (sessionServerJson, callback) => { callback(false); },

  connectToLauncherServer: () => {},

  getLauncherServer: (callback) => { callback({}); },

  startLauncherJobStatusStream: (jobId) => {},

  stopLauncherJobStatusStream: (jobId) => {},

  startLauncherJobOutputStream: (jobId) => {},

  stopLauncherJobOutputStream: (jobId) => {},

  controlLauncherJob: (jobId, operation) => {},

  submitLauncherJob: (job) => {},

  getJobContainerUser: () => {},

  validateJobsConfig: () => {},

  getProxyPortNumber: (callback) => { callback(-1); },

  signOut: () => {},
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
  endMainMenu: () => {console.log('endMainMenu')},
  setCommandVisible: (commandId, visible) => {},
  setCommandEnabled: (commandId, enabled) => {},
  setCommandChecked: (commandId, checked) => {},
  setMainMenuEnabled: (enabled) => {},
  setCommandLabel: (commandId, label) => {},
});
