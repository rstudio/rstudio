/*
 * page-language.test.ts
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
import { ipcMain, session, Session } from 'electron';
import i18next from 'i18next';
import { describe } from 'mocha';
import sinon from 'sinon';

import { changeLanguage, initI18n } from '../../../src/main/i18n-manager';
import { kGetLanguageChannel, registerLanguageChannel, trackLocaleCookie } from '../../../src/main/page-language';

const kCookieUrl = 'http://127.0.0.1/';

let partitionCount = 0;
function freshSession(): Session {
  // a partition without the 'persist:' prefix lives in memory only
  return session.fromPartition(`page-language-test-${++partitionCount}`);
}

function languageSeenByPages(): unknown {
  const event = { returnValue: undefined as unknown };
  ipcMain.emit(kGetLanguageChannel, event);
  return event.returnValue;
}

async function nextLanguageChange(): Promise<string> {
  return new Promise((resolve) => {
    i18next.on('languageChanged', function handler(lng: string) {
      i18next.off('languageChanged', handler);
      resolve(lng);
    });
  });
}

describe('page-language', () => {
  before(async () => {
    initI18n();
    if (!i18next.isInitialized) {
      await new Promise<void>((resolve) => i18next.on('initialized', () => resolve()));
    }
    registerLanguageChannel();
  });

  after(() => {
    ipcMain.removeAllListeners(kGetLanguageChannel);
  });

  afterEach(async () => {
    sinon.restore();
    await changeLanguage('en');
  });

  it('uses the language stored in the LOCALE cookie', async () => {
    const s = freshSession();
    await s.cookies.set({ url: kCookieUrl, name: 'LOCALE', value: 'fr' });
    await trackLocaleCookie(s);
    assert.strictEqual(languageSeenByPages(), 'fr');
  });

  it('uses English when there is no LOCALE cookie', async () => {
    await changeLanguage('fr');
    const s = freshSession();
    await s.cookies.set({ url: kCookieUrl, name: 'other', value: 'fr' });
    await trackLocaleCookie(s);
    assert.strictEqual(languageSeenByPages(), 'en');
  });

  it('follows a LOCALE cookie set later', async () => {
    const s = freshSession();
    await trackLocaleCookie(s);
    const changed = nextLanguageChange();
    await s.cookies.set({ url: kCookieUrl, name: 'LOCALE', value: 'fr' });
    assert.strictEqual(await changed, 'fr');
    assert.strictEqual(languageSeenByPages(), 'fr');
  });

  it('keeps the current language when the cookie store cannot be read', async () => {
    const s = freshSession();
    sinon.stub(s.cookies, 'get').rejects(new Error('cookie store unavailable'));
    await trackLocaleCookie(s);
    assert.strictEqual(languageSeenByPages(), 'en');
  });
});
