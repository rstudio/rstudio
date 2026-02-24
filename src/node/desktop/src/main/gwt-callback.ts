/*
 * gwt-callback.ts
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

import { exec, execSync } from 'child_process';
import {
  app,
  nativeTheme,
  BrowserWindow,
  clipboard,
  ipcMain,
  Rectangle,
  screen,
  shell,
  webFrameMain,
  dialog,
  FileFilter,
} from 'electron';
import { IpcMainEvent, MessageBoxOptions, OpenDialogOptions, SaveDialogOptions } from 'electron/main';
import EventEmitter from 'events';
import { existsSync, statSync, writeFileSync } from 'fs';
import { platform, release } from 'os';
import i18next from 'i18next';
import path, { dirname } from 'path';
import { pathToFileURL } from 'url';
import { FilePath, tempFilename } from '../core/file-path';
import { normalizeSeparatorsNative } from '../ui/utils';
import { logger } from '../core/logger';
import { isCentOS } from '../core/system';
import { resolveTemplateVar } from '../core/template-filter';
import desktop from '../native/desktop.node';
import { ChooseRModalWindow } from '../ui/widgets/choose-r';
import { appState } from './app-state';
import { findDefault32Bit, findDefault64Bit, findRInstallationsWin32 } from './detect-r';
import { GwtWindow } from './gwt-window';
import { MainWindow } from './main-window';
import { openMinimalWindow } from './minimal-window';
import { defaultFonts, ElectronDesktopOptions } from './preferences/electron-desktop-options';
import {
  parseFilter,
  findRepoRoot,
  getAppPath,
  handleLocaleCookies,
  resolveAliasedPath,
  raiseAndActivateWindow,
} from './utils';
import { activateWindow, focusedWebContents } from './window-utils';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { userHomePathString } from '../core/user';
import { detectRosetta } from './detect-rosetta';
import { showPersistentSplashScreen } from './splash-screen';

export enum PendingQuit {
  PendingQuitNone,
  PendingQuitAndExit,
  PendingQuitAndRestart,
  PendingQuitRestartAndReload,
}

function formatSelectedVersionForUi(rBinDir: string) {
  // binDir will have format <R_HOME>/bin/<arch>,
  // so we need two dirname()s to get the home path
  const rHome = dirname(dirname(rBinDir));

  // return formatted string as appropriate
  if (rBinDir.endsWith('x64')) {
    return `[64-bit] ${rHome}`;
  } else if (rBinDir.endsWith('i386')) {
    return `[32-bit] ${rHome}`;
  } else {
    return rHome;
  }
}

/**
 * This is the main-process side of the GwtCallbacks; dispatched from renderer processes
 * via the ContextBridge.
 */
export class GwtCallback extends EventEmitter {
  static WORKBENCH_INITIALIZED = 'gwt-callback-workbench_initialized';
  static SESSION_QUIT = 'gwt-callback-session_quit';

  initialized = false;
  pendingQuit: number = PendingQuit.PendingQuitNone;

  private hasFontConfig = false;
  private owners = new Set<GwtWindow>();

  // Info used by the "session failed to load" error page (error.html)
  errorPageData = new Map<string, string>();

  monospaceFonts: string[] = [];
  proportionalFonts: string[] = [];

  /**
   * Opens a file in the main window and activates it.
   * This should be called from the main process (e.g., application.ts, args-manager.ts).
   * Fixes issue #16740 by ensuring files always open in the main window, not satellite windows.
   */
  async openFileInMainWindow(filePath: string): Promise<void> {
    if (!filePath) {
      return;
    }

    try {
      const webContent = this.mainWindow.window.webContents;
      await webContent.executeJavaScript(`window.desktopHooks.openFile(${JSON.stringify(filePath)})`);
      raiseAndActivateWindow(this.mainWindow.window);
    } catch (error: unknown) {
      logger().logError(safeError(error));
    }
  }

  getFontsLinux(monospace: boolean): string[] {
    const command = monospace
      ? 'fc-list :spacing=mono family | sort'
      : 'fc-list :lang=en family | grep -i sans | grep -iv mono | sort';

    const result = execSync(command, { encoding: 'utf-8' });
    return result.trim().split('\n');
  }

