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
import { parsePsPids, parseTasklistPids } from '../../../src/main/running-instances';

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
});
