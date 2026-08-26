/*
 * whats-new-state.test.ts
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

import { describe, it, beforeEach, afterEach } from 'mocha';
import { assert } from 'chai';
import fs from 'fs';
import path from 'path';
import os from 'os';

import { WhatsNewState } from '../../../src/main/whats-new-state';
import { randomString } from '../../../src/main/utils';

describe('WhatsNewState', () => {
  let tmpDir: string;
  let state: WhatsNewState;

  beforeEach(() => {
    tmpDir = path.join(os.tmpdir(), 'whats-new-test-' + randomString());
    fs.mkdirSync(tmpDir, { recursive: true });
    state = new WhatsNewState(tmpDir);
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it('reports unseen release as not seen', () => {
    assert.isFalse(state.hasSeenRelease('Globemaster Allium'));
  });

  it('marks a release as seen', () => {
    state.markReleaseSeen('Globemaster Allium', 0);
    assert.isTrue(state.hasSeenRelease('Globemaster Allium'));
  });

  it('tracks multiple releases independently', () => {
    state.markReleaseSeen('Globemaster Allium', 0);
    state.markReleaseSeen('Prairie Trillium', 1);
    assert.isTrue(state.hasSeenRelease('Globemaster Allium'));
    assert.isTrue(state.hasSeenRelease('Prairie Trillium'));
    assert.isFalse(state.hasSeenRelease('Sea Holly'));
  });

  it('records the running patch level, which older builds read', () => {
    state.markReleaseSeen('Globemaster Allium', 2);
    assert.deepEqual(state.seenReleases(), [{ name: 'Globemaster Allium', patch: 2 }]);
  });

  it('marking a seen release again leaves the entry alone', () => {
    state.markReleaseSeen('Globemaster Allium', 0);
    state.markReleaseSeen('Globemaster Allium', 1);
    assert.deepEqual(state.seenReleases(), [{ name: 'Globemaster Allium', patch: 0 }]);
  });

  it('persists across instances', () => {
    state.markReleaseSeen('Globemaster Allium', 0);
    const state2 = new WhatsNewState(tmpDir);
    assert.isTrue(state2.hasSeenRelease('Globemaster Allium'));
    assert.isFalse(state2.hasSeenRelease('Sea Holly'));
  });

  it('honors seen-state written by a version that recorded a patch level', () => {
    fs.writeFileSync(
      path.join(tmpDir, 'whats-new-state.json'),
      JSON.stringify({ seenReleases: [{ name: 'Globemaster Allium', patch: 0 }] }),
    );
    const state2 = new WhatsNewState(tmpDir);
    assert.isTrue(state2.hasSeenRelease('Globemaster Allium'));
  });
});