  constructor(public mainWindow: MainWindow) {
    super();
    this.owners.add(mainWindow);

    if (process.platform === 'linux') {
      try {
        const _result = execSync('/usr/bin/which fc-list');
        this.hasFontConfig = true;
      } catch (error) {
        logger().logError(error);
      }
    }

    try {
      const queryFonts = getenv('RSTUDIO_QUERY_FONTS');
      if (queryFonts !== '0' && queryFonts.toLowerCase() !== 'false') {
        if (this.hasFontConfig) {
          // Linux with fontconfig
          this.monospaceFonts = this.getFontsLinux(true);
          this.proportionalFonts = this.getFontsLinux(false);
        } else if (platform() === 'win32') {
          this.monospaceFonts = desktop.win32ListMonospaceFonts();
          // Windows doesn't have a proportional font list API
          this.proportionalFonts = [];
        } else if (platform() === 'darwin') {
          // macOS: single-pass enumeration for both font types
          const fonts = desktop.macOSListFonts();
          this.monospaceFonts = fonts.monospace.sort((a, b) => a.localeCompare(b));
          this.proportionalFonts = fonts.proportional.sort((a, b) => a.localeCompare(b));
        }
      }
    } catch (err: unknown) {
      logger().logError(safeError(err));
    }

    ipcMain.on('desktop_write_stdout', (event, output) => {
      console.log(output);
    });

    ipcMain.on('desktop_write_stderr', (event, output) => {
      console.log(output);
    });

    ipcMain.on('desktop_browse_url', (event, url: string) => {
      // shell.openExternal() seems unreliable on Windows
      // https://github.com/electron/electron/issues/31347
      if (process.platform === 'win32' && url.startsWith('file:///')) {
        const path = decodeURI(url).substring('file:///'.length).replaceAll('/', '\\');
        desktop.openExternal(path);
      } else {
        void shell.openExternal(url);
      }
    });

    ipcMain.on('desktop_open_file', async (_event, filePath: string) => {
      // Delegate to the shared method that handles opening files in the main window
      await this.openFileInMainWindow(filePath);
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
        console.log('desktop_get_open_file_name');
        const openDialogOptions: OpenDialogOptions = {
          title: caption,
          defaultPath: normalizeSeparatorsNative(resolveAliasedPath(dir)),
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
          openDialogOptions.filters = parseFilter(filter);
        }

        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }
        if (focusedWindow) {
          return appState().modalTracker.trackElectronModalAsync(async () =>
            dialog.showOpenDialog(focusedWindow!, openDialogOptions),
          );
        } else {
          return appState().modalTracker.trackElectronModalAsync(async () => dialog.showOpenDialog(openDialogOptions));
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
        let defaultPath = normalizeSeparatorsNative(resolveAliasedPath(dir));

        // On macOS with Electron 36+, when defaultPath is just a directory and we have a defaultExtension,
        // provide a complete filename to avoid extension truncation issues (e.g., ".cpp" becoming ".cp")
        // https://github.com/rstudio/rstudio/issues/16444, https://github.com/electron/electron/issues/48332
        if (defaultExtension && process.platform === 'darwin') {
          const endsWithSeparator = defaultPath.endsWith('/');
          const parsedPath = path.parse(defaultPath);
          const hasNoFilename = !parsedPath.base || parsedPath.base === '.' || parsedPath.base === '..';
          const isExistingDir = existsSync(defaultPath) && statSync(defaultPath).isDirectory();

          if (endsWithSeparator || hasNoFilename || isExistingDir) {
            // Append a default filename with the extension
            defaultPath = path.join(defaultPath, `Untitled${defaultExtension}`);
          }
        }

        const saveDialogOptions: SaveDialogOptions = {
          title: caption,
          defaultPath: defaultPath,
          buttonLabel: label,
        };
        logger().logDebug(`Using path: ${saveDialogOptions.defaultPath}`);

        const filters: FileFilter[] = [{ name: i18next.t('common.allFiles'), extensions: ['*'] }];
        if (defaultExtension) {
          const extension = defaultExtension.replace('.', '');
          filters.unshift({ name: extension, extensions: [extension] });
        }
        saveDialogOptions['filters'] = filters;
        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }
        if (focusedWindow) {
          return appState().modalTracker.trackElectronModalAsync(async () =>
            dialog.showSaveDialog(focusedWindow!, saveDialogOptions),
          );
        } else {
          return appState().modalTracker.trackElectronModalAsync(async () => dialog.showSaveDialog(saveDialogOptions));
        }
      },
    );

