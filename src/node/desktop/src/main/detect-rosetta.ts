/*
 * detect-rosetta.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

import { execSync } from 'child_process';
import { logger } from '../core/logger';
import { dialog, MessageBoxOptions, shell } from 'electron';
import { appState } from './app-state';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { t } from 'i18next';
import { initI18n } from './i18n-manager';

enum DetectRosettaStatus {
  ROSETTA_CHECK_FAILED,
  ROSETTA_INSTALL_WARNING,
}

initI18n();

const DETECT_ROSETTA_STATUS_MAP: Map<DetectRosettaStatus, MessageBoxOptions> = new Map([
  [
    DetectRosettaStatus.ROSETTA_CHECK_FAILED,
    {
      type: 'error',
      buttons: [t('common.buttonOk')],
      defaultId: 0,
      title: t('detectRosetta.checkFailedTitle'),
      message: t('detectRosetta.checkFailedMessage'),
      detail: t('detectRosetta.checkFailedDetail'),
    },
  ],
  [
    DetectRosettaStatus.ROSETTA_INSTALL_WARNING,
    {
      type: 'warning',
      buttons: [
        t('detectRosetta.installWarningMoreInfo'),
        t('detectRosetta.installWarningRemindLater'),
        t('detectRosetta.installWarningDoNotRemind'),
      ],
      defaultId: 0,
      title: t('detectRosetta.installWarningTitle'),
      message: t('detectRosetta.installWarningMessage'),
      detail: t('detectRosetta.installWarningDetail'),
    },
  ],
]);

/**
 * Checks if Rosetta is installed and running on Apple Silicon. Warns user to install Rosetta
 * if it is not installed.
 *
 * Despite having a "Universal" Mac build, we currently still ship with Intel components.
 * As a result, users on Mac M1/M2 (aka: arm64, aarch64) will need to have Rosetta 2 to avoid
 * running into issues with Intel-only components.
 *
 * See https://github.com/rstudio/rstudio/issues/12572 regarding Intel-only components.
 *
 * @returns true if Rosetta is installed and running, false if Rosetta is not installed,
 *          and undefined if not on Apple Silicon.
 */
export function detectRosetta(): boolean | undefined {
  const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
  if (!isAppleSilicon) return undefined;

  const isRosettaInstalled = isRosettaRunning();
  if (!isRosettaInstalled) {
    logger().logDebug('Rosetta 2 is not running. Warning user to install Rosetta to avoid issues.');
    showDialogForStatus(DetectRosettaStatus.ROSETTA_INSTALL_WARNING, (selectedButtonId) => {
      switch (selectedButtonId) {
        case 0:
          // Selected 'More Information' button. Open link to Rosetta installation instructions.
          shell.openExternal(
            'https://docs.posit.co/ide/desktop-pro/getting_started/installation.html#apple-silicon-mac-m1m2',
          );
          break;
        case 1:
          // Selected 'Remind Me Later' button. Set ElectronDesktopOptions to check for Rosetta on next startup.
          ElectronDesktopOptions().setCheckForRosetta(true);
          break;
        case 2:
          // Selected 'Don't Remind Me Again' button. Set ElectronDesktopOptions to not check for Rosetta.
          ElectronDesktopOptions().setCheckForRosetta(false);
          break;
        default:
          break;
      }
    });
  }
  return isRosettaInstalled;
}

/**
 * Runs a command to check if Rosetta is running.
 * Internally on Mac, Rosetta 2 is referred to as OAH and its running service is called 'oahd'.
 * We check that the process id of 'oahd' is returned to determine if Rosetta 2 is running.
 * @returns true if Rosetta is running, false otherwise.
 */
function isRosettaRunning(): boolean {
  try {
    logger().logDebug('$ /usr/bin/pgrep oahd');
    const pgrepOutput = execSync('/usr/bin/pgrep oahd', { encoding: 'utf-8' });
    logger().logDebug(pgrepOutput);
    return pgrepOutput.trim().length > 0;
  } catch (error: unknown) {
    // The command will return exit code 1 with empty stderr if the process is not found.
    // If the process is not found, then we can assume Rosetta is not running.
    if (error instanceof Object && 'status' in error && 'stderr' in error) {
      if (error.status === 1 && (error.stderr as string).trim().length === 0) {
        logger().logDebug('Command returned exit code 1 with empty stderr. Rosetta is not running.');
        return false;
      }
    }

    // Otherwise, something went wrong while running the command.
    logger().logErrorMessage('Failed to verify if rosetta is installed.');
    logger().logErrorMessage(JSON.stringify(error));
    showDialogForStatus(DetectRosettaStatus.ROSETTA_CHECK_FAILED);
    throw error;
  }
}

/**
 * Shows a dialog for the given DetectRosettaStatus.
 * @param status the DetectRosettaStatus to show a dialog for.
 * @param callback an optional callback function to run after the dialog is interacted with.
 */
function showDialogForStatus(status: DetectRosettaStatus, callback?: (selectedButtonId: number) => void): void {
  const dialogOptions = DETECT_ROSETTA_STATUS_MAP.get(status);
  if (!dialogOptions) {
    logger().logErrorMessage(`No dialog options found for status: ${status}`);
    return;
  }
  appState().modalTracker.trackElectronModalSync(async () =>
    dialog.showMessageBox(dialogOptions).then((retVal) => {
      if (callback) callback(retVal.response);
    }),
  );
}
