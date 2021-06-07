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
import Main from './main';
import { getRStudioVersion } from './product-info';
import { augmentCommandLineArguments, getComponentVersions, removeStaleOptionsLockfile } from './utils';

/**
 * Startup code run before app 'ready' event.
 */

// look for a version check request; if we have one, just do that and exit
if (app.commandLine.hasSwitch('version')) {
  console.log(getRStudioVersion());
  app.exit(0);
}

// report extended version info and exit
if (app.commandLine.hasSwitch('version-json')) {
  console.log(getComponentVersions());
  app.exit(0);
}

// attempt to remove stale lockfiles, as they can impede application startup
try {
  removeStaleOptionsLockfile();
} catch (error) {
  console.error(error);
  app.exit(1);
}

// allow users to supply extra command-line arguments
augmentCommandLineArguments();

/**
 * Handlers for `app` events go here; otherwise do as little as possible in this
 * file (it cannot be unit-tested).
 */
app.whenReady().then(() => {
  new Main().run();
});
