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

export const _isTerminalFocused = async (params: Electron.ContextMenuParams): Promise<boolean> => {
  // null when the frame has navigated away or been destroyed since the click
  if (params.frame === null) {
    return false;
  }

  try {
    return (await params.frame.executeJavaScript(kTerminalFocusedScript)) === true;
  } catch (error: unknown) {
    logger().logErrorMessage(`Failed to check whether the context menu is over the terminal: ${error}`);
    return false;
  }
};

export const showContextMenu = async (sender: WebContents, params: Electron.ContextMenuParams): Promise<void> => {
  // On macOS the terminal copies and pastes with Cmd+C / Cmd+V, matching the role defaults.
  const terminalShortcuts = process.platform !== 'darwin' && (await _isTerminalFocused(params));
  const template = _createContextMenuTemplate(sender, params, terminalShortcuts);

  const menu = Menu.buildFromTemplate(template);
  menu.popup();
};

/**
 * @param terminalShortcuts Show the Terminal pane's Windows / Linux copy and paste keys
 *   (Ctrl+Shift+C / Ctrl+Shift+V) instead of Ctrl+C / Ctrl+V, which the terminal sends to the shell.
 */
export const _createContextMenuTemplate = (
  sender: WebContents,
  params: Electron.ContextMenuParams,
  terminalShortcuts = false,
): ContextMenuItem[] => {
  if (params.hasImageContents) {
    return createContextMenuImageTemplate(sender, params);
  } else {
    return createContextMenuTextTemplate(sender, params, terminalShortcuts);
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

const createContextMenuTextTemplate = (
  sender: WebContents,
  params: Electron.ContextMenuParams,
  terminalShortcuts: boolean,
): ContextMenuItem[] => {
  // We would like to just always use the already-existing roles for clipboard
  // actions, but https://www.electronjs.org/docs/api/menu-item has:
  //
  // When specifying a role on macOS, label and accelerator are the only options
  // that will affect the menu item. All other options will be ignored.
  // Lowercase role, e.g. toggledevtools, is still supported.
  //
  // so we have to do some extra gymnastics. As is tradition.
  const template: ContextMenuItem[] = [];

  if (params.editFlags.canCut) {
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

  if (params.editFlags.canSelectAll && terminalShortcuts) {
    // No role, so no Ctrl+A hint: in the terminal Ctrl+A goes to the shell, and there is
    // no select-all key on Windows or Linux.
    template.push({ label: i18next.t('contextMenu.selectAll'), click: () => sender.selectAll() });
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
