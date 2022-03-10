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

// preload never calls this again so these values are immutable
export function getDesktopInfoBridge() {
  return {
    platform: '',
    version: '',
    scrollingCompensationType: '',
    fixedWidthFontList: ipcRenderer.sendSync('desktop_get_fixed_width_font_list'),
    fixedWidthFont: ipcRenderer.sendSync('desktop_get_fixed_width_font'),
    proportionalFont: ipcRenderer.sendSync('desktop_get_proportional_font'),
    desktopSynctexViewer: '',
    zoomLevel: ipcRenderer.sendSync('desktop_get_zoom_level'),
    chromiumDevtoolsPort: 0,
  };
}
