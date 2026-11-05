/*
 * context-menu.ts
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

import { BrowserWindow, clipboard, dialog, Menu, WebContents } from 'electron';
import i18next from 'i18next';
import path from 'path';
import { logger } from '../core/logger';
import { appState } from './app-state';
import { queueClipboardWrite } from './clipboard-queue';

type ContextMenuItem = Electron.MenuItem | Electron.MenuItemConstructorOptions;

// xterm.js selects its hidden helper textarea when it handles a right-click, before
// Electron raises 'context-menu'; the params alone can't tell it from Ace's textarea.
const kTerminalFocusedScript = "document.activeElement?.classList.contains('xterm-helper-textarea') === true";

// The browser's select-all would only select the helper textarea's own (empty) text, so
// the terminal listens for this event on that textarea and selects its buffer instead.
const kTerminalSelectAllScript = `(() => {
  const textarea = document.activeElement;
  if (textarea?.classList.contains('xterm-helper-textarea') !== true) {
    return false;
  }
  textarea.dispatchEvent(new Event('rstudio-terminal-select-all'));
  return true;
})()`;

/**
 * Whether the menu is over the Terminal pane, i.e. xterm's helper textarea has focus.
 */
export const _isOverTerminal = async (params: Electron.ContextMenuParams): Promise<boolean> => {
  if (params.hasImageContents) {
    return false;
  }

  // The frame is null when it has navigated away or been destroyed since the click. The
  // terminal lives in the top-level frame, so don't run the check in Viewer or Help iframes.
  if (params.frame === null || params.frame.parent !== null) {
    return false;
  }

  try {
    return (await params.frame.executeJavaScript(kTerminalFocusedScript)) === true;
  } catch (error: unknown) {
    logger().logErrorMessage(`Failed to check whether the context menu is over the terminal: ${error}`);
    return false;
  }
};

export const _selectAllInTerminal = async (frame: Electron.WebFrameMain | null): Promise<void> => {
  try {
    if (frame === null) {
      throw new Error('the frame is gone');
    }
    if ((await frame.executeJavaScript(kTerminalSelectAllScript)) !== true) {
      throw new Error('the terminal no longer has focus');
    }
  } catch (error: unknown) {
    logger().logErrorMessage(`Failed to select all in the terminal: ${error}`);
  }
};

export const showContextMenu = async (sender: WebContents, params: Electron.ContextMenuParams): Promise<void> => {
  const overTerminal = await _isOverTerminal(params);
  const template = _createContextMenuTemplate(sender, params, overTerminal);

  const menu = Menu.buildFromTemplate(template);
  menu.popup();
};

/**
 * @param overTerminal The menu is over the Terminal pane: Select All selects the terminal's
 *   buffer, and on Windows / Linux the terminal's copy and paste keys (Ctrl+Shift+C /
 *   Ctrl+Shift+V) are shown instead of Ctrl+C / Ctrl+V, which the terminal sends to the shell.
 */
export const _createContextMenuTemplate = (
  sender: WebContents,
  params: Electron.ContextMenuParams,
  overTerminal = false,
  platform: NodeJS.Platform = process.platform,
): ContextMenuItem[] => {
  if (params.hasImageContents) {
    return createContextMenuImageTemplate(sender, params);
  } else {
    return createContextMenuTextTemplate(sender, params, overTerminal, platform);
  }
};

const createContextMenuImageTemplate = (sender: WebContents, params: Electron.ContextMenuParams): ContextMenuItem[] => {
  return [
    // Save Image As...
    {
      label: i18next.t('contextMenu.saveImageAsDots'),
      click: async () => {
        // ask the user for a download file path.  in theory, we could let the
        // default download handler do this, but Electron appears to barf if the
        // user cancels that dialog
        const webContents = sender;
        const window = BrowserWindow.fromWebContents(webContents) as BrowserWindow;
        const downloadPath = appState().modalTracker.trackElectronModalSync(() =>
          dialog.showSaveDialogSync(window, {
            title: i18next.t('contextMenu.saveImageAs'),
            defaultPath: path.basename(params.srcURL),
            buttonLabel: i18next.t('contextMenu.save'),
            properties: ['createDirectory'],
          }),
        );

        // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
        if (downloadPath == null) {
          return;
        }

        // set up a download handler
        sender.session.once('will-download', (event, item) => {
          // set the download path (so Electron doesn't try to prompt)
          item.setSavePath(downloadPath);

          // check for failure on completion
          item.once('done', (event, state) => {
            switch (state) {
              case 'cancelled': {
                appState().modalTracker.trackElectronModalSync(() =>
                  dialog.showErrorBox(
                    i18next.t('contextMenu.errorDownloadingImage'),
                    i18next.t('contextMenu.downloadCancelledMessage'),
                  ),
                );
                break;
              }

              case 'interrupted': {
                appState().modalTracker.trackElectronModalSync(() =>
                  dialog.showErrorBox(
                    i18next.t('contextMenu.errorDownloadingImage'),
                    i18next.t('contextMenu.downloadInterruptedMessage'),
                  ),
                );
                break;
              }
            }
          });
        });

        // initiate the actual download
        sender.downloadURL(params.srcURL);
      },
    },

    // Copy Image
    {
      label: i18next.t('contextMenu.copyImage'),
      click: () => {
        sender.copyImageAt(params.x, params.y);
      },
    },

    // Copy Image Address
    {
      label: i18next.t('contextMenu.copyImageAddress'),
      click: () => {
        queueClipboardWrite(async () => clipboard.writeText(params.srcURL));
      },
    },

    // Separator
    {
      type: 'separator',
    },

    // Reload
    { label: i18next.t('contextMenu.reload'), role: 'reload' },

    // Inspect Element
    {
      label: i18next.t('contextMenu.inspectElement'),
      click: () => {
        sender.inspectElement(params.x, params.y);
      },
    },
  ];
};

