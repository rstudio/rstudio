/*
 * shared-cookies.test.ts
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

import { assert } from 'chai';
import { session, Session } from 'electron';
import { existsSync, mkdirSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'fs';
import { describe } from 'mocha';
import os from 'os';
import path from 'path';

import { shareBootstrapCookies } from '../../../src/main/shared-cookies';

// cookies don't depend on the port, so this stands for any session's address
const kSessionUrl = 'http://127.0.0.1:41098/';
const kExpiry = Date.UTC(5000, 11, 31) / 1000;

let partitionCount = 0;
function freshSession(): Session {
  // a partition without the 'persist:' prefix lives in memory only
  return session.fromPartition(`shared-cookies-test-${++partitionCount}`);
}

async function cookieValues(s: Session): Promise<Record<string, string>> {
  const cookies = await s.cookies.get({ url: kSessionUrl });
  return Object.fromEntries(cookies.map((cookie) => [cookie.name, cookie.value]));
}

describe('shared-cookies', () => {
  let dir: string;
  let recordDir: string;

  beforeEach(() => {
    dir = mkdtempSync(path.join(os.tmpdir(), 'rstudio-shared-cookies-'));
    recordDir = path.join(dir, 'shared-cookies');
  });

  afterEach(() => {
    rmSync(dir, { recursive: true, force: true });
  });

  function recordedNames(): string[] {
    if (!existsSync(recordDir)) {
      return [];
    }
    return readdirSync(recordDir).map((entry) => path.basename(entry, '.json'));
  }

  function writeRecord(name: string, content: string): void {
    mkdirSync(recordDir, { recursive: true });
    writeFileSync(path.join(recordDir, `${name}.json`), content);
  }

  // the record is written after cookie change events, which arrive asynchronously
  async function waitForRecord(predicate: (names: string[]) => boolean): Promise<void> {
    const deadline = Date.now() + 5000;
    while (Date.now() < deadline) {
      if (predicate(recordedNames())) {
        return;
      }
      await new Promise((resolve) => setTimeout(resolve, 20));
    }
    assert.fail(`the recorded cookies never matched; last: ${recordedNames().join(', ')}`);
  }

  async function primaryWith(cookies: Record<string, string>): Promise<Session> {
    const s = freshSession();
    for (const [name, value] of Object.entries(cookies)) {
      await s.cookies.set({ url: kSessionUrl, name, value, expirationDate: kExpiry });
    }
    await shareBootstrapCookies(s, recordDir, true);
    return s;
  }

  it('carries the primary instance cookies over to a secondary instance', async () => {
    const primary = await primaryWith({ LOCALE: 'fr', WEBDIALOG: '1', other: 'x' });

    const secondary = freshSession();
    await shareBootstrapCookies(secondary, recordDir, false);
    assert.deepEqual(await cookieValues(secondary), { LOCALE: 'fr', WEBDIALOG: '1' });

    // Chromium caps the requested expiry, so compare with what the primary instance stored
    const [original] = await primary.cookies.get({ name: 'LOCALE' });
    const [restored] = await secondary.cookies.get({ name: 'LOCALE' });
    assert.isFalse(restored.session);
    assert.strictEqual(restored.expirationDate, original.expirationDate);
    assert.isTrue(restored.hostOnly);
  });

  it('records a change made later in any instance', async () => {
    const primary = await primaryWith({ LOCALE: 'fr' });
    const secondary = freshSession();
    await shareBootstrapCookies(secondary, recordDir, false);

    await secondary.cookies.set({ url: kSessionUrl, name: 'WEBDIALOG', value: '1', expirationDate: kExpiry });
    await waitForRecord((names) => names.includes('WEBDIALOG'));

    await primary.cookies.remove(kSessionUrl, 'LOCALE');
    await waitForRecord((names) => !names.includes('LOCALE'));

    const third = freshSession();
    await shareBootstrapCookies(third, recordDir, false);
    assert.deepEqual(await cookieValues(third), { WEBDIALOG: '1' });
  });

  it('records that the primary instance has none of the cookies', async () => {
    writeRecord('LOCALE', JSON.stringify({ name: 'LOCALE', value: 'fr', domain: '127.0.0.1', path: '/' }));
    await primaryWith({});
    assert.deepEqual(recordedNames(), []);
  });

  it('starts a secondary instance without the cookies when none were recorded', async () => {
    const secondary = freshSession();
    await shareBootstrapCookies(secondary, recordDir, false);
    assert.deepEqual(await cookieValues(secondary), {});
  });

  it('ignores a malformed record', async () => {
    const cookie = { name: 'LOCALE', value: 'fr', domain: '127.0.0.1', path: '/' };
    const malformed = ['not json', '[]', { ...cookie, value: 3 }, { ...cookie, name: 'WEBDIALOG' }];
    for (const content of malformed.map((entry) => (typeof entry === 'string' ? entry : JSON.stringify(entry)))) {
      writeRecord('LOCALE', content);
      const secondary = freshSession();
      await shareBootstrapCookies(secondary, recordDir, false);
      assert.deepEqual(await cookieValues(secondary), {}, content);
    }
  });
});
