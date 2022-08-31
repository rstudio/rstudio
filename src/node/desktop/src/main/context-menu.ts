/*
 * context-menu.ts
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

import { BrowserWindow, clipboard, dialog, Menu } from 'electron';
import path from 'path';
import i18next from 'i18next';

type ContextMenuItem = Electron.MenuItem | Electron.MenuItemConstructorOptions;

export const showContextMenu = (webContents: Electron.WebContents, params: Electron.ContextMenuParams): void => {
  const template = _createContextMenuTemplate(webContents, params);

  const menu = Menu.buildFromTemplate(template);
  menu.popup();
};

export const _createContextMenuTemplate = (
  webContents: Electron.WebContents,
  params: Electron.ContextMenuParams,
): ContextMenuItem[] => {

  let template: ContextMenuItem[] = [];
  if (params.hasImageContents) {
    template = createContextMenuImageTemplate(webContents, params);
  } else {
    template = createContextMenuTextTemplate(webContents, params);
  }

  return template;
};

const createContextMenuImageTemplate = (
  webContents: Electron.WebContents,
  params: Electron.ContextMenuParams,
): ContextMenuItem[] => {
  return [
    // Save Image As...
    {
      label: i18next.t('contextMenu.saveImageAsDots'),
      click: async () => {
        // ask the user for a download file path.  in theory, we could let the
        // default download handler do this, but Electron appears to barf if the
        // user cancels that dialog
        const window = BrowserWindow.fromWebContents(webContents) as BrowserWindow;
        const downloadPath = dialog.showSaveDialogSync(window, {
          title: i18next.t('contextMenu.saveImageAs'),
          defaultPath: path.basename(params.srcURL),
          buttonLabel: i18next.t('contextMenu.save'),
          properties: ['createDirectory'],
        });

        if (downloadPath == null) {
          return;
        }

        // set up a download handler
        webContents.session.once('will-download', (event, item) => {
          // set the download path (so Electron doesn't try to prompt)
          item.setSavePath(downloadPath);

          // check for failure on completion
          item.once('done', (event, state) => {
            switch (state) {
              case 'cancelled': {
                dialog.showErrorBox(
                  i18next.t('contextMenu.errorDownloadingImage'),
                  i18next.t('contextMenu.downloadCancelledMessage'),
                );
                break;
              }

              case 'interrupted': {
                dialog.showErrorBox(
                  i18next.t('contextMenu.errorDownloadingImage'),
                  i18next.t('contextMenu.downloadInterruptedMessage'),
                );
                break;
              }
            }
          });
        });

        // initiate the actual download
        webContents.downloadURL(params.srcURL);
      },
    },

    // Copy Image
    {
      label: i18next.t('contextMenu.copyImage'),
      click: () => {
        webContents.copyImageAt(params.x, params.y);
      },
    },

    // Copy Image Address
    {
      label: i18next.t('contextMenu.copyImageAddress'),
      click: () => {
        clipboard.writeText(params.srcURL);
      },
    },

    // Separator
    {
      type: 'separator',
    },

    // Reload
    {
      label: i18next.t('contextMenu.reload'),
      click: () => {
        webContents.reload();
      }
    },

    // Inspect Element
    {
      label: i18next.t('contextMenu.inspectElement'),
      click: () => {
        webContents.inspectElement(params.x, params.y);
      },
    },
  ];
};

const createContextMenuTextTemplate = (
  webContents: Electron.WebContents,
  params: Electron.ContextMenuParams,
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
    template.push({ label: i18next.t('contextMenu.copy'), role: 'copy' });
  } else {
    template.push({ label: i18next.t('contextMenu.copy'), enabled: false });
  }

  if (params.editFlags.canPaste) {
    template.push({ label: i18next.t('contextMenu.paste'), role: 'paste' });
  } else {
    template.push({ label: i18next.t('contextMenu.paste'), enabled: false });
  }

  if (params.editFlags.canSelectAll) {
    template.push({ label: i18next.t('contextMenu.selectAll'), role: 'selectAll' });
  } else {
    template.push({ label: i18next.t('contextMenu.selectAll'), enabled: false });
  }

  template.push({ type: 'separator' });

  template.push({
    label: i18next.t('contextMenu.reload'),
    click: () => { webContents.reload(); }
  });

  template.push({
    label: i18next.t('contextMenu.inspectElement'),
    click: () => {
      webContents.inspectElement(params.x, params.y);
    },
  });

  return template;
};
