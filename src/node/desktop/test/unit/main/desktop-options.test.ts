/*
 * desktop-options.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */

import { assert } from 'chai';
import { BrowserWindow, Rectangle, screen } from 'electron';
import { Display } from 'electron/main';
import { describe } from 'mocha';
import sinon from 'sinon';
import { Err, isSuccessful } from '../../../src/core/err';
import { FilePath } from '../../../src/core/file-path';
import DesktopOptions from '../../../src/main/preferences/desktop-options';
import {
  clearOptionsSingleton,
  DesktopOptionsImpl,
  ElectronDesktopOptions,
  firstIsInsideSecond,
  kDesktopOptionDefaults,
} from '../../../src/main/preferences/electron-desktop-options';
import { createSinonStubInstanceForSandbox, tempDirectory } from '../unit-utils';

const kTestingConfigDirectory = tempDirectory('DesktopOptionsTesting').toString();

function testingDesktopOptions(): DesktopOptionsImpl {
  const legacyOptions = new (class extends DesktopOptions {
    fixedWidthFont(): string | undefined {
      return kDesktopOptionDefaults.Font.FixedWidthFont;
    }
  })();
  return ElectronDesktopOptions(kTestingConfigDirectory, legacyOptions);
}

function deleteTestingDesktopOptions(): Err {
  clearOptionsSingleton();
  const filepath = new FilePath(kTestingConfigDirectory);
  return filepath.removeIfExistsSync();
}

function rec(height = 10, width = 10, x = 0, y = 0): Rectangle {
  return { height: height, width: width, x: x, y: y };
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
    assert.equal(options.fixedWidthFont(), kDesktopOptionDefaults.Font.FixedWidthFont);
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
      assert.equal(options.rBinDir(), kDesktopOptionDefaults.Platform.Windows.RBinDir);
      assert.equal(options.peferR64(), kDesktopOptionDefaults.Platform.Windows.PreferR64);
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
    const newWindowBounds = { width: 123, height: 321, x: 0, y: 0 };
    const newAccessibility = !kDesktopOptionDefaults.View.Accessibility;
    const newLastRemoteSessionUrl = 'testLastRemoteSessionUrl';
    const newAuthCookies = ['test', 'Autht', 'Cookies'];
    const newTempAuthCookies = ['test', 'Temp', 'Auth', 'Cookies'];
    const newIgnoredUpdateVersions = ['test', 'Ignored', 'Update', 'Versions'];
    const newClipboardMonitoring = !kDesktopOptionDefaults.General.ClipboardMonitoring;
    const newRBinDir = 'testRBinDir';
    const newPeferR64 = !kDesktopOptionDefaults.Platform.Windows.PreferR64;

    const nonWindowsRBinDir = '';
    const nonWindowsPreferR64 = false;

    options.setProportionalFont(newProportionalFont);
    options.setFixedWidthFont(newFixWidthFont);
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
    assert.equal(options.fixedWidthFont(), newFixWidthFont);
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
  it('restores window bounds to correct display', () => {
    const displays = [
      { workArea: { width: 2000, height: 2000, x: 0, y: 0 } },
      { workArea: { width: 2000, height: 2000, x: 2000, y: 0 } },
    ];
    const savedWinBounds = { width: 500, height: 500, x: 2100, y: 100 };

    // Save bounds onto a secondary display on the right
    ElectronDesktopOptions().saveWindowBounds(savedWinBounds);

    const sandbox = sinon.createSandbox();
    sandbox.stub(screen, 'getAllDisplays').returns(displays as Display[]);
    const testMainWindow = createSinonStubInstanceForSandbox(sandbox, BrowserWindow);
    testMainWindow.setBounds.withArgs(savedWinBounds);
    testMainWindow.getSize.returns([savedWinBounds.width, savedWinBounds.height]);

    ElectronDesktopOptions().restoreMainWindowBounds(testMainWindow);

    sandbox.assert.calledOnceWithExactly(testMainWindow.setBounds, savedWinBounds);
    sandbox.assert.calledOnce(testMainWindow.setSize);
    sandbox.assert.alwaysCalledWith(testMainWindow.setSize, savedWinBounds.width, savedWinBounds.height);
    sandbox.assert.callCount(testMainWindow.setPosition, 0);
    sandbox.restore();
  });
  it('restores window bounds to default when saved display no longer present', () => {
    const defaultDisplay = { bounds: { width: 2000, height: 2000, x: 0, y: 0 } };
    const savedWinBounds = { width: 500, height: 500, x: 0, y: 0 };
    const defaultWinWidth = kDesktopOptionDefaults.View.WindowBounds.width;
    const defaultWinHeight = kDesktopOptionDefaults.View.WindowBounds.height;

    const sandbox = sinon.createSandbox();
    sandbox.stub(screen, 'getAllDisplays').returns([]);
    sandbox.stub(screen, 'getPrimaryDisplay').returns(defaultDisplay as Display);
    const testMainWindow = createSinonStubInstanceForSandbox(sandbox, BrowserWindow);
    testMainWindow.setSize.withArgs(defaultWinWidth, defaultWinHeight);
    testMainWindow.getSize.returns([defaultWinWidth, defaultWinHeight]);

    // Make sure some bounds are already saved
    ElectronDesktopOptions().saveWindowBounds(savedWinBounds);

    ElectronDesktopOptions().restoreMainWindowBounds(testMainWindow);

    sandbox.assert.calledTwice(testMainWindow.setSize);
    sandbox.assert.alwaysCalledWith(testMainWindow.setSize, defaultWinWidth, defaultWinHeight);
    sandbox.restore();
  });
});

