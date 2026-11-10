/*
 * windows-administrator.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import { isAdministrator, parseAdministratorMembership } from '../../../src/main/windows-administrator';

// whoami /groups /fo csv /nh, for a non-elevated administrator (UAC lists
// the group as deny-only) and for a standard user
const adminGroups = [
  '"Everyone","Well-known group","S-1-1-0","Mandatory group, Enabled by default, Enabled group"',
  '"BUILTIN\\Administrators","Alias","S-1-5-32-544","Group used for deny only"',
  '"BUILTIN\\Users","Alias","S-1-5-32-545","Mandatory group, Enabled by default, Enabled group"',
].join('\r\n');
const userGroups = [
  '"Everyone","Well-known group","S-1-1-0","Mandatory group, Enabled by default, Enabled group"',
  '"BUILTIN\\Users","Alias","S-1-5-32-545","Mandatory group, Enabled by default, Enabled group"',
].join('\r\n');

describe('windows administrator check', () => {
  it('finds the Administrators group whatever its attributes', () => {
    assert.isTrue(parseAdministratorMembership(adminGroups));
    assert.isFalse(parseAdministratorMembership(userGroups));
    assert.isFalse(parseAdministratorMembership(''));
    // a longer SID that merely starts with the group's
    assert.isFalse(parseAdministratorMembership('"X","Alias","S-1-5-32-5440","Enabled group"'));
  });

  it('asks whoami and answers null when it cannot', async () => {
    const calls: string[] = [];
    const run = async (command: string, args: string[]) => {
      calls.push(`${command} ${args.join(' ')}`);
      return adminGroups;
    };
    assert.isTrue(await isAdministrator(run));
    assert.deepEqual(calls, ['whoami.exe /groups /fo csv /nh']);
    assert.isFalse(await isAdministrator(async () => userGroups));
    assert.isNull(await isAdministrator(async () => null));
  });
});
