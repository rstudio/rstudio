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
import { safeError } from '../core/err';
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
  return desktop.macOSIsDefaultApplicationForFileExtension(extension);
}

/**
 * Make RStudio the default application for the given extensions, skipping any
 * it already owns: macOS asks the user to confirm each change away from another
 * application, so skipping avoids needless prompts. Each extension is asked
 * about on its own, so declining or failing one still offers the rest.
 *
 * @returns One message per extension macOS failed to change; a declined
 *   confirmation is the user's answer, not a failure.
 */
export async function setMacOSDefaultApplicationForFileExtensions(extensions: string[]): Promise<string[]> {
  const errors: string[] = [];

  for (const extension of extensions) {
    if (isMacOSDefaultApplicationForFileExtension(extension)) {
      continue;
    }

    try {
      await desktop.macOSSetDefaultApplicationForFileExtension(extension);
    } catch (error: unknown) {
      errors.push(`.${extension}: ${safeError(error).message}`);
    }
  }

  return errors;
}

export interface DefaultApplicationForRFilesResult {
  // the R file extensions that still open in another application
  elsewhere: string[];
  // why some could not be changed, or empty
  error: string;
}

/**
 * Make RStudio the default application for every R file type, for the Options
 * button. Never rejects, so the button always hears back.
 */
export async function setDefaultApplicationForRFiles(): Promise<DefaultApplicationForRFilesResult> {
  if (process.platform !== 'darwin') {
    return { elsewhere: [], error: 'Setting the default application for R files is only supported on macOS' };
  }

  // unpackaged, this bundle is Electron.app, which would become the default instead
  if (!app.isPackaged) {
    return { elsewhere: [], error: 'Only an installed copy of RStudio can become the default application for R files' };
  }

  const errors = await setMacOSDefaultApplicationForFileExtensions(kRFileExtensions);
  const elsewhere = kRFileExtensions.filter((extension) => !isMacOSDefaultApplicationForFileExtension(extension));
  return { elsewhere, error: errors.join('\n') };
}

/**
 * macOS binds .R to its own legacy Rez source type, and Xcode wins that type
 * whenever it is installed, so no Info.plist declaration can make RStudio the
 * default. Ask macOS to switch it once per user: the OS shows the confirmation,
 * and a declined request is never repeated. The offer is only used up once
 * macOS is actually asked, so installing Xcode after RStudio still gets one.
 * Only installed builds ask; development and automated runs stay quiet.
 */
export async function promptForDefaultRFileHandlerOnce(): Promise<void> {
  if (process.platform !== 'darwin' || !app.isPackaged || isAutomated()) {
    return;
  }

  const options = ElectronDesktopOptions();
  if (options.macOSDefaultFileHandlerPrompted() || isMacOSDefaultApplicationForFileExtension('R')) {
    return;
  }

  options.setMacOSDefaultFileHandlerPrompted(true);
  try {
    await desktop.macOSSetDefaultApplicationForFileExtension('R');
  } catch (error: unknown) {
    logger().logErrorMessage(
      `Unable to set RStudio as the default application for R files: ${safeError(error).message}`,
    );
  }
}
