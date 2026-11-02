/*
 * windows-install-mode.ts
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

import { execFile } from 'child_process';
import path from 'path';

/**
 * The install mode of the running RStudio, as the NsisMultiUser installer
 * understands it. A silent reinstall must name the mode (`/allusers` or
 * `/currentuser`); the installer refuses to guess.
 */
export type WindowsInstallMode = 'allusers' | 'currentuser';

/** Uninstall key written by the installer (MULTIUSER_INSTALLMODE_UNINSTALL_REGISTRY_KEY). */
const kUninstallKey = 'Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\RStudio';

/**
 * Extract a REG_SZ value from `reg query ... /v <name>` output, e.g.
 *
 *     HKEY_CURRENT_USER\Software\...\Uninstall\RStudio
 *         InstallLocation    REG_SZ    C:\Users\me\AppData\Local\Programs\RStudio
 *
 * Returns '' when the value is absent.
 */
export function parseRegQueryValue(output: string, valueName: string): string {
  const pattern = new RegExp(`^\\s*${valueName}\\s+REG_(?:EXPAND_)?SZ\\s+(.*?)\\s*$`, 'im');
  const match = pattern.exec(output);
  return match ? match[1] : '';
}

function samePath(a: string, b: string): boolean {
  if (!a || !b) {
    return false;
  }
  const normalize = (p: string) =>
    path.win32
      .normalize(p)
      .replace(/[\\/]+$/, '')
      .toLowerCase();
  return normalize(a) === normalize(b);
}

/**
 * Decide which install mode owns `exeDir`, given the InstallLocation values
 * registered under HKCU and HKLM. Null when neither matches: an unpacked
 * zip ("xcopy") install or a layout this code does not recognize, which the
 * updater must not try to replace silently.
 */
export function windowsInstallModeFor(
  exeDir: string,
  locations: { currentUser: string; allUsers: string },
): WindowsInstallMode | null {
  if (samePath(exeDir, locations.currentUser)) {
    return 'currentuser';
  }
  if (samePath(exeDir, locations.allUsers)) {
    return 'allusers';
  }
  return null;
}

async function regQueryInstallLocation(hive: 'HKCU' | 'HKLM'): Promise<string> {
  return new Promise((resolve) => {
    // reg.exe exits non-zero when the key or value is missing; that just
    // means "not installed in this mode"
    execFile(
      'reg.exe',
      ['query', `${hive}\\${kUninstallKey}`, '/v', 'InstallLocation', '/reg:64'],
      { windowsHide: true },
      (error, stdout) => resolve(error ? '' : parseRegQueryValue(stdout, 'InstallLocation')),
    );
  });
}

/** The install mode of the RStudio at `exeDir`, read from the registry. */
export async function detectWindowsInstallMode(exeDir: string): Promise<WindowsInstallMode | null> {
  const [currentUser, allUsers] = await Promise.all([regQueryInstallLocation('HKCU'), regQueryInstallLocation('HKLM')]);
  return windowsInstallModeFor(exeDir, { currentUser, allUsers });
}
