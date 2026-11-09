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
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { dirname, join } from 'path';
import { restore, saveAndClear } from '../unit-utils';
import i18next from 'i18next';
import { initI18n } from '../../../src/main/i18n-manager';
import {
  detectREnvironment,
  detectREnvironmentAsync,
  frameworkVersionHome,
  parseRQueryResult,
  promptUserForR,
  rDetectionReady,
  rFromRStudioWhichR,
  RLaunchError,
  rLaunchFailureMessage,
  rNotFoundDialog,
  rQueryCommand,
  resolveRHome,
  scanForRPosix,
  startRDetection,
  windowsSessionPath,
} from '../../../src/main/detect-r';

describe('DetectR', () => {
  const vars: Record<string, string> = {
    RSTUDIO_WHICH_R: '',
  };

  // the launch failure messages are localized, so the strings must be loaded
  before(async () => {
    initI18n();
    await new Promise<void>((resolve) => {
      if (i18next.isInitialized) {
        resolve();
      } else {
        i18next.on('initialized', () => resolve());
      }
    });
  });

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
    const fields = [
      '4.5.1',
      '/opt/R',
      '/opt/R/doc',
      '/opt/R/include',
      '/opt/R/share',
      '',
      '',
      '/opt/R/lib',
      'aarch64-apple-darwin23',
    ];
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

  it('puts an R on the PATH RStudio was launched with, not behind an R prepared earlier', () => {
    const launched = process.env.PATH;
    process.env.PATH = `C:\\R\\R-4.4.1\\bin\\x64;${launched}`;
    try {
      const expected = launched ? `C:\\R\\R-4.5.0\\bin\\x64;${launched}` : 'C:\\R\\R-4.5.0\\bin\\x64';
      assert.equal(windowsSessionPath('C:\\R\\R-4.5.0\\bin\\x64'), expected);
    } finally {
      process.env.PATH = launched;
    }
  });

  it('rejects query output without the marker', () => {
    const [, error] = parseRQueryResult('/opt/R/bin/R', { stdout: 'not R', stderr: '', status: 1 });
    assert.isNotNull(error);

    const [, noOutputError] = parseRQueryResult('/opt/R/bin/R', { stdout: '', stderr: '', status: 127 });
    assert.isNotNull(noOutputError);
  });

  it('reports a failed query as R failing to start, with what R said', () => {
    // the generic "unknown error" told users nothing, and the caller then
    // claimed no R could be located even though this one was found
    const stderr = "Fatal error: cannot create 'R_TempDir'\n";
    const [, error] = parseRQueryResult('C:\\R\\R-4.6.2\\bin\\x64\\Rterm.exe', { stdout: '', stderr, status: 2 });
    assert.instanceOf(error, RLaunchError);
    assert.include(error.message, 'C:\\R\\R-4.6.2\\bin\\x64\\Rterm.exe');
    // the whole line, since the path above also contains a "2"
    assert.include(error.message, 'Exit status: 2');
    assert.include(error.message, "Fatal error: cannot create 'R_TempDir'");
    assert.notInclude(error.message, 'unknown error');

    // output without our marker is the same failure: R ran, but not our script
    const [, markerError] = parseRQueryResult('/opt/R/bin/R', { stdout: 'not R', stderr: 'oops', status: 1 });
    assert.instanceOf(markerError, RLaunchError);
    assert.include(markerError.message, 'oops');
  });

  it('keeps only the tail of a long stderr in the launch failure message', () => {
    const lines = Array.from({ length: 40 }, (_, i) => `line ${i + 1}`);
    const message = rLaunchFailureMessage('/opt/R/bin/R', { stdout: '', stderr: lines.join('\n'), status: 1 });
    assert.notInclude(message, 'line 1\n');
    assert.notInclude(message, 'line 29');
    assert.include(message, 'line 31');
    assert.include(message, 'line 40');
  });

  it('cuts long stderr lines in the launch failure message', () => {
    const stderr = `Error: ${'x'.repeat(300)}\nshort`;
    const message = rLaunchFailureMessage('/opt/R/bin/R', { stdout: '', stderr, status: 1 });
    assert.include(message, `Error: ${'x'.repeat(193)}...`);
    assert.notInclude(message, 'x'.repeat(194));
    assert.include(message, 'short');
  });

  it('names the signal that killed R', () => {
    // a crash at startup, or a kill by security software, leaves no status
    // and usually no output; the signal is all there is to report
    const [, error] = parseRQueryResult('/opt/R/bin/R', { stdout: '', stderr: '', status: null, signal: 'SIGKILL' });
    assert.instanceOf(error, RLaunchError);
    assert.include(error.message, 'SIGKILL');
    assert.notInclude(error.message, 'Exit status');
  });

  it('treats an answer R died partway through as a launch failure', () => {
    // the marker was printed but the query script did not finish; this
    // used to throw while unpacking the fields, and the dialog showed the
    // TypeError
    const stdout = '\x1E4.6.2\x1F/opt/R/lib/R';
    const [, error] = parseRQueryResult('/opt/R/bin/R', { stdout, stderr: '', status: null, signal: 'SIGSEGV' });
    assert.instanceOf(error, RLaunchError);
    assert.include(error.message, '/opt/R/bin/R');
    assert.include(error.message, 'SIGSEGV');
  });

  it('reports a path with no executable as R not found, not as R failing to start', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // RSTUDIO_WHICH_R with a typo, or a recorded R that was uninstalled
    const dir = mkdtempSync(join(tmpdir(), 'detect-r-missing-'));
    try {
      const missing = join(dir, 'R');
      const [, error] = detectREnvironment(missing);
      assert.instanceOf(error, RLaunchError);
      assert.isTrue(error.executableMissing);
      assert.include(error.message, `No R executable was found at ${missing}`);
      assert.include(error.message, 'ENOENT');
      assert.notInclude(error.message, 'failed to start');
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it('picks the dialog title by what went wrong', () => {
    // nothing found at all, or an error that is not about a particular R
    assert.deepEqual(rNotFoundDialog(), {
      title: 'R not found',
      message: 'Could not locate an R installation on the system.',
    });
    assert.deepEqual(rNotFoundDialog(new TypeError('Cannot read properties of undefined')), rNotFoundDialog());

    // an R that was found but did not start
    const broken = new RLaunchError('/opt/R/bin/R', { stdout: '', stderr: 'Fatal error', status: 2 });
    assert.deepEqual(rNotFoundDialog(broken), { title: 'R failed to start', message: broken.message });

    // a path with no R at it
    const enoent = Object.assign(new Error('spawnSync /opt/R/typo/bin/R ENOENT'), { code: 'ENOENT' });
    const missing = new RLaunchError('/opt/R/typo/bin/R', { stdout: null, stderr: null, status: null, error: enoent });
    assert.deepEqual(rNotFoundDialog(missing), { title: 'R not found', message: missing.message });
    assert.include(missing.message, '/opt/R/typo/bin/R');
  });

  it('reports the signal of an R killed at startup', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    const dir = mkdtempSync(join(tmpdir(), 'detect-r-killed-'));
    const fakeR = join(dir, 'R');
    writeFileSync(fakeR, '#!/bin/sh\nkill -KILL $$\n', { mode: 0o755 });

    try {
      const [, error] = detectREnvironment(fakeR);
      assert.instanceOf(error, RLaunchError);
      assert.include(error.message, fakeR);
      assert.include(error.message, 'Terminated by signal: SIGKILL');
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it('surfaces the exit status and stderr of an R that fails to start', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // a stand-in R that dies the way R does when its temp dir is unusable
    const dir = mkdtempSync(join(tmpdir(), 'detect-r-fatal-'));
    const fakeR = join(dir, 'R');
    writeFileSync(fakeR, '#!/bin/sh\necho "Fatal error: cannot create \'R_TempDir\'" >&2\nexit 2\n', { mode: 0o755 });

    try {
      const [, error] = detectREnvironment(fakeR);
      assert.instanceOf(error, RLaunchError);
      assert.include(error.message, fakeR);
      assert.include(error.message, 'Exit status: 2');
      assert.include(error.message, "Fatal error: cannot create 'R_TempDir'");
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it('reports a found-but-broken R from discovery instead of "no R found"', function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // discovery used to drop the launch error on the floor, so the dialog
    // claimed no R could be located even though one was installed
    const dir = mkdtempSync(join(tmpdir(), 'detect-r-scan-'));
    const brokenR = join(dir, 'R');
    writeFileSync(brokenR, '#!/bin/sh\necho "Fatal error: cannot create \'R_TempDir\'" >&2\nexit 2\n', { mode: 0o755 });
    const missingR = join(dir, 'missing', 'R');

    try {
      // a missing candidate is skipped; a broken one is reported
      const [, error] = scanForRPosix([missingR, brokenR], 'darwin');
      assert.instanceOf(error, RLaunchError);
      assert.include(error.message, brokenR);
      assert.include(error.message, 'Exit status: 2');

      // with no candidate present at all, the error is the generic one
      const [, notFound] = scanForRPosix([missingR], 'darwin');
      assert.isNotNull(notFound);
      assert.notInstanceOf(notFound, RLaunchError);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it('resolves an R home directory in RSTUDIO_WHICH_R to its executable', () => {
    const home = mkdtempSync(join(tmpdir(), 'detect-r-home-'));
    const executable = join(home, 'bin', process.platform === 'win32' ? 'x64/Rterm.exe' : 'R');
    mkdirSync(dirname(executable), { recursive: true });
    writeFileSync(executable, '');
    const empty = mkdtempSync(join(tmpdir(), 'detect-r-empty-'));

    try {
      process.env.RSTUDIO_WHICH_R = home;
      assert.equal(rFromRStudioWhichR(), executable);

      // an executable is used as given, and so are a directory holding no R
      // and a path that doesn't exist, so that validation reports them
      assert.equal(resolveRHome(executable), executable);
      assert.equal(resolveRHome(empty), empty);
      assert.equal(resolveRHome(join(home, 'missing')), join(home, 'missing'));

      // on Windows, the 32-bit executable is the fallback
      const i386 = join(home, 'bin', 'i386', 'Rterm.exe');
      mkdirSync(dirname(i386), { recursive: true });
      writeFileSync(i386, '');
      assert.equal(resolveRHome(home, 'win32'), process.platform === 'win32' ? executable : i386);
    } finally {
      rmSync(home, { recursive: true, force: true });
      rmSync(empty, { recursive: true, force: true });
    }
  });

  it('reports an R that answers but never exits as a timeout, and remembers nothing of it', async function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // a stand-in R that prints a complete answer and then hangs
    const fields = [
      '4.5.1',
      '/opt/fake-r',
      '/opt/fake-r/doc',
      '/opt/fake-r/include',
      '/opt/fake-r/share',
      '',
      '',
      '/opt/fake-r/lib',
      'aarch64-fake',
    ];
    const dir = mkdtempSync(join(tmpdir(), 'detect-r-hang-'));
    const fakeR = join(dir, 'R');
    writeFileSync(fakeR, `#!/bin/sh\nprintf '\\036${fields.join('\\037')}'\nsleep 5\n`, { mode: 0o755 });

    try {
      const [, error] = await detectREnvironmentAsync(fakeR, 250);
      assert.isNotNull(error);
      assert.include(error.message, 'did not answer');

      // nothing was cached: a query now has to launch the stand-in again,
      // which cannot happen once it is gone
      rmSync(dir, { recursive: true, force: true });
      const [, again] = detectREnvironment(fakeR);
      assert.isNotNull(again);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it('the background query fills the cache the synchronous path reads', async function () {
    if (process.platform === 'win32') {
      this.skip();
    }

    // a stand-in R: an executable that answers the version query the way R
    // would (\036 is the marker, \037 the field separator)
    const fields = [
      '4.5.1',
      '/opt/fake-r',
      '/opt/fake-r/doc',
      '/opt/fake-r/include',
      '/opt/fake-r/share',
      '',
      '',
      '/opt/fake-r/lib',
      'aarch64-fake',
    ];
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
