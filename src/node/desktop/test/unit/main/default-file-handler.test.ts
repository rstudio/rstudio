/*
 * default-file-handler.test.ts
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

import { assert } from 'chai';
import { app } from 'electron';
import { describe } from 'mocha';
import sinon from 'sinon';
import { properties } from '../../../../../cpp/session/resources/schema/user-state-schema.json';
import { FilePath } from '../../../src/core/file-path';
import {
  kRFileExtensions,
  promptForDefaultRFileHandlerOnce,
  setDefaultApplicationForRFiles,
  setMacOSDefaultApplicationForFileExtensions,
} from '../../../src/main/default-file-handler';
import DesktopOptions from '../../../src/main/preferences/desktop-options';
import {
  clearOptionsSingleton,
  ElectronDesktopOptions,
} from '../../../src/main/preferences/electron-desktop-options';
import desktop from '../../../src/native/desktop.node';
import { tempDirectory } from '../unit-utils';

const kTestingConfigDirectory = tempDirectory('DefaultFileHandlerTesting').toString();

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

// The native functions only exist in the macOS build of the addon, so install
// fakes by assignment rather than with sinon.stub(), which needs an existing property.
type NativeModule = {
  macOSDefaultApplicationForFileExtension?: (extension: string) => {
    bundleIdentifier: string;
    isCurrentApplication: boolean;
  };
  macOSSetDefaultApplicationForFileExtension?: (extension: string) => Promise<void>;
};

describe('DefaultFileHandler', () => {
  const native = desktop as unknown as NativeModule;
  const originalQuery = native.macOSDefaultApplicationForFileExtension;
  const originalSet = native.macOSSetDefaultApplicationForFileExtension;

  // exercises the real addon; the test runner is Electron.app, never the default for .R
  (process.platform === 'darwin' ? describe : describe.skip)('native', () => {
    it('describes the application registered for an extension', () => {
      const result = originalQuery!('R');
      assert.isString(result.bundleIdentifier);
      assert.isFalse(result.isCurrentApplication);
      assert.isFunction(originalSet);
    });

    it('reports no application for an unknown extension', () => {
      const result = originalQuery!('rstudio-no-such-extension');
      assert.equal(result.bundleIdentifier, '');
      assert.isFalse(result.isCurrentApplication);
    });
  });
  let query: sinon.SinonStub;
  let set: sinon.SinonStub;

  // the extensions some other application currently owns
  function ownedElsewhere(...extensions: string[]) {
    query.callsFake((extension: string) => ({
      bundleIdentifier: extensions.includes(extension) ? 'com.example.other' : 'com.rstudio.desktop',
      isCurrentApplication: !extensions.includes(extension),
    }));
  }

  beforeEach(() => {
    query = sinon.stub();
    set = sinon.stub().resolves();
    native.macOSDefaultApplicationForFileExtension = query;
    native.macOSSetDefaultApplicationForFileExtension = set;
    sinon.stub(process, 'platform').value('darwin');
    sinon.stub(app, 'isPackaged').value(true);
    sinon.stub(app.commandLine, 'hasSwitch').returns(false);
    ElectronDesktopOptions(kTestingConfigDirectory, new DesktopOptionsStub());
  });

  afterEach(() => {
    sinon.restore();
    native.macOSDefaultApplicationForFileExtension = originalQuery;
    native.macOSSetDefaultApplicationForFileExtension = originalSet;
    clearOptionsSingleton();
    new FilePath(kTestingConfigDirectory).removeIfExistsSync();
  });

  describe('setMacOSDefaultApplicationForFileExtensions', () => {
    it('only asks for the extensions another application owns', async () => {
      ownedElsewhere('R', 'Rd');

      await setMacOSDefaultApplicationForFileExtensions(['R', 'Rmd', 'Rd']);

      assert.deepEqual(
        set.getCalls().map((call) => call.args[0]),
        ['R', 'Rd'],
      );
    });

    it('propagates a failure from macOS', async () => {
      ownedElsewhere('R');
      set.rejects(new Error('nope'));

      try {
        await setMacOSDefaultApplicationForFileExtensions(['R']);
        assert.fail('expected rejection');
      } catch (error: unknown) {
        assert.equal((error as Error).message, 'nope');
      }
    });
  });

  describe('setDefaultApplicationForRFiles', () => {
    it('covers every R file extension', async () => {
      ownedElsewhere(...kRFileExtensions);

      await setDefaultApplicationForRFiles();

      assert.deepEqual(
        set.getCalls().map((call) => call.args[0]),
        kRFileExtensions,
      );
    });

    it('rejects off macOS', async () => {
      sinon.stub(process, 'platform').value('linux');

      try {
        await setDefaultApplicationForRFiles();
        assert.fail('expected rejection');
      } catch (error: unknown) {
        assert.match((error as Error).message, /macOS/);
      }
      assert.isTrue(set.notCalled);
    });
  });

  describe('promptForDefaultRFileHandlerOnce', () => {
    it('asks macOS once when another application owns .R', async () => {
      ownedElsewhere('R');

      await promptForDefaultRFileHandlerOnce();
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.calledOnceWithExactly('R'));
      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('records the check without asking when RStudio already owns .R', async () => {
      ownedElsewhere();

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('does not ask again after a previous run', async () => {
      ownedElsewhere('R');
      ElectronDesktopOptions().setMacOSDefaultFileHandlerPrompted(true);

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
    });

    it('stays quiet when not packaged', async () => {
      ownedElsewhere('R');
      sinon.stub(app, 'isPackaged').value(false);

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
      assert.isFalse(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('stays quiet under automation', async () => {
      ownedElsewhere('R');
      (app.commandLine.hasSwitch as sinon.SinonStub).withArgs('automation-agent').returns(true);

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
      assert.isFalse(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('stays quiet off macOS', async () => {
      ownedElsewhere('R');
      sinon.stub(process, 'platform').value('win32');

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
    });

    it('swallows a failure from macOS', async () => {
      ownedElsewhere('R');
      set.rejects(new Error('nope'));

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });
  });
});
