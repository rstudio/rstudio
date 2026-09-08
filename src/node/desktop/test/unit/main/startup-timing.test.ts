/*
 * startup-timing.test.ts
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
import { existsSync, mkdtempSync, readFileSync, rmSync } from 'fs';
import { tmpdir } from 'os';
import path from 'path';
import { startupCheckpoint, startupTimingEnabled } from '../../../src/main/startup-timing';

describe('StartupTiming', () => {
  let dir: string;
  let file: string;
  const savedEnv = process.env.RSTUDIO_STARTUP_TIMING;

  beforeEach(() => {
    dir = mkdtempSync(path.join(tmpdir(), 'startup-timing-'));
    file = path.join(dir, 'timing.jsonl');
  });

  afterEach(() => {
    rmSync(dir, { recursive: true, force: true });
    if (savedEnv === undefined) {
      delete process.env.RSTUDIO_STARTUP_TIMING;
    } else {
      process.env.RSTUDIO_STARTUP_TIMING = savedEnv;
    }
  });

  it('is disabled and writes nothing when RSTUDIO_STARTUP_TIMING is unset', () => {
    delete process.env.RSTUDIO_STARTUP_TIMING;
    assert.isFalse(startupTimingEnabled());
    startupCheckpoint('unused');
    assert.isFalse(existsSync(file));
  });

  it('appends one JSON line per checkpoint when enabled', () => {
    process.env.RSTUDIO_STARTUP_TIMING = file;
    assert.isTrue(startupTimingEnabled());

    const before = Date.now();
    startupCheckpoint('first');
    startupCheckpoint('second');

    const lines = readFileSync(file, 'utf8').trim().split('\n');
    assert.lengthOf(lines, 2);

    const first = JSON.parse(lines[0]);
    const second = JSON.parse(lines[1]);
    assert.deepEqual(
      [first.tier, first.name, first.pid],
      ['desktop', 'first', process.pid],
    );
    assert.equal(second.name, 'second');
    assert.isAtLeast(first.t, before - 1);
    assert.isAtLeast(second.t, first.t);
  });
});
