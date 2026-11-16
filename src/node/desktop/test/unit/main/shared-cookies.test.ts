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
import fs, { existsSync, mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'fs';
import { describe } from 'mocha';
import os from 'os';
import path from 'path';
import sinon from 'sinon';

import { shareBootstrapCookies } from '../../../src/main/shared-cookies';

// cookies don't depend on the port, so this stands for any local session's address
const kSessionUrl = 'http://127.0.0.1:41098/';
const kRemoteUrl = 'http://workbench.example.com/';
const kExpiry = Date.UTC(5000, 11, 31) / 1000;

let partitionCount = 0;
function freshSession(): Session {
  // a partition without the 'persist:' prefix lives in memory only
  return session.fromPartition(`shared-cookies-test-${++partitionCount}`);
}

async function cookieValues(s: Session, url = kSessionUrl): Promise<Record<string, string>> {
  const cookies = await s.cookies.get({ url });
  return Object.fromEntries(cookies.map((cookie) => [cookie.name, cookie.value]));
}

async function setCookie(s: Session, name: string, value: string, url = kSessionUrl): Promise<void> {
  await s.cookies.set({ url, name, value, expirationDate: kExpiry });
}

describe('shared-cookies', () => {
  let dir: string;
  let recordDir: string;

  beforeEach(() => {
    dir = mkdtempSync(path.join(os.tmpdir(), 'rstudio-shared-cookies-'));
    recordDir = path.join(dir, 'shared-cookies');
  });

  afterEach(() => {
    sinon.restore();
    rmSync(dir, { recursive: true, force: true });
  });

  function recordEntries(): string[] {
    return existsSync(recordDir) ? readdirSync(recordDir) : [];
  }

  function recordedValues(): Record<string, string> {
    const entries = recordEntries().filter(
      (entry) => entry.endsWith('.json') && statSync(path.join(recordDir, entry)).isFile(),
    );
    return Object.fromEntries(
      entries.map((entry) => [
        path.basename(entry, '.json'),
        (JSON.parse(readFileSync(path.join(recordDir, entry), 'utf8')) as { value: string }).value,
      ]),
    );
  }

  function writeRecord(name: string, content: string): void {
    mkdirSync(recordDir, { recursive: true });
    writeFileSync(path.join(recordDir, `${name}.json`), content);
  }

  // the record is written after cookie change events, which arrive asynchronously
  async function waitForRecord(expected: Record<string, string>): Promise<void> {
    const deadline = Date.now() + 5000;
    while (Date.now() < deadline) {
      try {
        if (JSON.stringify(recordedValues()) === JSON.stringify(expected)) {
          return;
        }
      } catch {
        // caught mid-write; look again
      }
      await new Promise((resolve) => setTimeout(resolve, 20));
    }
    assert.deepEqual(recordedValues(), expected);
  }

  async function primaryWith(cookies: Record<string, string>): Promise<Session> {
    const s = freshSession();
    for (const [name, value] of Object.entries(cookies)) {
      await setCookie(s, name, value);
    }
    await shareBootstrapCookies(s, recordDir, true);
    return s;
  }

  async function secondary(): Promise<Session> {
    const s = freshSession();
    await shareBootstrapCookies(s, recordDir, false);
    return s;
  }

  it('carries the primary instance cookies over to a secondary instance', async () => {
    const primary = await primaryWith({ LOCALE: 'fr', WEBDIALOG: '1', other: 'x' });

    const second = await secondary();
    assert.deepEqual(await cookieValues(second), { LOCALE: 'fr', WEBDIALOG: '1' });

    // Chromium caps the requested expiry, so compare with what the primary instance stored
    const [original] = await primary.cookies.get({ name: 'LOCALE' });
    const [restored] = await second.cookies.get({ name: 'LOCALE' });
    assert.isFalse(restored.session);
    assert.strictEqual(restored.expirationDate, original.expirationDate);
    assert.isTrue(restored.hostOnly);
  });

  it('records a change made later in any instance', async () => {
    const primary = await primaryWith({ LOCALE: 'fr' });
    const second = await secondary();

    await setCookie(second, 'WEBDIALOG', '1');
    await waitForRecord({ LOCALE: 'fr', WEBDIALOG: '1' });

    await primary.cookies.remove(kSessionUrl, 'LOCALE');
    await waitForRecord({ WEBDIALOG: '1' });

    assert.deepEqual(await cookieValues(await secondary()), { WEBDIALOG: '1' });
  });

  it('records the new value when a cookie is changed, without removing the record first', async () => {
    const primary = await primaryWith({ LOCALE: 'fr' });
    const rm = sinon.spy(fs, 'rmSync');

    await setCookie(primary, 'LOCALE', 'de');
    await waitForRecord({ LOCALE: 'de' });

    // a secondary instance starting in between would otherwise find no record
    const recordFile = path.join(recordDir, 'LOCALE.json');
    assert.isFalse(rm.calledWith(recordFile));
  });

  it('leaves cookies of other hosts out', async () => {
    const primary = freshSession();
    await setCookie(primary, 'LOCALE', 'de', kRemoteUrl);
    await setCookie(primary, 'LOCALE', 'fr');
    await shareBootstrapCookies(primary, recordDir, true);
    assert.deepEqual(recordedValues(), { LOCALE: 'fr' });

    await setCookie(primary, 'WEBDIALOG', '1', kRemoteUrl);
    await primary.cookies.remove(kRemoteUrl, 'LOCALE');
    await setCookie(primary, 'WEBDIALOG', '1');
    await waitForRecord({ LOCALE: 'fr', WEBDIALOG: '1' });

    const second = await secondary();
    assert.deepEqual(await cookieValues(second), { LOCALE: 'fr', WEBDIALOG: '1' });
    assert.deepEqual(await cookieValues(second, kRemoteUrl), {});
  });

  it('records that the primary instance has none of the cookies', async () => {
    writeRecord('LOCALE', JSON.stringify({ value: 'fr' }));
    await primaryWith({});
    assert.deepEqual(recordEntries(), []);
  });

  it('starts a secondary instance without the cookies when none were recorded', async () => {
    assert.deepEqual(await cookieValues(await secondary()), {});
  });

  it('ignores a malformed record', async () => {
    for (const content of ['not json', '[]', 'null', JSON.stringify({ value: 3 })]) {
      writeRecord('LOCALE', content);
      assert.deepEqual(await cookieValues(await secondary()), {}, content);
    }
  });

  it('restores the other cookie when one record cannot be read', async () => {
    mkdirSync(path.join(recordDir, 'LOCALE.json'), { recursive: true });
    writeRecord('WEBDIALOG', JSON.stringify({ value: '1' }));
    assert.deepEqual(await cookieValues(await secondary()), { WEBDIALOG: '1' });
  });

  it('restores the other cookie when one cannot be set', async () => {
    writeRecord('LOCALE', JSON.stringify({ value: 'fr' }));
    writeRecord('WEBDIALOG', JSON.stringify({ value: '1' }));
    const s = freshSession();
    const set = s.cookies.set.bind(s.cookies);
    sinon.stub(s.cookies, 'set').callsFake(async (details) => {
      if (details.name === 'LOCALE') {
        throw new Error('cookie store unavailable');
      }
      return set(details);
    });
    await shareBootstrapCookies(s, recordDir, false);
    assert.deepEqual(await cookieValues(s), { WEBDIALOG: '1' });
  });

  it('keeps the record when the primary instance cannot read its cookies', async () => {
    writeRecord('LOCALE', JSON.stringify({ value: 'fr' }));
    const s = freshSession();
    sinon.stub(s.cookies, 'get').rejects(new Error('cookie store unavailable'));
    await shareBootstrapCookies(s, recordDir, true);
    assert.deepEqual(recordedValues(), { LOCALE: 'fr' });
  });

  it('leaves no temporary file behind when a record cannot be replaced', async () => {
    const primary = await primaryWith({});
    mkdirSync(path.join(recordDir, 'LOCALE.json'), { recursive: true });

    await setCookie(primary, 'LOCALE', 'fr');
    // changes are recorded in order, so once this one is, the LOCALE one has been tried
    await setCookie(primary, 'WEBDIALOG', '1');
    await waitForRecord({ WEBDIALOG: '1' });

    assert.sameMembers(recordEntries(), ['LOCALE.json', 'WEBDIALOG.json']);
  });

  it('keeps going when a record and its temporary file can be neither replaced nor removed', async () => {
    const primary = await primaryWith({});
    const denied = (): never => {
      throw Object.assign(new Error('access denied'), { code: 'EACCES' });
    };
    sinon.stub(fs, 'renameSync').callsFake(denied);
    const rm = sinon.stub(fs, 'rmSync').callsFake(denied);

    // a throw here would escape the cookie change listener and fail the test
    await setCookie(primary, 'LOCALE', 'fr');
    const deadline = Date.now() + 5000;
    while (!rm.called && Date.now() < deadline) {
      await new Promise((resolve) => setTimeout(resolve, 20));
    }
    assert.match(String(rm.firstCall.args[0]), /LOCALE\.json\.\d+\.tmp$/);
  });
});
