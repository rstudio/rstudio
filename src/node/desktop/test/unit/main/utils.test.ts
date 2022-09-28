/*
 * utils.test.ts
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
import { saveAndClear, restore } from '../unit-utils';
import { app } from 'electron';

import { FilePath } from '../../../src/core/file-path';
import { getenv, setenv, unsetenv } from '../../../src/core/environment';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { NullLogger, setLogger } from '../../../src/core/logger';

import * as Utils from '../../../src/main/utils';
import { userHomePath } from '../../../src/core/user';
import { FileFilter } from 'electron/main';

describe('Utils', () => {
  const envVars: Record<string, string> = {
    RSTUDIO_CPP_BUILD_OUTPUT: '',
  };

  beforeEach(() => {
    saveAndClear(envVars);
    clearCoreSingleton();
    const f = new NullLogger();
    setLogger(f);
  });

  afterEach(() => {
    restore(envVars);
  });

  it('userLogPath returns a non-empty string', () => {
    assert.isNotEmpty(Utils.userLogPath().getAbsolutePath());
  });
  it('usereWebCachePath returns a non-empty string', () => {
    assert.isNotEmpty(Utils.userWebCachePath().getAbsolutePath());
  });
  it('devicePixelRatio returns 1.0', () => {
    assert.strictEqual(Utils.devicePixelRatio(), 1.0);
  });
  it('randomString generates a random string', () => {
    const str1 = Utils.randomString();
    const str2 = Utils.randomString();
    const str3 = Utils.randomString();
    assert.notEqual(str1, str2);
    assert.notEqual(str1, str3);
    assert.notEqual(str2, str3);
  });
  it('getComponentVersions returns expected JSON', () => {
    const result = Utils.getComponentVersions();
    const json: Utils.VersionInfo = JSON.parse(result);
    assert.isNotEmpty(json.electron);
    assert.isNotEmpty(json.rstudio);
    assert.isNotEmpty(json.node);
    assert.isNotEmpty(json.v8);
  });
  it('augmentCommandLineArguments adds contents of env var', () => {
    assert.isFalse(app.commandLine.hasSwitch('disable-gpu'));
    assert.isEmpty(getenv('RSTUDIO_CHROMIUM_ARGUMENTS'));
    setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu --use-gl=desktop');
    Utils.augmentCommandLineArguments();
    assert.isTrue(app.commandLine.hasSwitch('disable-gpu'));
    assert.isTrue(app.commandLine.getSwitchValue('use-gl') == 'desktop');
    app.commandLine.removeSwitch('disable-gpu');
    app.commandLine.removeSwitch('use-gl');
    unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
  });
  it('initializeSharedSecret generates a random string in RS_SHARED_SECRET envvar_', () => {
    const envvar = 'RS_SHARED_SECRET';
    assert.equal(getenv(envvar).length, 0);
    Utils.initializeSharedSecret();
    assert.isAtLeast(getenv(envvar).length, 0);
  });
  it('rsessionExeName returns non-empty string', () => {
    assert.isNotEmpty(Utils.rsessionExeName());
  });
  it('removeStaleOptionsLockfile is callable', () => {
    Utils.removeStaleOptionsLockfile();
    assert(true);
  });
  it('findComponents returns session and scripts paths', () => {
    process.env.RSTUDIO_CPP_BUILD_OUTPUT = '/somewhere/interesting/';
    const [, session, scripts] = Utils.findComponents();
    assert.isNotEmpty(session.getAbsolutePath());
    assert.isNotEmpty(scripts.getAbsolutePath());
  });
  it('getCurrentlyUniqueFolderName returns a unique foldername with prefix', () => {
    const folder1 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    assert.isFalse(folder1.existsSync()); // shouldn't create folder
    assert.isTrue(folder1.getAbsolutePath().indexOf('my-prefix-') >= 0); // contains prefix
  });
  it('getCurrentlyUniqueFolderName returns a unique foldername for each call', () => {
    const folder1 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    const folder2 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    assert.notEqual(folder1, folder2);
  });
  it('resolveAliasedPathSync replaces tilde with home', () => {
    const start = '~/foo/bar';
    const result = FilePath.resolveAliasedPathSync(start, userHomePath());
    const resultStr = result.getAbsolutePath();
    assert.isAtLeast(resultStr.length, start.length);
    assert.notEqual(resultStr.charAt(0), '~');
    assert.isAbove(resultStr.lastIndexOf('/foo/bar'), -1);
  });
  it('filterFromQFileDialogFilter converts file filter with one extension', () => {
    const input = 'R Projects (*.Rproj)';
    const expected: FileFilter[] = [{ name: 'R Projects', extensions: ['Rproj'] }];
    const result = Utils.filterFromQFileDialogFilter(input);
    assert.deepEqual(expected, result);
  });
  it('filterFromQFileDialogFilter converts file filter with multiple extensions', () => {
    const input = 'Theme Files (*.tmTheme *.rstheme)';
    const expected: FileFilter[] = [{ name: 'Theme Files', extensions: ['tmTheme', 'rstheme'] }];
    const result = Utils.filterFromQFileDialogFilter(input);
    assert.deepEqual(expected, result);
  });
  it('filterFromQFileDialogFilter converts file filter with file types', () => {
    const input = 'Images (*.png *.xpm *.jpg);;Text files (*.txt);;XML files (*.xml)';
    const expected: FileFilter[] = [
      { name: 'Images', extensions: ['png', 'xpm', 'jpg'] },
      { name: 'Text files', extensions: ['txt'] },
      { name: 'XML files', extensions: ['xml'] },
    ];
    const result = Utils.filterFromQFileDialogFilter(input);
    assert.deepEqual(expected, result);
  });
});
