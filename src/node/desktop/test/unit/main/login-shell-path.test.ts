/*
 * login-shell-path.test.ts
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
import {
  cachedLoginShellPath,
  isShellDerivedPath,
  loginShellPath,
  resetLoginShellPath,
  startLoginShellPathQuery,
  withDefaultToolPaths,
} from '../../../src/main/login-shell-path';

describe('LoginShellPath', () => {
  let dir: string;
  const savedShell = process.env.RSTUDIO_SESSION_SHELL;

  beforeEach(() => {
    dir = mkdtempSync(path.join(tmpdir(), 'login-shell-path-'));
    resetLoginShellPath();
  });

  afterEach(() => {
    resetLoginShellPath();
    rmSync(dir, { recursive: true, force: true });
    if (savedShell === undefined) {
      delete process.env.RSTUDIO_SESSION_SHELL;
    } else {
      process.env.RSTUDIO_SESSION_SHELL = savedShell;
    }
  });

  it('knows nothing before a query has run', async () => {
    assert.isNull(cachedLoginShellPath());
    assert.isNull(await loginShellPath());
  });

  it('waits for the shell on a first launch, then remembers its answer', async function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    this.timeout(15000);

    // /bin/sh -l reads /etc/profile, which is enough to produce a PATH
    process.env.RSTUDIO_SESSION_SHELL = '/bin/sh';

    startLoginShellPathQuery(dir);
    assert.isNull(cachedLoginShellPath(), 'nothing is known on a first launch');

    const fresh = await loginShellPath();
    assert.isNotNull(fresh);
    assert.include(fresh, '/usr/bin');
    assert.equal(cachedLoginShellPath(), fresh);

    const stateFile = path.join(dir, 'startup-state.json');
    assert.isTrue(existsSync(stateFile));
    assert.include(readFileSync(stateFile, 'utf8'), '/usr/bin');

    // a later launch has last time's answer before the shell replies
    resetLoginShellPath();
    startLoginShellPathQuery(dir);
    assert.equal(cachedLoginShellPath(), fresh);
    assert.equal(await loginShellPath(), fresh);
  });

  it('recognizes a shell-derived PATH by /usr/local/bin, as the session does', () => {
    // a terminal launch: the shell's PATH (possibly customized) must be kept
    assert.isTrue(isShellDerivedPath('/usr/local/bin:/usr/bin:/bin'));
    assert.isTrue(isShellDerivedPath('/opt/conda/envs/myenv/bin:/usr/local/bin:/usr/bin'));
    assert.isTrue(isShellDerivedPath('/usr/bin:/usr/local/bin'));
    assert.isTrue(isShellDerivedPath('/usr/bin:/usr/local/bin/:/bin'));

    // a Finder / Dock launch: launchd's minimal PATH
    assert.isFalse(isShellDerivedPath('/usr/bin:/bin:/usr/sbin:/sbin'));
    assert.isFalse(isShellDerivedPath(''));

    // entries merely prefixed with /usr/local/bin do not count
    assert.isFalse(isShellDerivedPath('/usr/local/binaries:/usr/bin'));
  });

  it('builds a fallback PATH with tool locations first, without duplicates', () => {
    const extended = withDefaultToolPaths('/usr/bin:/opt/homebrew/bin:/custom/bin');
    const parts = extended.split(':');

    assert.include(parts, '/usr/local/bin');
    assert.include(parts, '/opt/homebrew/bin');
    assert.include(parts, '/custom/bin');
    assert.equal(new Set(parts).size, parts.length, 'expected no duplicate entries');

    // the path_helper entries take precedence over inherited ones, as they
    // would in a real login shell
    if (process.platform === 'darwin') {
      assert.isBelow(
        parts.indexOf('/usr/local/bin'),
        parts.indexOf('/custom/bin'),
        'expected path_helper entries to precede inherited ones',
      );
    }
  });
});
