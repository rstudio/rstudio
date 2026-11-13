/*
 * choose-r.test.ts
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
import sinon from 'sinon';
import { dialog } from 'electron';
import { mkdtempSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';

import { Application } from '../../../src/main/application';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { changeLanguage, initI18n } from '../../../src/main/i18n-manager';
import { checkValid, ChooseRModalWindow } from '../../../src/ui/widgets/choose-r';
import i18next from 'i18next';

describe('ChooseR', () => {
  beforeEach(() => {
    setApplication(new Application());
  });

  afterEach(() => {
    sinon.restore();
    clearApplicationSingleton();
  });

  it('keeps the current UI language when the chooser opens', async () => {
    // webpack defines these in real builds; the constructor only passes them along
    const globals = globalThis as unknown as Record<string, string>;
    globals.CHOOSE_R_WEBPACK_ENTRY = 'about:blank';
    globals.CHOOSE_R_PRELOAD_WEBPACK_ENTRY = '';

    initI18n();
    await changeLanguage('fr');
    const chooser = new ChooseRModalWindow([]);
    try {
      assert.strictEqual(i18next.resolvedLanguage, 'fr');
    } finally {
      // the dialog's 'closed' handler uses the application singleton, which afterEach clears
      const closed = new Promise<void>((resolve) => chooser.once('closed', () => resolve()));
      chooser.destroy();
      await closed;
      delete globals.CHOOSE_R_WEBPACK_ENTRY;
      delete globals.CHOOSE_R_PRELOAD_WEBPACK_ENTRY;
      await changeLanguage('en');
    }
  });

  it('tells the user what a chosen R that fails to start reported', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    const dir = mkdtempSync(join(tmpdir(), 'choose-r-fatal-'));
    const brokenR = join(dir, 'R');
    writeFileSync(brokenR, '#!/bin/sh\necho "Fatal error: cannot create \'R_TempDir\'" >&2\nexit 2\n', { mode: 0o755 });
    const showMessageBox = sinon.stub(dialog, 'showMessageBoxSync').returns(0);

    try {
      assert.isFalse(checkValid({ binaryPath: brokenR }));
      assert.isTrue(showMessageBox.calledOnce);

      // the dialog's message is generic; its detail names the R and what it said
      const options = showMessageBox.firstCall.args[0] as Electron.MessageBoxSyncOptions;
      assert.equal(options.type, 'error');
      assert.include(options.detail, brokenR);
      assert.include(options.detail, 'Exit status: 2');
      assert.include(options.detail, "Fatal error: cannot create 'R_TempDir'");
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });
});