// Display-only accelerators: the terminal's own key handler does the work, so registering
// them would only shadow it.
const terminalCopyAccelerator = { accelerator: 'Ctrl+Shift+C', registerAccelerator: false };
const terminalPasteAccelerator = { accelerator: 'Ctrl+Shift+V', registerAccelerator: false };
const terminalMacSelectAllAccelerator = { accelerator: 'Cmd+A', registerAccelerator: false };

const createContextMenuTextTemplate = (
  sender: WebContents,
  params: Electron.ContextMenuParams,
  overTerminal: boolean,
  platform: NodeJS.Platform,
): ContextMenuItem[] => {
  // On macOS the terminal's keys are Cmd+C / Cmd+V / Cmd+A, matching the role defaults.
  const terminalShortcuts = overTerminal && platform !== 'darwin';

  // We would like to just always use the already-existing roles for clipboard
  // actions, but https://www.electronjs.org/docs/api/menu-item has:
  //
  // When specifying a role on macOS, label and accelerator are the only options
  // that will affect the menu item. All other options will be ignored.
  // Lowercase role, e.g. toggledevtools, is still supported.
  //
  // so we have to do some extra gymnastics. As is tradition.
  const template: ContextMenuItem[] = [];

  if (params.editFlags.canCut && terminalShortcuts) {
    // No role, so no Ctrl+X hint: in the terminal Ctrl+X goes to the shell.
    template.push({ label: i18next.t('contextMenu.cut'), click: () => sender.cut() });
  } else if (params.editFlags.canCut) {
    template.push({ label: i18next.t('contextMenu.cut'), role: 'cut' });
  } else {
    template.push({ label: i18next.t('contextMenu.cut'), enabled: false });
  }

  if (params.editFlags.canCopy) {
    const accelerator = terminalShortcuts ? terminalCopyAccelerator : {};
    template.push({ label: i18next.t('contextMenu.copy'), role: 'copy', ...accelerator });
  } else {
    template.push({ label: i18next.t('contextMenu.copy'), enabled: false });
  }

  if (params.editFlags.canPaste) {
    const accelerator = terminalShortcuts ? terminalPasteAccelerator : {};
    template.push({ label: i18next.t('contextMenu.paste'), role: 'paste', ...accelerator });
  } else {
    template.push({ label: i18next.t('contextMenu.paste'), enabled: false });
  }

  if (overTerminal) {
    // Enabled whatever the helper textarea's edit flags say: they don't describe the
    // terminal's buffer. On Windows and Linux there is no hint, since Ctrl+A goes to the
    // shell and the terminal has no select-all key there.
    const accelerator = platform === 'darwin' ? terminalMacSelectAllAccelerator : {};
    template.push({
      label: i18next.t('contextMenu.selectAll'),
      click: () => void _selectAllInTerminal(params.frame),
      ...accelerator,
    });
  } else if (params.editFlags.canSelectAll) {
    template.push({ label: i18next.t('contextMenu.selectAll'), role: 'selectAll' });
  } else {
    template.push({ label: i18next.t('contextMenu.selectAll'), enabled: false });
  }

  template.push({ type: 'separator' });

  template.push({ label: i18next.t('contextMenu.reload'), role: 'reload' });

  template.push({
    label: i18next.t('contextMenu.inspectElement'),
    click: () => {
      sender.inspectElement(params.x, params.y);
    },
  });

  return template;
};
