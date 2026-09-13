/*
 * process-diagnostics.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import { app, Details, RenderProcessGoneDetails } from 'electron';
import { logger } from '../core/logger';

function logProcessExit(eventName: string, details: Details | RenderProcessGoneDetails, webContentsId?: number): void {
  const message = `Electron ${eventName}: ${JSON.stringify({ ...details, webContentsId })}`;
  if (details.reason === 'clean-exit') {
    logger().logDebug(message);
  } else {
    logger().logErrorMessage(message);
  }
}

/** Register before creating windows so failures in every renderer and GPU/utility process are logged. */
export function registerProcessDiagnostics(): void {
  app.on('render-process-gone', (_event, webContents, details) => {
    logProcessExit('render-process-gone', details, webContents.id);
  });

  app.on('child-process-gone', (_event, details) => {
    logProcessExit('child-process-gone', details);
  });
}