/**
 * A note on Electron's rectangle/display coordinate system:
 * (x, y) coord is top left corner of a Rectangle or Display object
 * (x + width, y + height) is bottom right corner
 *
 * x increases to the right, decreases to the left
 * y increases downwards, decreases upwards
 *
 * primary display's (x, y) coord is always (0, 0)
 * negative values are legal
 * external display to the right of primary display could be (primary.width, 0) ex. (1920, 0)
 * external display to the left of primary display could be (-secondary.width, 0) ex. (-1200, 0)
 */
describe('FirstIsInsideSecond', () => {
  const INNER_WIDTH = 10;
  const INNER_HEIGHT = 10;
  const INNER_X = 0;
  const INNER_Y = 0;

  const OUTER_WIDTH = 20;
  const OUTER_HEIGHT = 20;
  const OUTER_X = 0;
  const OUTER_Y = 0;

  const X_FAR_OUT_WEST = -100;
  const X_FAR_BACK_EAST = 100;
  const Y_FAR_UP_NORTH = -100;
  const Y_FAR_DOWN_SOUTH = 100;

  it('basic case', () => {
    assert.isTrue(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, INNER_X + 1, INNER_Y + 1),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    ); // entirely inside

    // top and left boarders shared
    assert.isTrue(firstIsInsideSecond(rec(), rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y)));

    // same size rectangles is valid
    assert.isTrue(firstIsInsideSecond(rec(), rec()));
  });
  it('backwards case', () => {
    assert.isFalse(firstIsInsideSecond(rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y), rec()));
  });
  it('partially outside', () => {
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, INNER_X + 11, INNER_Y),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, INNER_X, INNER_Y + 11),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, INNER_X - 1, INNER_Y),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, INNER_X, INNER_Y - 1),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
  });
  it('entirely outside', () => {
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, X_FAR_BACK_EAST, Y_FAR_DOWN_SOUTH),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, X_FAR_OUT_WEST, Y_FAR_DOWN_SOUTH),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, X_FAR_BACK_EAST, Y_FAR_UP_NORTH),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
    assert.isFalse(
      firstIsInsideSecond(
        rec(INNER_WIDTH, INNER_HEIGHT, X_FAR_OUT_WEST, Y_FAR_UP_NORTH),
        rec(OUTER_WIDTH, OUTER_HEIGHT, OUTER_X, OUTER_Y),
      ),
    );
  });
});

describe('Font tests', () => {
  afterEach(() => {
    assert(isSuccessful(deleteTestingDesktopOptions()));
  });

  it('can get the legacy font', () => {
    const mockLegacyOptions = new (class extends DesktopOptions {
      fixedWidthFont(): string | undefined {
        return 'legacy font';
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    assert.strictEqual(testDesktopOptions.fixedWidthFont(), 'legacy font');
  });

  it('set font overrides legacy font option', () => {
    const mockLegacyOptions = new (class extends DesktopOptions {
      fixedWidthFont(): string | undefined {
        return 'legacy font';
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    testDesktopOptions.setFixedWidthFont('new font');
    assert.strictEqual(testDesktopOptions.fixedWidthFont(), 'new font');
  });
});
