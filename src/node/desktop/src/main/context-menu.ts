/*
 * context-menu.ts
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

import { BrowserWindow, clipboard, dialog, Menu } from 'electron';
import path from 'path';
import { createStandaloneErrorDialog } from './utils';

type ContextMenuItem = Electron.MenuItem | Electron.MenuItemConstructorOptions;

function showContextMenuImageTemplate(
  event: Electron.IpcMainEvent,
  params: Electron.ContextMenuParams,
): ContextMenuItem[] {
  return [
    // Save Image As...
    {
      label: 'Save Image As...',
      click: async () => {
        // ask the user for a download file path.  in theory, we could let the
        // default download handler do this, but Electron appears to barf if the
        // user cancels that dialog
        const webContents = event.sender;
        const window = BrowserWindow.fromWebContents(webContents) as BrowserWindow;
        const downloadPath = dialog.showSaveDialogSync(window, {
          title: 'Save Image As',
          defaultPath: path.basename(params.srcURL),
          buttonLabel: 'Save',
          properties: ['createDirectory'],
        });

        if (downloadPath == null) {
          return;
        }

        // set up a download handler
        event.sender.session.once('will-download', (event, item) => {
          // set the download path (so Electron doesn't try to prompt)
          item.setSavePath(downloadPath);

          // check for failure on completion
          item.once('done', (event, state) => {
            switch (state) {
              case 'cancelled': {
                dialog.showErrorBox('Error Downloading Image', 'The download was cancelled.');
                break;
              }

              case 'interrupted': {
                dialog.showErrorBox('Error Downloading Image', 'The download was interrupted.');
                break;
              }
            }
          });
        });

        // initiate the actual download
        event.sender.downloadURL(params.srcURL);
      },
    },

    // Copy Image
    {
      label: 'Copy Image',
      click: () => {
        event.sender.copyImageAt(params.x, params.y);
      },
    },

    // Copy Image Address
    {
      label: 'Copy Image Address',
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
      role: 'reload',
    },

    // Inspect Element
    {
      label: 'Inspect Element',
      click: () => {
        event.sender.inspectElement(params.x, params.y);
      },
    },
  ];
}

function showContextMenuTextTemplate(
  event: Electron.IpcMainEvent,
  params: Electron.ContextMenuParams,
): ContextMenuItem[] {
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
    template.push({ role: 'cut' });
  } else {
    template.push({ label: 'Cut', enabled: false });
  }

  if (params.editFlags.canCopy) {
    template.push({ role: 'copy' });
  } else {
    template.push({ label: 'Copy', enabled: false });
  }

  if (params.editFlags.canPaste) {
    template.push({ role: 'paste' });
  } else {
    template.push({ label: 'Paste', enabled: false });
  }

  if (params.editFlags.canSelectAll) {
    template.push({ role: 'selectAll' });
  } else {
    template.push({ label: 'Select All', enabled: false });
  }

  template.push({ type: 'separator' });

  template.push({ role: 'reload' });

  template.push({
    label: 'Inspect Element',
    click: () => {
      event.sender.inspectElement(params.x, params.y);
    },
  });

  return template;
}

export function showContextMenu(event: Electron.IpcMainEvent, params: Electron.ContextMenuParams): void {
  let template: ContextMenuItem[] = [];
  if (params.hasImageContents) {
    template = showContextMenuImageTemplate(event, params);
  } else {
    template = showContextMenuTextTemplate(event, params);
  }

  const menu = Menu.buildFromTemplate(template);
  menu.popup();
}
