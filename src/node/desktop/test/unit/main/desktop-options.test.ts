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

import { assert, expect } from 'chai';
import { BrowserWindow, Rectangle, screen } from 'electron';
import { Display } from 'electron/main';
import { describe } from 'mocha';
import sinon from 'sinon';
import { properties } from '../../../../../cpp/session/resources/schema/user-state-schema.json';
import { Err, isSuccessful } from '../../../src/core/err';
import { FilePath } from '../../../src/core/file-path';
import DesktopOptions from '../../../src/main/preferences/desktop-options';
import {
  clearOptionsSingleton,
  DesktopOptionsImpl,
  ElectronDesktopOptions,
  firstIsInsideSecond,
} from '../../../src/main/preferences/electron-desktop-options';
import { createSinonStubInstanceForSandbox, tempDirectory } from '../unit-utils';

const kTestingConfigDirectory = tempDirectory('DesktopOptionsTesting').toString();

class DesktopOptionsStub extends DesktopOptions {
  fixedWidthFont(): string | undefined {
    return properties.font.default.fixedWidthFont;
  }
  zoomLevel(): number | undefined {
    return properties.view.default.zoomLevel;
  }
  rBinDir(): string | undefined {
    return properties.platform.default.windows.rBinDir;
  }
}

function testingDesktopOptions(): DesktopOptionsImpl {
  const legacyOptions = new DesktopOptionsStub();
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

    assert.equal(options.proportionalFont(), properties.font.default.proportionalFont);
    assert.equal(options.fixedWidthFont(), properties.font.default.fixedWidthFont);
    assert.equal(options.zoomLevel(), properties.view.default.zoomLevel);
    assert.deepEqual(options.windowBounds(), properties.view.default.windowBounds);
    assert.equal(options.accessibility(), properties.view.default.accessibility);
    assert.equal(options.lastRemoteSessionUrl(), properties.session.default.lastRemoteSessionUrl);
    assert.deepEqual(options.authCookies(), properties.session.default.authCookies);
    assert.deepEqual(options.tempAuthCookies(), properties.session.default.tempAuthCookies);
    assert.deepEqual(options.ignoredUpdateVersions(), properties.general.default.ignoredUpdateVersions);
    if (process.platform === 'win32') {
      assert.equal(options.rBinDir(), properties.platform.default.windows.rBinDir);
      assert.equal(options.peferR64(), properties.platform.default.windows.preferR64);
    } else {
      assert.equal(options.rBinDir(), nonWindowsRBinDir);
      assert.equal(options.peferR64(), nonWindowsPreferR64);
    }
  });
  it('set/get functionality returns correct values', () => {
    const options = testingDesktopOptions();

    const newProportionalFont = 'testProportionalFont';
    const newFixWidthFont = 'testFixWidthFont';
    const newZoom = 1.5;
    const newWindowBounds = { width: 123, height: 321, x: 0, y: 0 };
    const newAccessibility = !(properties.view.default.accessibility as boolean);
    const newLastRemoteSessionUrl = 'testLastRemoteSessionUrl';
    const newAuthCookies = ['test', 'Autht', 'Cookies'];
    const newTempAuthCookies = ['test', 'Temp', 'Auth', 'Cookies'];
    const newIgnoredUpdateVersions = ['test', 'Ignored', 'Update', 'Versions'];
    const newRBinDir = 'testRBinDir';
    const newPeferR64 = !(properties.platform.default.windows.preferR64 as boolean);

    const nonWindowsRBinDir = '';
    const nonWindowsPreferR64 = false;

    options.setProportionalFont(newProportionalFont);
    options.setFixedWidthFont(newFixWidthFont);
    options.setZoomLevel(newZoom);
    options.saveWindowBounds(newWindowBounds);
    options.setAccessibility(newAccessibility);
    options.setLastRemoteSessionUrl(newLastRemoteSessionUrl);
    options.setAuthCookies(newAuthCookies);
    options.setTempAuthCookies(newTempAuthCookies);
    options.setIgnoredUpdateVersions(newIgnoredUpdateVersions);
    options.setRBinDir(newRBinDir);
    options.setPeferR64(newPeferR64);

    assert.equal(options.proportionalFont(), newProportionalFont);
    assert.equal(options.fixedWidthFont(), newFixWidthFont);
    assert.equal(options.zoomLevel(), newZoom);
    assert.deepEqual(options.windowBounds(), newWindowBounds);
    assert.equal(options.accessibility(), newAccessibility);
    assert.equal(options.lastRemoteSessionUrl(), newLastRemoteSessionUrl);
    assert.deepEqual(options.authCookies(), newAuthCookies);
    assert.deepEqual(options.tempAuthCookies(), newTempAuthCookies);
    assert.deepEqual(options.ignoredUpdateVersions(), newIgnoredUpdateVersions);
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
    const newZoom = 2.5;

    assert.equal(options1.zoomLevel(), properties.view.default.zoomLevel);
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
    const defaultWinWidth = properties.view.default.windowBounds.width;
    const defaultWinHeight = properties.view.default.windowBounds.height;

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
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      fixedWidthFont(): string | undefined {
        return 'legacy font';
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    assert.strictEqual(testDesktopOptions.fixedWidthFont(), 'legacy font');
  });

  it('set font overrides legacy font option', () => {
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      fixedWidthFont(): string | undefined {
        return 'legacy font';
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    testDesktopOptions.setFixedWidthFont('new font');
    assert.strictEqual(testDesktopOptions.fixedWidthFont(), 'new font');
  });

  it('can get the zoom level', () => {
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      zoomLevel(): number | undefined {
        return 1.5;
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    assert.strictEqual(testDesktopOptions.zoomLevel(), 1.5);
  });

  it('set zoom level overrides legacy zoom level', () => {
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      zoomLevel(): number | undefined {
        return 1.5;
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    testDesktopOptions.setZoomLevel(0.5);
    assert.strictEqual(testDesktopOptions.zoomLevel(), 0.5);
  });

  it('WIPhas an error when setting an invalid zoom level', () => {
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      zoomLevel(): number | undefined {
        return 1.5;
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);
    const min = properties.view.properties.zoomLevel.minimum;
    const max = properties.view.properties.zoomLevel.maximum;

    expect(testDesktopOptions.setZoomLevel.bind(testDesktopOptions, min - 0.1)).to.throw();
    expect(testDesktopOptions.setZoomLevel.bind(testDesktopOptions, max + 0.1)).to.throw();
    expect(testDesktopOptions.setZoomLevel.bind(testDesktopOptions, min)).to.not.throw();
    expect(testDesktopOptions.setZoomLevel.bind(testDesktopOptions, max)).to.not.throw();
  });

  it('can get the rBinDir (Windows)', () => {
    const testRBinDir = 'C:/R/bin/x64';
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      rBinDir(): string | undefined {
        return testRBinDir;
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    assert.strictEqual(testDesktopOptions.rBinDir(), process.platform === 'win32' ? testRBinDir : '');
  });

  it('set rBinDir overrides the legacy rBinDir (Windows)', () => {
    const testRBinDir = 'C:/R/bin/x64';
    const mockLegacyOptions = new (class extends DesktopOptionsStub {
      rBinDir(): string | undefined {
        return 'C:/foo/R/bin/x64';
      }
    })();
    const testDesktopOptions = ElectronDesktopOptions(kTestingConfigDirectory, mockLegacyOptions);

    testDesktopOptions.setRBinDir(testRBinDir);
    assert.strictEqual(testDesktopOptions.rBinDir(), process.platform === 'win32' ? testRBinDir : '');
  });
});
