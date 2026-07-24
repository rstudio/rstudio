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

import { execFileSync, execSync } from 'child_process';
import { dialog } from 'electron';
import { t } from 'i18next';
import { logger } from '../core/logger';

// Instructions shown when an Intel build of R is selected on Apple Silicon
// without Rosetta 2 available.
const kRosettaInstallUrl =
  'https://docs.posit.co/ide/desktop-pro/getting_started/installation.html#apple-silicon-mac-m1m2';

/**
 * Ensures Rosetta 2 is available before launching an x86_64 (Intel) build of R
 * on Apple Silicon.
 *
 * RStudio's own components all have native arm64 builds, so RStudio itself never
 * needs Rosetta 2. It is required only to run an Intel build of R, because the session
 * process must match R's architecture and runs as x86_64 under Rosetta in that
 * case. When Rosetta is missing that session cannot start at all, so this shows
 * a blocking error with install guidance instead of letting the launch fail with
 * an opaque "Bad CPU type in executable" crash. Only call this once the selected
 * R has been determined to be x86_64.
 *
 * @returns true if the launch may proceed (Rosetta present, not Apple Silicon,
 *          or the check could not be performed); false if the caller must abort
 *          the launch because Rosetta is required but not installed.
 */
export function ensureRosettaForIntelR(): boolean {
  const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
  if (!isAppleSilicon) {
    return true;
  }

  if (isRosettaInstalled()) {
    return true;
  }

  logger().logDebug('Selected R is Intel (x86_64) but Rosetta 2 is not installed; cannot start session.');
  const response = dialog.showMessageBoxSync({
    type: 'error',
    buttons: [t('detectRosetta.installButton'), t('detectRosetta.quitButton')],
    defaultId: 0,
    cancelId: 1,
    title: t('detectRosetta.requiredTitle'),
    message: t('detectRosetta.requiredMessage'),
    detail: t('detectRosetta.requiredDetail'),
  });

  if (response === 0) {
    // Open the instructions synchronously via `open` so the browser launch is
    // handed off to the OS before the caller exits the app; shell.openExternal
    // is asynchronous and could be cut off by app.exit().
    try {
      execFileSync('/usr/bin/open', [kRosettaInstallUrl]);
    } catch (error: unknown) {
      logger().logErrorMessage('Failed to open Rosetta 2 install instructions.');
      logger().logError(error);
    }
  }

  return false;
}

/**
 * Detects whether Rosetta 2 is installed on this Apple Silicon Mac.
 *
 * On macOS, Rosetta 2 is internally referred to as OAH and its daemon is 'oahd',
 * which is expected to be running whenever Rosetta is installed; we treat a
 * running 'oahd' as installed. If the probe fails unexpectedly we cannot tell, so
 * we assume it is present and let the launch proceed rather than wrongly block a
 * working setup.
 *
 * @returns true if Rosetta 2 appears installed, false if it is definitively not.
 */
function isRosettaInstalled(): boolean {
  try {
    logger().logDebug('$ /usr/bin/pgrep oahd');
    const pgrepOutput = execSync('/usr/bin/pgrep oahd', { encoding: 'utf-8' });
    return pgrepOutput.trim().length > 0;
  } catch (error: unknown) {
    // pgrep exits 1 with empty stderr when 'oahd' is not found: Rosetta absent.
    if (error instanceof Object && 'status' in error && 'stderr' in error) {
      if (error.status === 1 && (error.stderr as string).trim().length === 0) {
        return false;
      }
    }

    // Something else went wrong; we can't determine the state, so assume Rosetta
    // is present and let the launch proceed rather than block it.
    logger().logErrorMessage('Failed to check for a Rosetta 2 installation; assuming it is present.');
    logger().logError(error);
    return true;
  }
}
