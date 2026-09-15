/*
 * electron-flags.test.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the GNU
 * Affero General Public License. This program is distributed WITHOUT ANY
 * EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import fs from 'fs';
import os from 'os';
import path from 'path';

import {
  buildRelaunchArgs,
  getConfiguredOzonePlatform,
  getOzonePlatformFromArgs,
  loadElectronFlags,
  parseElectronFlags,
  shouldRelaunchForOzonePlatform,
} from '../../../src/main/electron-flags';

describe('Electron flags', () => {
  it('parses supported lines and preserves values after the first equals sign', () => {
    const contents = ['# comment', ' --ignored', '--disable-gpu', '--use-gl=angle', '--value=a=b', ''].join('\n');

    assert.deepEqual(parseElectronFlags(contents), [
      { name: 'disable-gpu' },
      { name: 'use-gl', value: 'angle' },
      { name: 'value', value: 'a=b' },
    ]);
  });

  it('returns no config when no electron-flags.conf exists', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-electron-flags-'));

    try {
      assert.isUndefined(loadElectronFlags([root]));
    } finally {
      fs.rmSync(root, { recursive: true, force: true });
    }
  });

  it('uses the first config directory containing electron-flags.conf', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-electron-flags-'));
    const first = path.join(root, 'first');
    const second = path.join(root, 'second');
    fs.mkdirSync(first);
    fs.mkdirSync(second);
    fs.writeFileSync(path.join(first, 'electron-flags.conf'), '--use-gl=angle\n');
    fs.writeFileSync(path.join(second, 'electron-flags.conf'), '--disable-gpu\n');

    try {
      const config = loadElectronFlags([first, second]);
      assert.isDefined(config);
      assert.strictEqual(config.path, path.join(first, 'electron-flags.conf'));
      assert.deepEqual(config.flags, [{ name: 'use-gl', value: 'angle' }]);
    } finally {
      fs.rmSync(root, { recursive: true, force: true });
    }
  });

  describe('Ozone platform', () => {
    it('ignores unrelated flags and uses the last duplicate Ozone entry', () => {
      assert.isUndefined(getConfiguredOzonePlatform(parseElectronFlags('--use-gl=angle')));
      assert.strictEqual(
        getConfiguredOzonePlatform(parseElectronFlags('--ozone-platform=x11\n--ozone-platform=wayland')),
        'wayland',
      );
    });

    it('distinguishes matching, differing, and absent startup values', () => {
      assert.isFalse(shouldRelaunchForOzonePlatform(undefined, undefined));
      assert.isFalse(shouldRelaunchForOzonePlatform('wayland', 'wayland'));
      assert.isTrue(shouldRelaunchForOzonePlatform('x11', 'wayland'));
      assert.isTrue(shouldRelaunchForOzonePlatform(undefined, 'wayland'));
      assert.strictEqual(getOzonePlatformFromArgs(['rstudio', '--ozone-platform=wayland']), 'wayland');
    });
  });

  describe('relaunch arguments', () => {
    it('adds the configured switch to packaged argv and preserves user arguments', () => {
      const processArgs = ['/usr/bin/rstudio', '--use-gl=angle', 'project.Rproj', 'script.R'];

      assert.deepEqual(buildRelaunchArgs(processArgs, 'wayland', true), [
        '--ozone-platform=wayland',
        '--use-gl=angle',
        'project.Rproj',
        'script.R',
      ]);
    });

    it('adds the configured switch after the app path in development argv', () => {
      const processArgs = ['/usr/bin/electron', '/workspace/rstudio', '.', '--use-gl=angle', 'script.R'];

      assert.deepEqual(buildRelaunchArgs(processArgs, 'wayland', false), [
        '/workspace/rstudio',
        '--ozone-platform=wayland',
        '.',
        '--use-gl=angle',
        'script.R',
      ]);
    });

    it('replaces duplicate Ozone switches with one configured value', () => {
      const processArgs = [
        '/usr/bin/rstudio',
        '--ozone-platform=x11',
        '--use-gl=angle',
        '--ozone-platform=wayland',
        'project.Rproj',
        '--ozone-platform=x11',
        'script.R',
      ];

      assert.deepEqual(buildRelaunchArgs(processArgs, 'wayland', true), [
        '--ozone-platform=wayland',
        '--use-gl=angle',
        'project.Rproj',
        'script.R',
      ]);
      assert.deepEqual(processArgs, [
        '/usr/bin/rstudio',
        '--ozone-platform=x11',
        '--use-gl=angle',
        '--ozone-platform=wayland',
        'project.Rproj',
        '--ozone-platform=x11',
        'script.R',
      ]);
    });
  });
});
