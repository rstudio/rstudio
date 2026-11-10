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
import { checkValid } from '../../../src/ui/widgets/choose-r';

describe('ChooseR', () => {
  beforeEach(() => {
    setApplication(new Application());
  });

  afterEach(() => {
    sinon.restore();
    clearApplicationSingleton();
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
