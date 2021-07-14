/*
 * desktop-options.test.ts
 * 
 * Copyright (C) 2021 by RStudio, PBC
 * 
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */

import { describe } from 'mocha';
import { assert } from 'chai';

import { DesktopOptions, DesktopOptionsImpl, kDesktopOptionDefaults, clearOptionsSingleton } from '../../../src/main/desktop-options';
import { FilePath } from '../../../src/core/file-path';
import { Err, isSuccessful } from '../../../src/core/err';
import { tempDirectory } from '../unit-utils';

const kTestingConfigDirectory = tempDirectory('DesktopOptionsTesting').toString();

function testingDesktopOptions(): DesktopOptionsImpl {
  return DesktopOptions(kTestingConfigDirectory);
}

function deleteTestingDesktopOptions(): Err {
  clearOptionsSingleton();
  const filepath = new FilePath(kTestingConfigDirectory);
  return filepath.removeSync();
}

describe('DesktopOptions', () => {
  afterEach(() => {
    assert(isSuccessful(deleteTestingDesktopOptions()));
  });

  it('use default values when no value has been set before', () => {
    const options = testingDesktopOptions();

    const nonWindowsRBinDir = '';
    const nonWindowsPreferR64 = false;

    assert.equal(options.proportionalFont(), kDesktopOptionDefaults.Font.ProportionalFont);
    assert.equal(options.fixWidthFont(), kDesktopOptionDefaults.Font.FixWidthFont);
    assert.equal(options.useFontConfigDb(), kDesktopOptionDefaults.Font.UseFontConfigDb);
    assert.equal(options.zoomLevel(), kDesktopOptionDefaults.View.ZoomLevel);
    assert.deepEqual(options.windowBounds(), kDesktopOptionDefaults.View.WindowBounds);
    assert.equal(options.accessibility(), kDesktopOptionDefaults.View.Accessibility);
    assert.equal(options.lastRemoteSessionUrl(), kDesktopOptionDefaults.Session.LastRemoteSessionUrl);
    assert.deepEqual(options.authCookies(), kDesktopOptionDefaults.Session.AuthCookies);
    assert.deepEqual(options.tempAuthCookies(), kDesktopOptionDefaults.Session.TempAuthCookies);
    assert.deepEqual(options.ignoredUpdateVersions(), kDesktopOptionDefaults.General.IgnoredUpdateVersions);
    assert.equal(options.clipboardMonitoring(), kDesktopOptionDefaults.General.ClipboardMonitoring);
    if (process.platform === 'win32') {
      assert.equal(options.rBinDir(), kDesktopOptionDefaults.WindowsOnly.RBinDir);
      assert.equal(options.peferR64(), kDesktopOptionDefaults.WindowsOnly.PreferR64);
    } else {
      assert.equal(options.rBinDir(), nonWindowsRBinDir);
      assert.equal(options.peferR64(), nonWindowsPreferR64);
    }
  });
  it('set/get functionality returns correct values', () => {
    const options = testingDesktopOptions();

    const newProportionalFont = 'testProportionalFont';
    const newFixWidthFont = 'testFixWidthFont';
    const newUseFontConfigDb = !kDesktopOptionDefaults.Font.UseFontConfigDb;
    const newZoom = 123;
    const newWindowBounds = {width: 123, height: 321};
    const newAccessibility = !kDesktopOptionDefaults.View.Accessibility;
    const newLastRemoteSessionUrl = 'testLastRemoteSessionUrl';
    const newAuthCookies = ['test', 'Autht', 'Cookies'];
    const newTempAuthCookies = ['test', 'Temp', 'Auth', 'Cookies'];
    const newIgnoredUpdateVersions = ['test', 'Ignored', 'Update', 'Versions'];
    const newClipboardMonitoring = !kDesktopOptionDefaults.General.ClipboardMonitoring;
    const newRBinDir = 'testRBinDir';
    const newPeferR64 = !kDesktopOptionDefaults.WindowsOnly.PreferR64;
    
    const nonWindowsRBinDir = '';
    const nonWindowsPreferR64 = false;

    options.setProportionalFont(newProportionalFont);
    options.setFixWidthFont(newFixWidthFont);
    options.setUseFontConfigDb(newUseFontConfigDb);
    options.setZoomLevel(newZoom);
    options.saveWindowBounds(newWindowBounds);
    options.setAccessibility(newAccessibility);
    options.setLastRemoteSessionUrl(newLastRemoteSessionUrl);
    options.setAuthCookies(newAuthCookies);
    options.setTempAuthCookies(newTempAuthCookies);
    options.setIgnoredUpdateVersions(newIgnoredUpdateVersions);
    options.setClipboardMonitoring(newClipboardMonitoring);
    options.setRBinDir(newRBinDir);
    options.setPeferR64(newPeferR64);

    assert.equal(options.proportionalFont(), newProportionalFont);
    assert.equal(options.fixWidthFont(), newFixWidthFont);
    assert.equal(options.useFontConfigDb(), newUseFontConfigDb);
    assert.equal(options.zoomLevel(), newZoom);
    assert.deepEqual(options.windowBounds(), newWindowBounds);
    assert.equal(options.accessibility(), newAccessibility);
    assert.equal(options.lastRemoteSessionUrl(), newLastRemoteSessionUrl);
    assert.deepEqual(options.authCookies(), newAuthCookies);
    assert.deepEqual(options.tempAuthCookies(), newTempAuthCookies);
    assert.deepEqual(options.ignoredUpdateVersions(), newIgnoredUpdateVersions);
    assert.equal(options.clipboardMonitoring(), newClipboardMonitoring);
    if (process.platform === 'win32') {
      assert.equal(options.rBinDir(), newRBinDir);
      assert.equal(options.peferR64(), newPeferR64);
    } else {
      assert.equal(options.rBinDir(), nonWindowsRBinDir);
      assert.equal(options.peferR64(), nonWindowsPreferR64);
    }
  });
  it('values persist between instances', () => {
    const options1 = testingDesktopOptions();
    const newZoom = 1234;

    assert.equal(options1.zoomLevel(), kDesktopOptionDefaults.View.ZoomLevel);
    options1.setZoomLevel(newZoom);
    assert.equal(options1.zoomLevel(), newZoom);

    clearOptionsSingleton();
    const options2 = testingDesktopOptions();
    assert.equal(options2.zoomLevel(), newZoom);
  });
});