/*
 * desktop-info-bridge.ts
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

import { ipcRenderer } from 'electron';

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getDesktopInfoBridge() {
  console.log('desktopInfoBridge');
  const desktopInfoBridge = {
    platform: '',
    version: '',
    scrollingCompensationType: '',
    fixedWidthFontList: ipcRenderer.sendSync('desktop_get_fixed_width_font_list'),
    fixedWidthFont: ipcRenderer.sendSync('desktop_get_fixed_width_font'),
    desktopSynctexViewer: '',
    zoomLevel: 1.0,
    chromiumDevtoolsPort: 0,
  };
  if (process.platform === 'win32') {
    return {
      ...desktopInfoBridge,
      ...{
        proportionalFont: 'Segoe UI',
      },
    };
  } else {
    return {
      ...desktopInfoBridge,
      ...{
        proportionalFont: 'Lucida Grande',
      },
    };
  }
}
