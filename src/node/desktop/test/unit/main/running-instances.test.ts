/*
 * running-instances.test.ts
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
import {
  execFileRunner,
  otherInstanceCount,
  parsePsPids,
  parseTasklistPids,
} from '../../../src/main/running-instances';

describe('running instances', () => {
  describe('parseTasklistPids', () => {
    it('reads one pid per CSV row', () => {
      const output =
        '"rstudio.exe","1234","Console","1","123,456 K"\r\n"rstudio.exe","5678","Console","1","98,765 K"\r\n';
      assert.deepEqual(parseTasklistPids(output), [1234, 5678]);
    });

    it('is empty when tasklist matched nothing', () => {
      assert.deepEqual(parseTasklistPids('INFO: No tasks are running which match the specified criteria.\r\n'), []);
      assert.deepEqual(parseTasklistPids(''), []);
    });
  });

  describe('parsePsPids', () => {
    const app = '/Applications/RStudio.app/Contents/MacOS/RStudio';
    const output = [
      '    1 /sbin/launchd',
      `  812 ${app}`,
      '  840 /Applications/RStudio.app/Contents/Frameworks/RStudio Helper (GPU).app/Contents/MacOS/RStudio Helper (GPU)',
      '  901 /Applications/RStudio.app/Contents/Resources/app/bin/rsession',
      `  950 ${app}`,
      ' 1010 /tmp/RStudio.app/Contents/MacOS/RStudio',
      '',
    ].join('\n');

    it('matches only the main executable of this installation', () => {
      assert.deepEqual(parsePsPids(output, app), [812, 950]);
      assert.deepEqual(parsePsPids(output, '/tmp/RStudio.app/Contents/MacOS/RStudio'), [1010]);
    });

    it('is empty when nothing matches', () => {
      assert.deepEqual(parsePsPids(output, '/opt/RStudio'), []);
      assert.deepEqual(parsePsPids('', app), []);
    });
  });

  describe('otherInstanceCount', () => {
    const me = process.pid;
    const other = me + 3; // a pid that can never be one of the simulated own processes
    const exe = 'C:\\RStudio\\rstudio.exe';
    const windowsOutput = [me, me + 1, me + 2, other]
      .map((pid) => `"rstudio.exe","${pid}","Console","1","1,000 K"`)
      .join('\r\n');
    const app = '/Applications/RStudio.app/Contents/MacOS/RStudio';
    const helper = '/Applications/RStudio.app/Contents/Frameworks/RStudio Helper.app/Contents/MacOS/RStudio Helper';
    const macOutput = `${me} ${app}\n${me + 1} ${helper}\n${other} ${app}\n`;

    it("excludes this application's own processes and counts the rest", async () => {
      const own = [me + 1, me + 2];
      assert.equal(await otherInstanceCount(exe, own, 'win32', async () => windowsOutput), 1);
      assert.equal(await otherInstanceCount(app, own, 'darwin', async () => macOutput), 1);
    });

    it('is 0 when only this application is running', async () => {
      const alone = windowsOutput.split('\r\n').slice(0, 3).join('\r\n');
      assert.equal(await otherInstanceCount(exe, [me + 1, me + 2], 'win32', async () => alone), 0);
      assert.equal(await otherInstanceCount(app, [], 'darwin', async () => `${me} ${app}\n`), 0);
    });

    it('is null, not 0, when the process listing fails', async () => {
      assert.isNull(await otherInstanceCount(exe, [], 'win32', async () => null));
      assert.isNull(await otherInstanceCount(app, [], 'darwin', async () => null));
    });

    it('is 0 on platforms without an installer to protect, without running anything', async () => {
      const never = async () => {
        throw new Error('should not run');
      };
      assert.equal(await otherInstanceCount('/usr/lib/rstudio/rstudio', [], 'linux', never), 0);
    });
  });

  describe('execFileRunner', () => {
    it('answers null, rather than waiting, when the listing command stalls', async function () {
      if (process.platform === 'win32') {
        this.skip(); // no sleep(1) to stall with
      }
      assert.isNull(await execFileRunner(100)('/bin/sleep', ['30']));
    });

    it('answers the output of a command that finishes', async function () {
      if (process.platform === 'win32') {
        this.skip();
      }
      assert.equal(await execFileRunner()('/bin/echo', ['listed']), 'listed\n');
    });

    it('adds the given variables to the environment it inherits', async function () {
      if (process.platform === 'win32') {
        this.skip();
      }
      const output = await execFileRunner()('/bin/sh', ['-c', 'printf "%s|%s" "$RS_RUNNER_TEST" "$PATH"'], {
        RS_RUNNER_TEST: "it's ’quoted’",
      });
      assert.equal(output, `it's ’quoted’|${process.env.PATH ?? ''}`);
    });
  });
});
