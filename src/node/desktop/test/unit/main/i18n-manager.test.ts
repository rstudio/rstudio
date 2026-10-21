/*
 * i18n-manager.test.ts
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
import { describe } from 'mocha';
import i18next from 'i18next';

import { changeLanguage, initI18n } from '../../../src/main/i18n-manager';

// Tests run in the main process, which has no DOM; stand in for the renderer's document.
type GlobalWithDocument = { document?: { documentElement: { lang: string } } };
const globalWithDocument = globalThis as unknown as GlobalWithDocument;

describe('i18n-manager', () => {
  before(async () => {
    initI18n();
    await new Promise<void>((resolve) => {
      if (i18next.isInitialized) {
        resolve();
      } else {
        i18next.on('initialized', () => resolve());
      }
    });
  });

  afterEach(async () => {
    delete globalWithDocument.document;
    await changeLanguage('en');
  });

  it('sets the document language to the chosen UI language', async () => {
    globalWithDocument.document = { documentElement: { lang: 'en' } };
    await changeLanguage('fr');
    assert.strictEqual(globalWithDocument.document.documentElement.lang, 'fr');
  });

  it('sets the document language back to English', async () => {
    globalWithDocument.document = { documentElement: { lang: 'fr' } };
    await changeLanguage('en');
    assert.strictEqual(globalWithDocument.document.documentElement.lang, 'en');
  });

  it('uses the fallback language when the requested one has no translations', async () => {
    globalWithDocument.document = { documentElement: { lang: 'fr' } };
    await changeLanguage('de');
    assert.strictEqual(globalWithDocument.document.documentElement.lang, 'en');
  });

  it('changes the language without a document, as in the main process', async () => {
    await changeLanguage('fr');
    assert.strictEqual(i18next.resolvedLanguage, 'fr');
  });
});
