/*
 * r-choice.test.ts
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

import {
  clearRChoice,
  launchRStudioWhichR,
  rChoiceApplies,
  sameRExecutable,
  saveRChoice,
  savedRChoice,
} from '../../../src/main/r-choice';
import { fakeROptions } from './fake-r-options';

describe('RChoice', () => {
  afterEach(() => {
    sinon.restore();
  });

  describe('savedRChoice', () => {
    it('is null when no R was chosen', () => {
      fakeROptions();
      assert.isNull(savedRChoice('linux'));
    });

    it('is the chosen R, with RSTUDIO_WHICH_R as it was then', () => {
      fakeROptions({ path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '/opt/R/4.5.0/bin/R' });
      assert.deepEqual(savedRChoice('darwin'), { path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '/opt/R/4.5.0/bin/R' });
    });

    it('is null on Windows when the default installation was asked for', () => {
      fakeROptions({ path: 'C:/R/R-4.4.1/bin/x64/R.exe', useDefault64BitR: true });
      assert.isNull(savedRChoice('win32'));
    });
  });

  describe('rChoiceApplies', () => {
    const choice = { path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '/opt/R/4.5.0/bin/R' };

    it('applies while RSTUDIO_WHICH_R is as it was when the R was chosen', () => {
      assert.isTrue(rChoiceApplies(choice, '/opt/R/4.5.0/bin/R'));
    });

    it('gives way to an RSTUDIO_WHICH_R set since', () => {
      assert.isFalse(rChoiceApplies(choice, '/opt/R/4.6.0/bin/R'));
      assert.isFalse(rChoiceApplies(choice, ''));
    });

    it('compares against RSTUDIO_WHICH_R as RStudio was launched with it', () => {
      assert.isTrue(rChoiceApplies({ path: '/opt/R/4.4.1/bin/R', rstudioWhichR: launchRStudioWhichR() }));
    });
  });

  describe('saveRChoice', () => {
    it('stores the R with RSTUDIO_WHICH_R as RStudio was launched with it', () => {
      const options = fakeROptions();

      saveRChoice('/opt/R/4.4.1/bin/R', 'linux');

      assert.isTrue(options.setRExecutablePath.calledOnceWithExactly('/opt/R/4.4.1/bin/R'));
      assert.isTrue(options.setRStudioWhichRWhenChosen.calledOnceWithExactly(launchRStudioWhichR()));
      assert.isFalse(options.setUseDefault64BitR.called);
    });

    it('stops asking for the default installations on Windows', () => {
      const options = fakeROptions({ useDefault64BitR: true });

      saveRChoice('C:/R/R-4.4.1/bin/x64/R.exe', 'win32');

      assert.isTrue(options.setUseDefault32BitR.calledOnceWithExactly(false));
      assert.isTrue(options.setUseDefault64BitR.calledOnceWithExactly(false));
      assert.isTrue(options.setRExecutablePath.calledOnceWithExactly('C:/R/R-4.4.1/bin/x64/R.exe'));
    });
  });

  describe('clearRChoice', () => {
    it('forgets the chosen R on macOS and Linux', () => {
      const options = fakeROptions({ path: '/opt/R/4.4.1/bin/R' });

      clearRChoice('darwin');

      assert.isTrue(options.setRExecutablePath.calledOnceWithExactly(''));
      assert.isTrue(options.setRStudioWhichRWhenChosen.calledOnceWithExactly(''));
    });

    it('asks for the default 64-bit installation on Windows, keeping the path to fall back on', () => {
      const options = fakeROptions({ path: 'C:/R/R-4.4.1/bin/x64/R.exe' });

      clearRChoice('win32');

      assert.isTrue(options.setUseDefault64BitR.calledOnceWithExactly(true));
      assert.isTrue(options.setUseDefault32BitR.calledOnceWithExactly(false));
      assert.isFalse(options.setRExecutablePath.called);
    });
  });

  describe('sameRExecutable', () => {
    it('compares paths as given, so a link is not the R it points to', () => {
      assert.isTrue(sameRExecutable('/opt/R/4.4.1/bin/R', '/opt/R/4.4.1/bin/../bin/R', 'linux'));
      assert.isFalse(sameRExecutable('/usr/local/bin/R', '/opt/R/4.4.1/bin/R', 'linux'));
    });

    it('ignores case and separators on Windows', () => {
      assert.isTrue(sameRExecutable('C:\\R\\R-4.4.1\\bin\\x64\\R.exe', 'c:/r/r-4.4.1/bin/x64/r.exe', 'win32'));
    });

    it('never matches an empty path', () => {
      assert.isFalse(sameRExecutable('', '', 'linux'));
      assert.isFalse(sameRExecutable('/opt/R/4.4.1/bin/R', '', 'linux'));
    });
  });
});
