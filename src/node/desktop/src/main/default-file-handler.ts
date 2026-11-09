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

import { app, BrowserWindow, dialog, MessageBoxOptions } from 'electron';
import i18next from 'i18next';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import desktop from '../native/desktop.node';
import { appState } from './app-state';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { isAutomated } from './utils';

// The R-specific extensions RStudio registers in Info.plist.in. Generic ones it
// also registers (qmd, md, html, css, js, tex) are left to the user's existing
// choice; .qmd is often owned by Positron, VS Code or the Quarto app.
export const kRFileExtensions = [
  'R',
  'Rmd',
  'Rmarkdown',
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
 * Whether macOS asks the user to confirm a change of the default application
 * for a file type. Before macOS 26.4 the change is made without asking.
 */
export function macOSConfirmsDefaultApplicationChanges(): boolean {
  const [major, minor] = process.getSystemVersion().split('.').map(Number);
  return major > 26 || (major === 26 && minor >= 4);
}

/**
 * Ask the user whether RStudio should open R files, for macOS versions that
 * would otherwise switch them without asking. Resolves to true for RStudio.
 */
export async function confirmDefaultRFileHandler(window?: BrowserWindow): Promise<boolean> {
  const options: MessageBoxOptions = {
    type: 'question',
    message: i18next.t('defaultFileHandlerTs.confirmMessage'),
    detail: i18next.t('defaultFileHandlerTs.confirmDetail'),
    buttons: [i18next.t('defaultFileHandlerTs.useRStudio'), i18next.t('defaultFileHandlerTs.keepCurrent')],
    defaultId: 0,
    cancelId: 1,
  };

  const result = await appState().modalTracker.trackElectronModalAsync(async () =>
    window ? dialog.showMessageBox(window, options) : dialog.showMessageBox(options),
  );
  return result.response === 0;
}

/**
 * Make RStudio the default application for the given extensions, skipping any
 * it already owns: macOS 26.4 and later ask the user to confirm each change away
 * from another application, so skipping avoids needless prompts. Each extension is asked
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

  // asking about .R here answers the startup offer too, so it is not repeated
  if (!isMacOSDefaultApplicationForFileExtension('R')) {
    ElectronDesktopOptions().setMacOSDefaultFileHandlerPrompted(true);
  }

  const errors = await setMacOSDefaultApplicationForFileExtensions(kRFileExtensions);
  const elsewhere = kRFileExtensions.filter((extension) => !isMacOSDefaultApplicationForFileExtension(extension));
  return { elsewhere, error: errors.join('\n') };
}

/**
 * macOS binds .R to its own legacy Rez source type, and Xcode wins that type
 * whenever it is installed, so no Info.plist declaration can make RStudio the
 * default. Offer to switch it once per user, and never repeat a declined offer.
 * macOS 26.4 and later confirm the change themselves; earlier versions make it
 * silently, so RStudio asks first. The offer is only used up once the user is
 * actually asked, so installing Xcode after RStudio still gets one. Only
 * installed builds ask; development and automated runs stay quiet.
 */
export async function promptForDefaultRFileHandlerOnce(window?: BrowserWindow): Promise<void> {
  if (process.platform !== 'darwin' || !app.isPackaged || isAutomated()) {
    return;
  }

  const options = ElectronDesktopOptions();
  if (options.macOSDefaultFileHandlerPrompted() || isMacOSDefaultApplicationForFileExtension('R')) {
    return;
  }

  options.setMacOSDefaultFileHandlerPrompted(true);
  if (!macOSConfirmsDefaultApplicationChanges() && !(await confirmDefaultRFileHandler(window))) {
    return;
  }

  try {
    await desktop.macOSSetDefaultApplicationForFileExtension('R');
  } catch (error: unknown) {
    logger().logErrorMessage(
      `Unable to set RStudio as the default application for R files: ${safeError(error).message}`,
    );
  }
}
