/*
 * pending-window.js
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

module.exports = class PendingWindow {
  name = null;
  x = 0;
  y = 0;
  width = 0;
  height = 0;
  isSatellite = false;
  allowExternalNavigate = false; // for RDP
  showToolbar = false;

  constructor(name, screenX, screenY, width, height) {
    this.name = name;
    this.x = screenX;
    this.y = screenY;
    this.width = width;
    this.height = height;
    this.isSatellite = true;
  }

  get isEmpty() {
    return !this.name;
  }
};
 