/*
 * main.test.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import path from 'path';
import { Application } from 'spectron';

// Find path to electron binary in node_modules
function getElectron(): string {
  const base = './node_modules/electron/dist/';
  switch (process.platform) {
  case 'darwin':
    return path.join(base, 'Electron.app/Contents/MacOS/Electron');
  case 'linux':
    return path.join(base, 'electron');
  case 'win32':
    return path.join(base, 'electron.exe');
  default:
    throw Error(`Unsupported platform: ${ process.platform }`);
  }
}

// Find path to RStudio entrypoint
function getMain(): string {
  return path.join(__dirname, '../../dist/main/app.js');
}

describe('Startup and Exit', function () {
  this.timeout(10000);

  beforeEach(function () {
    this.app = new Application({
      path: getElectron(),
      args: [getMain()]
    });
    return this.app.start();
  });

  afterEach(function () {
    if (this.app && this.app.isRunning()) {
      return this.app.stop();
    }
  });

  it('Shows a window after starting', async function () {
    const winCount: number = await this.app.client.getWindowCount();
    assert.equal(winCount, 1);
  });
});
