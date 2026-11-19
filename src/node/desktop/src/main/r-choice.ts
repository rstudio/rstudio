/*
 * r-choice.ts
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

// The version of R chosen in RStudio, kept across launches in the desktop
// options. With no choice saved, RStudio uses the default R: the one named by
// RSTUDIO_WHICH_R, or else the platform's default installation.

import path from 'path';

import { getenv } from '../core/environment';
import { ElectronDesktopOptions, fixWindowsRExecutablePath } from './preferences/electron-desktop-options';

// RSTUDIO_WHICH_R as RStudio was launched with it. Windows startup sets it
// to the R it picked, so the launch value is kept aside, as the PATH is in
// detect-r.ts.
const kLaunchRStudioWhichR = getenv('RSTUDIO_WHICH_R');

export function launchRStudioWhichR(): string {
  return kLaunchRStudioWhichR;
}

export interface RChoice {
  /** the R executable chosen */
  path: string;

  /** RSTUDIO_WHICH_R when it was chosen ('' when unset) */
  rstudioWhichR: string;
}

/**
 * The R chosen in RStudio, or null when RStudio is to use the default R. On
 * Windows, asking for a default installation (in the Choose R dialog) is not
 * a choice of a particular R.
 */
export function savedRChoice(platform = process.platform): RChoice | null {
  const options = ElectronDesktopOptions();
  if (platform === 'win32' && (options.useDefault32BitR() || options.useDefault64BitR())) {
    return null;
  }

  const path = options.rExecutablePath();
  if (!path) {
    return null;
  }

  return { path, rstudioWhichR: options.rstudioWhichRWhenChosen() };
}

/**
 * Whether a saved choice still decides which R RStudio uses: it does while
 * RSTUDIO_WHICH_R, which otherwise would, is as it was when the choice was
 * made. A different value is the newer choice (rig's default R changed, or
 * RStudio was launched from a shell that sets it). Without this, rig's
 * LaunchAgent on macOS, which sets RSTUDIO_WHICH_R for every application,
 * would override any R chosen in RStudio.
 */
export function rChoiceApplies(choice: RChoice, rstudioWhichR = kLaunchRStudioWhichR): boolean {
  return choice.rstudioWhichR === rstudioWhichR;
}

/**
 * Forget a saved choice that RSTUDIO_WHICH_R has superseded (see
 * rChoiceApplies), so that it doesn't apply again should the variable go
 * back to its old value: after `rig default 4.6` and then `rig default 4.5`,
 * the user expects 4.5, not an R chosen in RStudio before either. Run once
 * at startup, before anything reads the choice. Returns whether a choice was
 * forgotten.
 */
export function forgetSupersededRChoice(platform = process.platform): boolean {
  const choice = savedRChoice(platform);
  if (choice === null || rChoiceApplies(choice)) {
    return false;
  }

  clearRChoice(platform);
  return true;
}

/**
 * Whether two paths name the same R executable, as the paths they are:
 * a link (rig's /usr/local/bin/R, say) is not the R it points to, as it
 * follows whichever R it is pointed at next. On Windows, case and
 * separators don't matter, and bin\R.exe is the architecture's executable.
 */
export function sameRExecutable(a: string, b: string, platform = process.platform): boolean {
  if (!a || !b) {
    return false;
  }

  if (platform === 'win32') {
    const normalize = (rPath: string) => fixWindowsRExecutablePath(rPath).replace(/\\/g, '/').toLowerCase();
    return normalize(a) === normalize(b);
  }

  return path.normalize(a) === path.normalize(b);
}

/** Remember the given R as the one RStudio uses from now on. */
export function saveRChoice(rPath: string, platform = process.platform): void {
  const options = ElectronDesktopOptions();

  // the default installations would otherwise take precedence
  if (platform === 'win32') {
    options.setUseDefault32BitR(false);
    options.setUseDefault64BitR(false);
  }

  options.setRExecutablePath(rPath);
  options.setRStudioWhichRWhenChosen(kLaunchRStudioWhichR);
}

/**
 * Forget the chosen R, so that RStudio uses the default R from now on. On
 * Windows this asks for the default 64-bit installation, as the Choose R
 * dialog does, which keeps the stored path to fall back on.
 */
export function clearRChoice(platform = process.platform): void {
  const options = ElectronDesktopOptions();
  if (platform === 'win32') {
    options.setUseDefault32BitR(false);
    options.setUseDefault64BitR(true);
  } else {
    options.setRExecutablePath('');
  }

  options.setRStudioWhichRWhenChosen('');
}
