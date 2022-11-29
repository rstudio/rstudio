/*
 * build-info.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import { BrowserWindow } from 'electron';

describe('BrowserWindow', () => {

  // skip Windows because it fails in the build because of networking
  // [3672:1119/035441.205:ERROR:network_change_notifier_win.cc(225)] WSALookupServiceBegin failed with: 0
  (process.platform === 'win32' ? it.skip : it)('fires events in the expected order', async () => {
    const win = new BrowserWindow({ show: false });
    const eventHistory: string[] = [];

    const events = [
      'did-start-loading',
      'did-start-navigation',
      'did-fail-load',
    ];

    for (const event of events) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      win.webContents.on(event as any, () => {
        eventHistory.push(event);
      });
    }

    await win.loadURL('http://localhost:9875')
      .then(() => assert.fail())
      .catch(() => {
        assert.deepEqual(eventHistory, events);
      });
  });

});

