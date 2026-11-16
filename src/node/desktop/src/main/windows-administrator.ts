/*
 * windows-administrator.ts
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

import { CommandRunner } from './running-instances';

// Whether the Windows user can consent to the UAC prompt an all-users
// install raises. An administrator (even one running without elevation, as
// UAC arranges) gets a consent prompt; anyone else gets a credentials prompt
// for an account they most likely do not have, after which the installer
// gives up. The updater offers an all-users install only to the former.

/** The well-known SID of the local Administrators group. */
export const kAdministratorsSid = 'S-1-5-32-544';

/**
 * Whether `whoami /groups /fo csv /nh` output lists the Administrators
 * group. UAC strips the group from a non-elevated administrator's token for
 * most purposes but still lists it ("Group used for deny only"), which is
 * exactly the membership that lets the user consent.
 */
export function parseAdministratorMembership(output: string): boolean {
  return output.split(/\r?\n/).some((line) => line.includes(`"${kAdministratorsSid}"`));
}

/** True when the current user is an administrator; null when that could not be read. */
export async function isAdministrator(run: CommandRunner): Promise<boolean | null> {
  const output = await run('whoami.exe', ['/groups', '/fo', 'csv', '/nh']);
  return output === null ? null : parseAdministratorMembership(output);
}
