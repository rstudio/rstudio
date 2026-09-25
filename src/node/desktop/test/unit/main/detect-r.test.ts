/*
 * detect-r.test.ts
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
import { mkdtempSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { restore, saveAndClear } from '../unit-utils';
import {
  detectREnvironment,
  frameworkVersionHome,
  parseRQueryResult,
  promptUserForR,
  rDetectionReady,
  rQueryCommand,
  startRDetection,
} from '../../../src/main/detect-r';

describe('DetectR', () => {
  const vars: Record<string, string> = {
    RSTUDIO_WHICH_R: '',
  };

  beforeEach(() => {
    saveAndClear(vars);
  });

  afterEach(() => {
    restore(vars);
  });

  it('Prompt User for R on Non-Windows OS', async () => {
    const platform = 'linux';

    const [path, preflightError] = await promptUserForR(platform);
    assert.equal(path, null);
    assert.equal(preflightError?.message, 'This window can only be opened on Windows');
  });

  it('parses the output of the R query script', () => {
    const fields = ['4.5.1', '/opt/R', '/opt/R/doc', '/opt/R/include', '/opt/R/share', '', '', '/opt/R/lib', 'aarch64-apple-darwin23'];
    const stdout = 'Loading profile...\x1E' + fields.join('\x1F');

    const [environment, error] = parseRQueryResult('/opt/R/bin/R', { stdout, stderr: '', status: 0 });
    assert.isNull(error);
    assert.equal(environment.version, '4.5.1');
    assert.equal(environment.rScriptPath, '/opt/R/bin/R');
    assert.equal(environment.envVars.R_HOME, '/opt/R');
    assert.equal(environment.envVars.R_SHARE_DIR, '/opt/R/share');
    assert.equal(environment.envVars.R_PLATFORM, 'aarch64-apple-darwin23');
    assert.isTrue(environment.ldLibraryPath.endsWith('/opt/R/lib'));
  });

  it('queries a versioned macOS framework install through its own launch steps', () => {
    const home = '/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources';
    const query = rQueryCommand(`${home}/bin/R`);

    // the install's ldpaths sets the library path, then its real executable runs
    assert.equal(query.command, '/bin/sh');
    assert.equal(query.args[0], '-c');
    assert.include(query.args[1], '. "$R_HOME/etc/ldpaths"');
    assert.include(query.args[1], 'exec "$R_HOME/bin/exec/R" "$@"');
    assert.deepEqual(query.args.slice(2), ['R', '--vanilla', '-s']);
    assert.equal(query.env.R_HOME, home);
  });

  it('queries other R installations through their launcher without R_HOME', () => {
    for (const path of ['/Library/Frameworks/R.framework/Resources/bin/R', '/opt/R/4.4.1/bin/R', '/usr/bin/R']) {
      const query = rQueryCommand(path);
      assert.equal(query.command, path);
      assert.deepEqual(query.args, ['--vanilla', '-s']);
      assert.isUndefined(query.env.R_HOME);
    }
    assert.isNull(frameworkVersionHome('/Library/Frameworks/R.framework/Resources/bin/R'));
  });

  it('queries R with the library path RStudio was launched with', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // preparing an R replaces the process's library path with that R's
    // paths, which must not carry over into the query of another R
    const variable = process.platform === 'darwin' ? 'DYLD_FALLBACK_LIBRARY_PATH' : 'LD_LIBRARY_PATH';
    const launched = process.env[variable];
    process.env[variable] = '/opt/R/4.3.3/lib/R/lib';
    try {
      const query = rQueryCommand('/opt/R/4.4.1/bin/R');
      assert.equal(query.env[variable], launched);
    } finally {
      if (launched === undefined) {
        delete process.env[variable];
      } else {
        process.env[variable] = launched;
      }
    }
  });

  it('rejects query output without the marker', () => {
    const [, error] = parseRQueryResult('/opt/R/bin/R', { stdout: 'not R', stderr: '', status: 1 });
    assert.isNotNull(error);

    const [, noOutputError] = parseRQueryResult('/opt/R/bin/R', { stdout: '', stderr: '', status: 127 });
    assert.isNotNull(noOutputError);
  });

  it('the background query fills the cache the synchronous path reads', async function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // a stand-in R: an executable that answers the version query the way R
    // would (\036 is the marker, \037 the field separator)
    const fields = ['4.5.1', '/opt/fake-r', '/opt/fake-r/doc', '/opt/fake-r/include', '/opt/fake-r/share', '', '', '/opt/fake-r/lib', 'aarch64-fake'];
    const dir = mkdtempSync(join(tmpdir(), 'detect-r-test-'));
    const fakeR = join(dir, 'R');
    writeFileSync(fakeR, `#!/bin/sh\nprintf '\\036${fields.join('\\037')}'\n`, { mode: 0o755 });

    try {
      // the production wiring: main.ts starts the detection, and the launch
      // sequence awaits it before the synchronous path reads the cache
      process.env.RSTUDIO_WHICH_R = fakeR;
      startRDetection();
      await rDetectionReady();

      // remove the stand-in: another query would now fail to launch, so a
      // successful synchronous detection proves the cached result is reused
      rmSync(dir, { recursive: true, force: true });

      const [environment, error] = detectREnvironment(fakeR);
      assert.isNull(error);
      assert.equal(environment.version, '4.5.1');
      assert.equal(environment.envVars.R_HOME, '/opt/fake-r');
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });
});