    ipcMain.handle(
      'desktop_get_existing_directory',
      async (event, caption: string, label: string, dir: string, focusOwner: boolean) => {
        const openDialogOptions: OpenDialogOptions = {
          title: caption,
          defaultPath: normalizeSeparatorsNative(resolveAliasedPath(dir)),
          buttonLabel: label,
          properties: ['openDirectory', 'createDirectory', 'promptToCreate'],
        };

        let focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusOwner) {
          focusedWindow = this.getSender('desktop_open_minimal_window', event.processId, event.frameId).window;
        }

        if (focusedWindow) {
          return appState().modalTracker.trackElectronModalAsync(async () =>
            dialog.showOpenDialog(focusedWindow!, openDialogOptions),
          );
        } else {
          return appState().modalTracker.trackElectronModalAsync(async () => dialog.showOpenDialog(openDialogOptions));
        }
      },
    );

    ipcMain.handle('desktop_get_user_home_path', () => {
      return userHomePathString();
    });

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
      if (!clipboard.availableFormats().includes('image/png')) {
        return '';
      }

      // read image from clipboard
      const image = clipboard.readImage('clipboard');
      const pngData = image.toPNG();

      const scratchDir = appState().scratchTempDir(new FilePath('/tmp'));
      const tempPathName = path.join(scratchDir.getAbsolutePath(), 'rstudio-clipboard');

      const tempPath = new FilePath(tempPathName);
      tempPath.ensureDirectorySync();

      const pngPath = path.join(tempPathName, tempFilename('png', 'image').getFilename());

      // write image to file
      writeFileSync(pngPath, pngData);

      // return file path
      return pngPath;
    });

    ipcMain.on('desktop_set_global_mouse_selection', (event, selection: string) => {
      clipboard.writeText(selection, 'selection');
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
      this.initialized = true;
      this.emit(GwtCallback.WORKBENCH_INITIALIZED);
      appState().setScratchTempDir(new FilePath(scratchPath));
    });

    ipcMain.on('desktop_show_folder', (event, path: string) => {
      shell.openPath(resolveAliasedPath(normalizeSeparatorsNative(path))).catch((value) => {
        console.log('error:', value);
        logger().logErrorMessage(value);
      });
    });

    function showFileInSystemViewer(file: string) {
      shell.openPath(resolveAliasedPath(normalizeSeparatorsNative(file))).catch((value) => {
        console.log('error:', value);
        logger().logErrorMessage(value);
      });
    }

    ipcMain.on('desktop_show_file', (event, file: string) => {
      showFileInSystemViewer(file);
    });

    ipcMain.on('desktop_beep', () => {
      shell.beep();
    });

    ipcMain.on('desktop_show_word_doc', (event, wordDoc: string) => {
      showFileInSystemViewer(wordDoc);
    });

    ipcMain.on('desktop_show_ppt_presentation', (event, pptDoc: string) => {
      showFileInSystemViewer(pptDoc);
    });

    ipcMain.on('desktop_show_pdf', (_event, path: string, _pdfPage: string) => {
      // TODO: when desktop_external_synctex_view is implemented, use synctex viewer as appropriate
      // pdfPage is only relevant for synctex
      showFileInSystemViewer(path);
    });

    ipcMain.on('desktop_prepare_show_word_doc', () => {
      // if possible, close most recently rendered docx item
      return '';
    });

    ipcMain.on('desktop_prepare_show_ppt_presentation', () => {
      // if possible, close most recently rendered pptx item
      return '';
    });

    ipcMain.handle('desktop_get_r_version', () => {
      const options = ElectronDesktopOptions();

      if (options.useDefault32BitR()) {
        const rHomeDir = findDefault32Bit();
        if (rHomeDir) {
          return `[32-bit] ${rHomeDir} [Default]`;
        }
      }

      if (options.useDefault64BitR()) {
        const rHomeDir = findDefault64Bit();
        if (rHomeDir) {
          return `[64-bit] ${rHomeDir} [Default]`;
        }
      }

      const rBinDir = options.rBinDir();
      return formatSelectedVersionForUi(rBinDir);
    });

    ipcMain.handle('desktop_choose_r_version', async () => {
      // discover available R installations
      const rInstalls = findRInstallationsWin32();
      if (rInstalls.length === 0) {
        logger().logInfo('No R installations found via registry or common R install locations.');
      }

      // ask the user what version of R they'd like to use
      const chooseRDialog = new ChooseRModalWindow(rInstalls, mainWindow.window);
      void handleLocaleCookies(chooseRDialog);

      const [data, error] = await chooseRDialog.showModal();
      if (error) {
        logger().logError(error);
        return '';
      }

      // if the dialog was cancelled, the path may be null
      if (data == null || data.binaryPath == null) {
        return '';
      }

      // save options from dialog result
      const options = ElectronDesktopOptions();
      const path = data.binaryPath as string;
      options.setUseDefault32BitR(data.useDefault32BitR || false);
      options.setUseDefault64BitR(data.useDefault64BitR || false);
      options.setRExecutablePath(path);

      // return a formatted string for the client
      const rBinDir = dirname(path);
      logger().logDebug(`Using R: ${rBinDir}`);
      return formatSelectedVersionForUi(rBinDir);
    });

    ipcMain.on(
      'desktop_open_minimal_window',
      async (event: IpcMainEvent, name: string, url: string, width: number, height: number) => {
        // handle some internal chrome urls specially
        if (url === 'chrome://gpu' || url === 'chrome://accessibility') {
          const window = new BrowserWindow({
            autoHideMenuBar: true,
            webPreferences: { sandbox: true },
            acceptFirstMouse: true,
          });
          window.removeMenu(); // this isn't permanent but sufficient for these internal pages

          // ensure window can be closed with Ctrl+W (Cmd+W on macOS)
          window.webContents.on('before-input-event', (event, input) => {
            const ctrlOrMeta = process.platform === 'darwin' ? input.meta : input.control;
            if (ctrlOrMeta && input.key.toLowerCase() === 'w') {
              event.preventDefault();
              window.close();
            }
          });

          return window.loadURL(url);
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
          allowExternalNavigate: false,
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
          mainWindow: this.mainWindow,
        });
      },
    );

    ipcMain.on('desktop_set_gwt_num_modals_showing', (_event, gwtModalsShowing: number) => {
      appState().modalTracker.setNumGwtModalsShowing(gwtModalsShowing);
    });

    ipcMain.handle(
      'desktop_copy_page_region_to_clipboard',
      async (_event, x: number, y: number, width: number, height: number) => {
        try {
          const rect: Rectangle = { x, y, width, height };
          const image = await this.mainWindow.window.capturePage(rect);
          clipboard.writeImage(image);
        } catch (e: unknown) {
          logger().logError(e);
        }
      },
    );

    ipcMain.handle('desktop_copy_image_at_xy_to_clipboard', (_event, x: number, y: number) => {
      const focusedWindow = BrowserWindow.getFocusedWindow();
      if (focusedWindow?.webContents) {
        focusedWindow.webContents.copyImageAt(x, y);
      } else {
        logger().logError(`Failed to copy image at x: ${x}, y: ${y} to clipboard`);
      }
    });

    ipcMain.handle(
      'desktop_export_page_region_to_file',
      async (event, targetPath, format, left, top, width, height) => {
        try {
          const rect: Rectangle = { x: left, y: top, width, height };
          targetPath = resolveAliasedPath(targetPath);
          const image = await this.mainWindow.window.capturePage(rect);
          const buffer = format == 'jpeg' ? image.toJPEG(100) : image.toPNG();
          writeFileSync(targetPath, buffer);
        } catch (e: unknown) {
          logger().logError(e);
        }
      },
    );

    ipcMain.handle('desktop_supports_clipboard_metafile', () => {
      return process.platform === 'win32';
    });

    ipcMain.handle(
      'desktop_show_message_box',
      async (event, type, caption, message, buttons, _defaultButton, _cancelButton) => {
        let openDialogOptions: MessageBoxOptions;
        if (process.platform === 'darwin') {
          openDialogOptions = {
            type: this.convertMessageBoxType(type),
            message: caption,
            detail: message,
            cancelId: _cancelButton,
            defaultId: _defaultButton,
            buttons: this.convertButtons(buttons),
          };
        } else {
          openDialogOptions = {
            type: this.convertMessageBoxType(type),
            title: caption,
            cancelId: _cancelButton,
            defaultId: _defaultButton,
            message: message,
            buttons: this.convertButtons(buttons),
          };
        }

        const focusedWindow = BrowserWindow.getFocusedWindow();
        if (focusedWindow) {
          return appState().modalTracker.trackElectronModalAsync(async () =>
            dialog.showMessageBox(focusedWindow, openDialogOptions),
          );
        } else {
          return appState().modalTracker.trackElectronModalAsync(async () => dialog.showMessageBox(openDialogOptions));
        }
      },
    );

    ipcMain.on('desktop_bring_main_frame_to_front', () => {
      this.mainWindow.window.focus();
    });

    ipcMain.on('desktop_bring_main_frame_behind_active', () => {
      const mainWindow = this.mainWindow.window;
      const activeWindow = BrowserWindow.getFocusedWindow();

      // bring main window under active window by focusing main window then back to active
      if (activeWindow && mainWindow !== activeWindow) {
        mainWindow.show();
        activeWindow.focus();
      }
    });

    ipcMain.handle('desktop_rendering_engine', () => {
      return ElectronDesktopOptions().renderingEngine();
    });

    ipcMain.on('desktop_set_desktop_rendering_engine', (event, engine) => {
      ElectronDesktopOptions().setRenderingEngine(engine);
    });

    ipcMain.on('desktop_clean_clipboard', (event, stripHtml) => {
      desktop.cleanClipboard(stripHtml);
    });

    ipcMain.handle('desktop_set_pending_quit', (event, pendingQuit: number) => {
      this.pendingQuit = pendingQuit;
    });

    ipcMain.on('desktop_set_project_directory', (event, projectDirectory) => {
      appState().projectDirectory = resolveAliasedPath(projectDirectory);
    });

    ipcMain.on('desktop_open_project_in_new_window', (event, projectFilePath) => {
      this.mainWindow.launchRStudio({
        projectFilePath: resolveAliasedPath(projectFilePath),
      });
    });

    ipcMain.on('desktop_open_session_in_new_window', (event, workingDirectoryPath) => {
      this.mainWindow.launchRStudio({
        workingDirectory: resolveAliasedPath(workingDirectoryPath),
      });
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
        defaultFonts = ['Lucida Sans', 'DejaVu Sans', 'Noto Sans', 'Lucida Grande', 'Segoe UI', 'Verdana', 'Helvetica'];
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

    ipcMain.on('desktop_set_background_color', (_event, _rgbColor) => {
      /**/
    });

    ipcMain.on('desktop_change_title_bar_color', (_event, _red, _green, _blue) => {
      /**/
    });

    ipcMain.on('desktop_sync_to_editor_theme', (_event, isDark: boolean) => {
      nativeTheme.themeSource = isDark ? 'dark' : 'light';
    });

    ipcMain.on('desktop_set_mousewheel_zoom_enabled', (_event, enabled: boolean) => {
      // Broadcast to all windows
      for (const window of BrowserWindow.getAllWindows()) {
        window.webContents.send('desktop_set_mousewheel_zoom_enabled', enabled);
      }
    });

    ipcMain.on('desktop_set_mousewheel_zoom_debounce', (_event, zoomDebounceMs: number) => {
      // Broadcast to all windows
      for (const window of BrowserWindow.getAllWindows()) {
        window.webContents.send('desktop_set_mousewheel_zoom_debounce', zoomDebounceMs);
      }
    });

    ipcMain.handle('desktop_get_enable_accessibility', () => {
      return ElectronDesktopOptions().accessibility();
    });

    ipcMain.on('desktop_set_enable_accessibility', (_event, enable) => {
      ElectronDesktopOptions().setAccessibility(enable);
    });

    ipcMain.handle('desktop_get_enable_splash_screen', () => {
      return ElectronDesktopOptions().enableSplashScreen();
    });

    ipcMain.on('desktop_set_enable_splash_screen', (_event, enable) => {
      ElectronDesktopOptions().setEnableSplashScreen(enable);
    });

    ipcMain.on('desktop_set_autohide_menubar', (_event, autohide: boolean) => {
      this.mainWindow.window.setAutoHideMenuBar(autohide);
      this.mainWindow.window.setMenuBarVisibility(!autohide);
    });

    ipcMain.on('desktop_set_disable_renderer_accessibility', (_event, disable) => {
      ElectronDesktopOptions().setDisableRendererAccessibility(disable);
    });

    ipcMain.handle('desktop_get_ignore_gpu_exclusion_list', (_event, _ignore) => {
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

    ipcMain.handle('desktop_get_init_messages', () => {
      return '';
    });

    ipcMain.handle('desktop_get_desktop_synctex_viewer', () => {
      GwtCallback.unimpl('desktop_get_desktop_synctex_viewer');
      return '';
    });

    ipcMain.on('desktop_external_synctex_preview', (_event, _pdfPath, _page) => {
      GwtCallback.unimpl('desktop_external_synctex_preview');
    });

    ipcMain.on('desktop_external_synctex_view', (_event, _pdfFile, _srcFile, _line, _column) => {
      GwtCallback.unimpl('desktop_external_synctex_view');
    });

    ipcMain.handle('desktop_supports_fullscreen_mode', () => {
      return process.platform === 'darwin';
    });

    ipcMain.on('desktop_toggle_fullscreen_mode', () => {
      this.mainWindow.window.fullScreen = !this.mainWindow.window.fullScreen;
    });

    ipcMain.on('desktop_show_keyboard_shortcut_help', () => {
      let docUrl: URL;
      if (app.isPackaged) {
        docUrl = pathToFileURL(path.join(getAppPath(), 'www', 'docs', 'keyboard.htm'));
      } else {
        // dev build scenario
        docUrl = pathToFileURL(
          new FilePath(findRepoRoot()).completeChildPath('src/gwt/www/docs/keyboard.htm').getAbsolutePath(),
        );
      }
      void shell.openExternal(docUrl.toString());
    });

    ipcMain.on('desktop_launch_session', (_event, reload) => {
      this.mainWindow.launchSession(reload);
    });

    ipcMain.on('desktop_reload_zoom_window', () => {
      const browser = appState().windowTracker.getWindow('_rstudio_zoom');
      if (browser) {
        browser.window.webContents.reload();
      }
    });

    ipcMain.on('desktop_set_tutorial_url', (event, url) => {
      this.getSender('desktop_set_tutorial_url', event.processId, event.frameId).setTutorialUrl(url);
    });

    ipcMain.on('desktop_set_viewer_url', (event, url) => {
      this.getSender('desktop_set_viewer_url', event.processId, event.frameId).setViewerUrl(url);
    });

    ipcMain.on('desktop_set_presentation_url', (event, url) => {
      this.getSender('desktop_set_presentation_url', event.processId, event.frameId).setPresentationUrl(url);
    });

    ipcMain.on('desktop_reload_viewer_zoom_window', (_event, url) => {
      const browser = appState().windowTracker.getWindow('_rstudio_viewer_zoom');
      if (browser) {
        void browser.window.webContents.loadURL(url);
      }
    });

    ipcMain.on('desktop_set_shiny_dialog_url', (event, url) => {
      this.getSender('desktop_set_shiny_dialog_url', event.processId, event.frameId).setShinyDialogUrl(url);
    });

    ipcMain.handle('desktop_allow_navigation', (event, url) => {
      return this.getSender('desktop_allow_navigation', event.processId, event.frameId).allowNavigation(url);
    });

    ipcMain.handle('desktop_is_macos', () => {
      return process.platform === 'darwin';
    });

    ipcMain.handle('desktop_is_centos', () => {
      return isCentOS();
    });

    ipcMain.on('desktop_set_busy', (_event, _busy) => {
      /**/
    });

    ipcMain.on('desktop_set_window_title', (event, title: string) => {
      this.mainWindow.window.setTitle(`${title} - ${appState().activation().editionName()}`);
    });

    ipcMain.on('desktop_install_rtools', (_event, version, installerPath) => {
      let command = `${installerPath} /SP- /SILENT`;
      const systemDrive = process.env.SYSTEMDRIVE;

      if (systemDrive?.length && existsSync(systemDrive)) {
        command = `${command} /DIR=${systemDrive}\\RBuildTools\\${version}`;
      }

      exec(command, (error, _stdout, stderr) => {
        if (error) {
          logger().logError(stderr);
        }
      });
    });

    ipcMain.handle('desktop_get_display_dpi', () => {
      const primaryDisplay = screen.getPrimaryDisplay();
      // scaling factor of 1 = 96 dpi
      const dpi = primaryDisplay.scaleFactor * 96;
      return dpi.toString();
    });

    ipcMain.on('desktop_on_session_quit', () => {
      this.emit(GwtCallback.SESSION_QUIT);
    });

    ipcMain.on('desktop_stop_main_thread', () => {
      process.crash();
    });

    // Define an interface for owners that have notifyAltMouseDown
    interface AltMouseDownNotifiable {
      notifyAltMouseDown: () => void;
      window: BrowserWindow;
    }

    // Handle Alt+mouse down notification for Windows multi-cursor fix
    ipcMain.on('desktop_alt_mouse_down', (event) => {
      const window = BrowserWindow.fromWebContents(event.sender);
      if (window) {
        // Find the GwtWindow that owns this BrowserWindow
        for (const owner of this.owners) {
          if (owner.window === window && 'notifyAltMouseDown' in owner) {
            (owner as AltMouseDownNotifiable).notifyAltMouseDown();
            break;
          }
        }
      }
    });

    ipcMain.handle('desktop_get_session_server', () => {
      GwtCallback.unimpl('desktop_get_session_server');
      return {};
    });

    ipcMain.handle('desktop_get_session_servers', () => {
      return [];
    });

    ipcMain.on('desktop_reconnect_to_session_server', (_event, _sessionServerJson) => {
      GwtCallback.unimpl('desktop_reconnect_to_session_server');
    });

    ipcMain.handle('desktop_set_launcher_server', (_event, _sessionServerJson) => {
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

    ipcMain.on('desktop_start_launcher_job_status_stream', (_event, _jobId) => {
      GwtCallback.unimpl('desktop_start_launcher_job_status_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_status_stream', (_event, _jobId) => {
      GwtCallback.unimpl('desktop_stop_launcher_job_status_stream');
    });

    ipcMain.on('desktop_start_launcher_job_output_stream', (_event, _jobId) => {
      GwtCallback.unimpl('desktop_start_launcher_job_output_stream');
    });

    ipcMain.on('desktop_stop_launcher_job_output_stream', (_event, _jobId) => {
      GwtCallback.unimpl('desktop_stop_launcher_job_output_stream');
    });

    ipcMain.on('desktop_control_launcher_job', (_event, _jobId, _operation) => {
      GwtCallback.unimpl('desktop_control_launcher_job');
    });

    ipcMain.on('desktop_submit_launcher_job', (_event, _job) => {
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
      if (varName === 'launch_failed') {
        this.addMacOSVersionError();
      }
      return resolveTemplateVar(varName, this.errorPageData);
    });

    ipcMain.on('desktop_show_splash_screen', () => {
      showPersistentSplashScreen();
    });

    ipcMain.on('desktop_detect_rosetta', () => {
      if (ElectronDesktopOptions().checkForRosetta()) {
        detectRosetta();
      }
    });
  }

  addMacOSVersionError(): void {
    if (platform() === 'darwin') {
      ipcMain.on('desktop_console_log', async (event, output) => {
        console.log(output);
      });
      const release_major = parseInt(release().substring(0, release().indexOf('.')));
      // macOS 11.0 uses darwin 20.0.0
      if (release_major < 20) {
        const versionProductName = execSync('sw_vers -productName').toString().trim();
        const versionProductVersion = execSync('sw_vers -productVersion').toString().trim();
        let versionError =
          'You are using an unsupported operating system: ' +
          versionProductName +
          ' ' +
          versionProductVersion +
          '. RStudio requires macOS 11 (Big Sur) or higher.';
        if (this.errorPageData.get('process_error')) {
          const launch_failed = this.errorPageData.get('process_error');
          if (!launch_failed?.includes('No error available')) {
            versionError += '\n\n' + launch_failed;
          }
        }
        this.errorPageData.set('process_error', versionError);
      }
    }
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
      void appState().modalTracker.trackElectronModalAsync(async () =>
        dialog.showMessageBox(focusedWindow, dialogOptions),
      );
    } else {
      void appState().modalTracker.trackElectronModalAsync(async () => dialog.showMessageBox(dialogOptions));
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

  convertMessageBoxType(type: number): 'info' | 'warning' | 'error' | 'question' {
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
        try {
          // Some owners in this.owners may not have been unregistered, but have
          // since been closed/destroyed. If that's the case for an owner, its
          // WebContents (win.window.webContents) will have been destoyed.
          // As a result, when we iterate through owners and hit one that has
          // been destroyed, we get: "TypeError: Object has been destroyed".
          // Then, we error out and cause a satellite window to have blank contents.
          // See https://github.com/rstudio/rstudio/issues/12468 and
          //     https://github.com/rstudio/rstudio/issues/12569
          // To avoid failing, we catch the error and move on to check the next owner.
          if (win.window.webContents.mainFrame === frame) {
            return win;
          }
        } catch (_error: unknown) {
          logger().logDebug('Window WebContents has been destroyed. Skipping this window.');
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
