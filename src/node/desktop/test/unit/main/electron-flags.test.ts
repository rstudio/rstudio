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
  ElectronFlagsConfig,
  loadElectronFlags,
  OzonePlatformInputs,
  parseElectronFlags,
  planOzoneRelaunch,
} from '../../../src/main/electron-flags';

function withTempDir(callback: (root: string) => void): void {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-electron-flags-'));
  try {
    callback(root);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

function confWith(contents: string): ElectronFlagsConfig {
  return { path: '/home/user/.config/electron-flags.conf', flags: parseElectronFlags(contents) };
}

function plan(inputs: Partial<OzonePlatformInputs>) {
  return planOzoneRelaunch({
    argv: ['/usr/lib/rstudio/rstudio', 'project.Rproj'],
    currentPlatform: 'x11',
    chromiumArguments: '',
    config: undefined,
    isPackaged: true,
    ...inputs,
  });
}

describe('Electron flags', () => {
  it('parses supported lines and preserves values after the first equals sign', () => {
    const contents = ['# comment', ' --ignored', '--disable-gpu', '--use-gl=angle', '--value=a=b', ''].join('\n');

    assert.deepEqual(parseElectronFlags(contents), [
      { name: 'disable-gpu' },
      { name: 'use-gl', value: 'angle' },
      { name: 'value', value: 'a=b' },
    ]);
  });

  it('drops trailing whitespace from lines', () => {
    assert.deepEqual(parseElectronFlags('--ozone-platform=wayland \t\r\n--disable-gpu  '), [
      { name: 'ozone-platform', value: 'wayland' },
      { name: 'disable-gpu' },
    ]);
  });

  it('returns no config when no electron-flags.conf exists', () => {
    withTempDir((root) => {
      assert.isUndefined(loadElectronFlags([root]));
    });
  });

  it('uses the first config directory containing electron-flags.conf', () => {
    withTempDir((root) => {
      const first = path.join(root, 'first');
      const second = path.join(root, 'second');
      fs.mkdirSync(first);
      fs.mkdirSync(second);
      fs.writeFileSync(path.join(first, 'electron-flags.conf'), '--use-gl=angle\n');
      fs.writeFileSync(path.join(second, 'electron-flags.conf'), '--disable-gpu\n');

      const config = loadElectronFlags([first, second]);
      assert.isDefined(config);
      assert.strictEqual(config.path, path.join(first, 'electron-flags.conf'));
      assert.deepEqual(config.flags, [{ name: 'use-gl', value: 'angle' }]);
    });
  });

  it('names the file when electron-flags.conf cannot be read', () => {
    withTempDir((root) => {
      const configPath = path.join(root, 'electron-flags.conf');
      fs.mkdirSync(configPath);

      assert.throws(() => loadElectronFlags([root]), `Unable to read Electron flags from ${configPath}`);
    });
  });

  describe('Ozone relaunch', () => {
    it('does nothing when no Ozone platform is requested', () => {
      assert.deepEqual(plan({ config: confWith('--use-gl=angle') }), {});
    });

    it('does not relaunch when the requested platform is already in use', () => {
      assert.deepEqual(plan({ config: confWith('--ozone-platform=x11') }), {});
    });

    it('relaunches with the requested switch ahead of the original arguments', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '--use-gl=angle', 'project.Rproj'];
      const result = plan({ argv, config: confWith('--ozone-platform=x11\n--ozone-platform=wayland') });

      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', '--use-gl=angle', 'project.Rproj']);
      assert.include(result.message, '/home/user/.config/electron-flags.conf');
      assert.deepEqual(argv, ['/usr/lib/rstudio/rstudio', '--use-gl=angle', 'project.Rproj']);
    });

    it('relaunches when Chromium reports no platform', () => {
      const result = plan({ currentPlatform: undefined, config: confWith('--ozone-platform=x11') });
      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=x11', 'project.Rproj']);
    });

    it('keeps the switch ahead of a switch terminator', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '--', 'script.R'];
      const result = plan({ argv, config: confWith('--ozone-platform=wayland') });

      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', '--', 'script.R']);
    });

    it('reads the single-dash spelling on the command line, as Chromium does', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '-ozone-platform=x11'];
      const result = plan({ argv, config: confWith('--ozone-platform=wayland') });

      assert.isUndefined(result.relaunchArgs);
      assert.include(result.message, 'the command line sets --ozone-platform');
    });

    it('relaunches past an upper-case command-line switch, which Chromium ignores', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '--OZONE-PLATFORM=x11'];
      const result = plan({ argv, config: confWith('--ozone-platform=wayland') });

      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', '--OZONE-PLATFORM=x11']);
    });

    it('reads every spelling that app.commandLine.appendSwitch accepts', () => {
      for (const chromiumArguments of [
        '-ozone-platform=wayland',
        '--OZONE-PLATFORM=wayland',
        'ozone=no -Ozone-Platform=wayland',
      ]) {
        const result = plan({ chromiumArguments });
        assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', 'project.Rproj'], chromiumArguments);
      }

      const result = plan({ config: confWith('--OZONE-PLATFORM=wayland') });
      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', 'project.Rproj']);
    });

    it('ignores an Ozone-like plain argument in RSTUDIO_CHROMIUM_ARGUMENTS', () => {
      assert.deepEqual(plan({ chromiumArguments: 'ozone-platform=wayland' }), {});
    });

    it('relaunches when an Ozone switch appears only after a switch terminator', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '--', '--ozone-platform=x11'];
      const result = plan({ argv, config: confWith('--ozone-platform=wayland') });

      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=wayland', '--', '--ozone-platform=x11']);
    });

    it('prefers RSTUDIO_CHROMIUM_ARGUMENTS over electron-flags.conf', () => {
      const result = plan({
        currentPlatform: 'wayland',
        chromiumArguments: '--disable-gpu --ozone-platform=x11',
        config: confWith('--ozone-platform=wayland'),
      });

      assert.deepEqual(result.relaunchArgs, ['--ozone-platform=x11', 'project.Rproj']);
      assert.include(result.message, 'RSTUDIO_CHROMIUM_ARGUMENTS');
    });

    it('treats a final bare switch in RSTUDIO_CHROMIUM_ARGUMENTS as the requested value', () => {
      const result = plan({ chromiumArguments: '--ozone-platform=wayland --ozone-platform' });

      assert.isUndefined(result.relaunchArgs);
      assert.include(result.message, 'expected one of x11, wayland');
    });

    it('keeps a platform given on the command line and says why', () => {
      const result = plan({
        argv: ['/usr/lib/rstudio/rstudio', '--ozone-platform=x11'],
        config: confWith('--ozone-platform=wayland'),
      });

      assert.isUndefined(result.relaunchArgs);
      assert.include(result.message, 'the command line sets --ozone-platform');
    });

    it('does not relaunch again after relaunching', () => {
      const argv = ['/usr/lib/rstudio/rstudio', '--ozone-platform=wayland', 'project.Rproj'];
      assert.deepEqual(plan({ argv, currentPlatform: 'wayland', config: confWith('--ozone-platform=wayland') }), {});
    });

    it('ignores an unsupported platform instead of relaunching into it', () => {
      for (const contents of ['--ozone-platform=waylnd', '--ozone-platform', '--ozone-platform=']) {
        const result = plan({ config: confWith(contents) });
        assert.isUndefined(result.relaunchArgs, contents);
        assert.include(result.message, 'expected one of x11, wayland', contents);
      }
    });

    it('does not relaunch a development build', () => {
      const result = plan({ isPackaged: false, config: confWith('--ozone-platform=wayland') });

      assert.isUndefined(result.relaunchArgs);
      assert.include(result.message, 'development build');
    });
  });
});
