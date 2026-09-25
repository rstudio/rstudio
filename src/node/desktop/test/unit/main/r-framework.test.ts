/*
 * r-framework.test.ts
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
import { mkdirSync, mkdtempSync, readFileSync, readlinkSync, rmSync, symlinkSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';

import {
  frameworkVersion,
  isOrthogonal,
  kRFrameworkRoot,
  makeOrthogonal,
  orthogonalEdits,
} from '../../../src/main/r-framework';

describe('RFramework', () => {
  it('recognizes the homes of framework versions', () => {
    const version = frameworkVersion(`${kRFrameworkRoot}/Versions/4.4-arm64/Resources`);
    assert.deepEqual(version, {
      name: '4.4-arm64',
      dir: `${kRFrameworkRoot}/Versions/4.4-arm64`,
      home: `${kRFrameworkRoot}/Versions/4.4-arm64/Resources`,
    });

    // the default version, other installations, and anything unexpected
    assert.isNull(frameworkVersion(`${kRFrameworkRoot}/Resources`));
    assert.isNull(frameworkVersion(`${kRFrameworkRoot}/Versions/Current/Resources`));
    assert.isNull(frameworkVersion(`${kRFrameworkRoot}/Versions/../Resources`));
    assert.isNull(frameworkVersion(`${kRFrameworkRoot}/Versions/4.4'; rm -rf ~/Resources`));
    assert.isNull(frameworkVersion('/opt/R/4.4.1/lib/R'));
  });

  describe('makeOrthogonal', function () {
    let root: string;

    // a framework version laid out as the CRAN installer lays it out
    beforeEach(function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      root = join(mkdtempSync(join(tmpdir(), 'r-framework-')), 'R.framework');
      const dir = join(root, 'Versions', '4.4-arm64');
      const home = join(dir, 'Resources');
      for (const sub of ['bin', 'etc', 'fontconfig/fonts', 'include', 'lib']) {
        mkdirSync(join(home, sub), { recursive: true });
      }
      mkdirSync(join(dir, 'PrivateHeaders'));
      symlinkSync('Resources/include', join(dir, 'Headers'));
      symlinkSync('Resources/lib/libR.dylib', join(dir, 'R'));

      writeFileSync(
        join(home, 'bin', 'R'),
        [
          '#!/bin/sh',
          'R_HOME_DIR=/Library/Frameworks/R.framework/Resources',
          'R_SHARE_DIR=/Library/Frameworks/R.framework/Resources/share',
        ].join('\n'),
      );
      writeFileSync(
        join(home, 'etc', 'Renviron'),
        "R_QPDF=${R_QPDF-'/Library/Frameworks/R.framework/Resources/bin/qpdf'}\n",
      );
      writeFileSync(join(home, 'etc', 'Makeconf'), 'LIBR = -F/Library/Frameworks/R.framework/.. -framework R\n');
      writeFileSync(
        join(home, 'fontconfig', 'fonts', 'fonts.conf'),
        '<dir>/Library/Frameworks/R.framework/Resources/fontconfig</dir>\n',
      );
    });

    afterEach(() => {
      if (root) {
        rmSync(join(root, '..'), { recursive: true, force: true });
      }
    });

    it('points a version at its own directory, as rig does', async () => {
      const version = frameworkVersion(join(root, 'Versions', '4.4-arm64', 'Resources'), root);
      assert.isNotNull(version);

      assert.isFalse(isOrthogonal(version));
      const edits = orthogonalEdits(version);
      assert.lengthOf(edits.files, 4);
      assert.lengthOf(edits.links, 5);

      await makeOrthogonal(version, 'unused: the files are writable');
      assert.isTrue(isOrthogonal(version));

      const read = (file: string) => readFileSync(join(version.home, file), 'utf-8');
      assert.include(read('bin/R'), 'R_HOME_DIR=/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources\n');
      assert.include(read('bin/R'), 'R_SHARE_DIR=/Library/Frameworks/R.framework/Versions/4.4-arm64/Resources/share');
      assert.include(read('etc/Renviron'), 'R.framework/Versions/4.4-arm64/Resources/bin/qpdf');
      assert.include(read('etc/Makeconf'), 'LIBR = -F/Library/Frameworks/R.framework/Versions/4.4-arm64 -framework R');
      assert.include(read('fontconfig/fonts/fonts.conf'), 'R.framework/Versions/4.4-arm64/Resources/fontconfig');

      // a framework to link packages against, inside the version's directory
      assert.equal(readlinkSync(join(version.dir, 'R.framework', 'R')), '../R');
      assert.equal(readlinkSync(join(version.dir, 'R.framework', 'Libraries')), '../Resources/lib');

      // nothing is left to do the second time
      const again = orthogonalEdits(version);
      assert.lengthOf(again.files, 0);
      assert.lengthOf(again.links, 0);
      await makeOrthogonal(version, 'unused');
    });
  });
});
