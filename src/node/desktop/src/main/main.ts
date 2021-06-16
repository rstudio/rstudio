/*
 * app.ts
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

import { app } from 'electron';
import { Application } from './application';

const rstudio: Application = new Application();

const initStatus = rstudio.beforeAppReady();
if (initStatus.exit) {
  app.exit(initStatus.exitCode);
}

/**
 * Handlers for `app` events go here; otherwise do as little as possible in this
 * file (it cannot be unit-tested).
 */
app.whenReady().then(() => {
  rstudio.run();
});
