/*
 * fake-r-options.ts
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

import sinon from 'sinon';

import * as DesktopOptions from '../../../src/main/preferences/electron-desktop-options';
import { DesktopOptionsImpl } from '../../../src/main/preferences/electron-desktop-options';

export interface FakeROptions {
  useDefault32BitR: sinon.SinonStub;
  useDefault64BitR: sinon.SinonStub;
  rExecutablePath: sinon.SinonStub;
  rstudioWhichRWhenChosen: sinon.SinonStub;
  setUseDefault32BitR: sinon.SinonStub;
  setUseDefault64BitR: sinon.SinonStub;
  setRExecutablePath: sinon.SinonStub;
  setRStudioWhichRWhenChosen: sinon.SinonStub;
}

/**
 * Stand in for the desktop options with the given choice of R stored; the
 * caller restores them (sinon.restore()).
 */
export function fakeROptions(
  stored: { path?: string; rstudioWhichR?: string; useDefault64BitR?: boolean } = {},
): FakeROptions {
  const options: FakeROptions = {
    useDefault32BitR: sinon.stub().returns(false),
    useDefault64BitR: sinon.stub().returns(stored.useDefault64BitR ?? false),
    rExecutablePath: sinon.stub().returns(stored.path ?? ''),
    rstudioWhichRWhenChosen: sinon.stub().returns(stored.rstudioWhichR ?? ''),
    setUseDefault32BitR: sinon.stub(),
    setUseDefault64BitR: sinon.stub(),
    setRExecutablePath: sinon.stub(),
    setRStudioWhichRWhenChosen: sinon.stub(),
  };
  sinon.stub(DesktopOptions, 'ElectronDesktopOptions').returns(options as unknown as DesktopOptionsImpl);
  return options;
}
