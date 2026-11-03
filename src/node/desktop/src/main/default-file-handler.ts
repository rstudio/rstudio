/*
 * default-file-handler.ts
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

import { app } from 'electron';
import { logger } from '../core/logger';
import desktop from '../native/desktop.node';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { isAutomated } from './utils';

// The R-specific extensions RStudio registers in Info.plist.in. Generic ones it
// also registers (md, html, css, js, tex) are left to the user's existing choice.
export const kRFileExtensions = [
  'R',
  'Rmd',
  'Rmarkdown',
  'qmd',
  'Rnw',
  'Rtex',
  'Rd',
  'Rhtml',
  'Rpres',
  'Rprofvis',
  'Rproj',
  'RData',
  'rda',
];

export function isMacOSDefaultApplicationForFileExtension(extension: string): boolean {
  return desktop.macOSDefaultApplicationForFileExtension(extension).isCurrentApplication;
}

/**
 * Make RStudio the default application for the given extensions, skipping any
 * it already owns: macOS asks the user to confirm each change away from another
 * application, so skipping avoids needless prompts.
 */
export async function setMacOSDefaultApplicationForFileExtensions(extensions: string[]): Promise<void> {
  for (const extension of extensions) {
    if (!isMacOSDefaultApplicationForFileExtension(extension)) {
      await desktop.macOSSetDefaultApplicationForFileExtension(extension);
    }
  }
}

export async function setDefaultApplicationForRFiles(): Promise<void> {
  if (process.platform !== 'darwin') {
    throw new Error('Setting the default application for R files is only supported on macOS');
  }
  await setMacOSDefaultApplicationForFileExtensions(kRFileExtensions);
}

/**
 * macOS binds .R to its own legacy Rez source type, and Xcode wins that type
 * whenever it is installed, so no Info.plist declaration can make RStudio the
 * default. Ask macOS to switch it once per installation: the OS shows the
 * confirmation and records the user's answer, so a declined request is never
 * repeated. Only installed builds ask; development and automated runs stay quiet.
 */
export async function promptForDefaultRFileHandlerOnce(): Promise<void> {
  if (process.platform !== 'darwin' || !app.isPackaged || isAutomated()) {
    return;
  }

  const options = ElectronDesktopOptions();
  if (options.macOSDefaultFileHandlerPrompted()) {
    return;
  }
  options.setMacOSDefaultFileHandlerPrompted(true);

  if (isMacOSDefaultApplicationForFileExtension('R')) {
    return;
  }

  try {
    await desktop.macOSSetDefaultApplicationForFileExtension('R');
  } catch (error: unknown) {
    logger().logErrorMessage(
      `Unable to set RStudio as the default application for R files: ${error instanceof Error ? error.message : String(error)}`,
    );
  }
}
