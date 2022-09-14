/*
 * toolbar-manager.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

/* eslint-disable max-len */

import { BrowserWindow } from 'electron';
import * as fs from 'fs';
import { logger } from '../core/logger';

type ToolbarButton = {
  iconPath: string;
  tooltip: string;
  onClick: string;
};

export interface ToolbarData {
  buttons: ToolbarButton[];
}

export class ToolbarManager {
  constructor() {}

  async createAndShowToolbar(window: BrowserWindow, toolbarData: ToolbarData) {
    const jsScript = this.createToolbarData(toolbarData.buttons);

    try {
      await window.webContents.executeJavaScript(jsScript);

      // eslint-disable-next-line @typescript-eslint/no-implicit-any-catch
    } catch (err: any) {
      if (err.message !== 'An object could not be cloned.') {
        const error =
          'Error message: ' +
          err.message +
          '\nError when trying to create toolbar with data:\n' +
          JSON.stringify(toolbarData) +
          '\n---------------------------\nJS Script for Toolbar:\n' +
          jsScript;
        logger().logError(error);
      }
    }
  }

  createToolbarData(buttons: ToolbarButton[]) {
    const jsScript =
      this.addToolbarJsAsText() + ';' + this.addStylesJsAsText() + ';' + this.addButtonsJsAsText(buttons);

    return jsScript;
  }

  addToolbarJsAsText() {
    const toolbar = ` 
      const toolbar = document.createElement('div');
      toolbar.id = 'custom-toolbar';
      toolbar.setAttribute('aria-label', 'Navigation');
      toolbar.setAttribute('role', 'toolbar');
      
      document.body.prepend(toolbar);
    `;

    return toolbar;
  }

  addStylesJsAsText() {
    const styles = `
        body{
          padding-top:29px;
        }

        #custom-toolbar {
          display: flex;
          align-items: center;
          justify-content: flex-start;
          width: 100%;
          background: gray;
          padding: 4px;
          background-color: rgb(239, 239, 239);
          border-bottom: 1px solid rgb(214, 214, 214);
          position: fixed;
          top:0;
          left:0;
        }

        .custom-toolbar_button {
          display: flex;
          align-items: center;
          justify-content: center;

          cursor: pointer;
          margin-right: 8px;

          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;

          border: none !important;
          background: none !important;
          padding: 0 !important;
        }

        .custom-toolbar_icon {
          width: 26px;
          height: 20px;
          margin: 0 !important;
          border: none !important;
          background: none !important;
          padding: 0 !important;
        }

        .custom-toolbar_icon:active {
          filter: brightness(80%) saturate(150%);
        }

        @media (prefers-color-scheme: dark) {
          #custom-toolbar {
            background-color: rgb(48, 48, 48);
            border-bottom: 1px solid rgb(12, 31, 48);
          }
        }
    `;

    return `
      const styleSheet = document.createElement('style');
      styleSheet.innerText = \`${styles}\`;
      document.head.appendChild(styleSheet);
      `;
  }

  addButtonsJsAsText(buttons: ToolbarButton[]) {
    let addButtonsFn = `
      const createButton = (iconPath, tooltip, onClick, index) => {
        const button = document.createElement('button');
        button.id = 'custom-toolbar_button-' + tooltip.replace(/ /g, '-');
        button.classList.add('custom-toolbar_button');
        button.title = tooltip;
        button.setAttribute('aria-label', tooltip);
        button.setAttribute('tabindex', index + 1);
        button.onclick = onClick;

        const icon = document.createElement('img');
        icon.src = iconPath;
        icon.alt = tooltip;
        icon.id = 'custom-toolbar_icon-' + tooltip.replace(/ /g, '-');
        icon.classList.add('custom-toolbar_icon');

        button.appendChild(icon);

        if ( index == 0 ){
          button.focus();
          button.blur();
        }

        return button;
      };    
    `;

    buttons.forEach((button, index) => {
      addButtonsFn += `const button${index} = createButton('${this.convertImagePathToBase64(button.iconPath)}', '${
        button.tooltip
      }', ${button.onClick}, ${index});
      document.getElementById('custom-toolbar').appendChild(button${index});`;
    });

    return addButtonsFn;
  }

  convertImagePathToBase64(imagePath: string) {
    const bitmap = fs.readFileSync(imagePath);
    return 'data:image/png;base64, ' + Buffer.from(bitmap).toString('base64');
  }
}
