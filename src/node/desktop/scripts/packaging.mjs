/*
 * packaging.mjs
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

function isRHEL8OrRocky8() {
  try {
    const osRelease = execSync('cat /etc/os-release').toString();
    return (
      (osRelease.includes('RHEL') || osRelease.includes('Rocky')) &&
      osRelease.includes('VERSION_ID="8')
    );
  } catch {
    return false;
  }
}

const baseCommand = 'npm ci && electron-forge package';
const command = isRHEL8OrRocky8()
  ? `scl enable gcc-toolset-13 "${baseCommand}"`
  : baseCommand;

execSync(command, { stdio: 'inherit' });
