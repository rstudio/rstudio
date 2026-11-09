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
import { app, dialog } from 'electron';
import { describe } from 'mocha';
import sinon from 'sinon';
import { properties } from '../../../../../cpp/session/resources/schema/user-state-schema.json';
import { FilePath } from '../../../src/core/file-path';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';
import {
  kRFileExtensions,
  macOSConfirmsDefaultApplicationChanges,
  promptForDefaultRFileHandlerOnce,
  setDefaultApplicationForRFiles,
  setMacOSDefaultApplicationForFileExtensions,
} from '../../../src/main/default-file-handler';
import DesktopOptions from '../../../src/main/preferences/desktop-options';
import { clearOptionsSingleton, ElectronDesktopOptions } from '../../../src/main/preferences/electron-desktop-options';
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

// exercises the real addon; the test runner is Electron.app, never the default for .R
(process.platform === 'darwin' ? describe : describe.skip)('DefaultFileHandler native', () => {
  it('reports whether this application opens an extension', () => {
    assert.isFalse(desktop.macOSIsDefaultApplicationForFileExtension('R'));
    assert.isFalse(desktop.macOSIsDefaultApplicationForFileExtension('rstudio-no-such-extension'));
  });

  it('refuses an invalid extension without asking', async () => {
    try {
      await desktop.macOSSetDefaultApplicationForFileExtension('');
      assert.fail('expected rejection');
    } catch (error: unknown) {
      assert.match((error as Error).message, /not a valid file extension/);
    }
  });
});

describe('DefaultFileHandler', () => {
  let set: sinon.SinonStub;
  let showMessageBox: sinon.SinonStub;
  let elsewhere: Set<string>;

  // the extensions some other application currently owns; accepting a
  // confirmation moves one to RStudio
  function ownedElsewhere(...extensions: string[]) {
    elsewhere = new Set(extensions);
  }

  // the user answers "keep" to the confirmation for these extensions
  function declines(...extensions: string[]) {
    for (const extension of extensions) {
      set.withArgs(extension).resolves();
    }
  }

  function askedFor(): string[] {
    return set.getCalls().map((call) => call.args[0] as string);
  }

  beforeEach(() => {
    elsewhere = new Set();
    sinon
      .stub(desktop, 'macOSIsDefaultApplicationForFileExtension')
      .callsFake((extension: string) => !elsewhere.has(extension));
    set = sinon.stub(desktop, 'macOSSetDefaultApplicationForFileExtension').callsFake(async (extension: string) => {
      elsewhere.delete(extension);
    });
    sinon.stub(process, 'platform').value('darwin');
    sinon.stub(app, 'isPackaged').value(true);
    sinon.stub(app.commandLine, 'hasSwitch').returns(false);

    // a macOS that confirms the change itself, unless a test says otherwise
    sinon.stub(process, 'getSystemVersion').returns('26.4.0');
    showMessageBox = sinon.stub(dialog, 'showMessageBox');
    setApplication(new Application());

    ElectronDesktopOptions(kTestingConfigDirectory, new DesktopOptionsStub());
  });

  afterEach(() => {
    sinon.restore();
    clearApplicationSingleton();
    clearOptionsSingleton();
    new FilePath(kTestingConfigDirectory).removeIfExistsSync();
  });

  describe('macOSConfirmsDefaultApplicationChanges', () => {
    it('is true from macOS 26.4 on', () => {
      const confirms = (version: string) => {
        (process.getSystemVersion as sinon.SinonStub).returns(version);
        return macOSConfirmsDefaultApplicationChanges();
      };

      assert.isFalse(confirms('15.7.1'));
      assert.isFalse(confirms('26.3.1'));
      assert.isTrue(confirms('26.4'));
      assert.isTrue(confirms('26.4.0'));
      assert.isTrue(confirms('27.0'));
    });
  });

  describe('setMacOSDefaultApplicationForFileExtensions', () => {
    it('only asks for the extensions another application owns', async () => {
      ownedElsewhere('R', 'Rd');

      const errors = await setMacOSDefaultApplicationForFileExtensions(['R', 'Rmd', 'Rd']);

      assert.deepEqual(askedFor(), ['R', 'Rd']);
      assert.deepEqual(errors, []);
    });

    it('keeps asking after a failure partway through', async () => {
      ownedElsewhere('R', 'Rmd', 'Rd');
      set.withArgs('Rmd').rejects(new Error('nope'));

      const errors = await setMacOSDefaultApplicationForFileExtensions(['R', 'Rmd', 'Rd']);

      assert.deepEqual(askedFor(), ['R', 'Rmd', 'Rd']);
      assert.deepEqual(errors, ['.Rmd: nope']);
    });
  });

  describe('setDefaultApplicationForRFiles', () => {
    it('covers every R file extension', async () => {
      ownedElsewhere(...kRFileExtensions);

      const result = await setDefaultApplicationForRFiles();

      assert.deepEqual(askedFor(), kRFileExtensions);
      assert.deepEqual(result, { elsewhere: [], error: '' });
    });

    it('still offers the rest after a decline, and reports what was kept', async () => {
      ownedElsewhere('R', 'Rmd', 'Rproj');
      declines('R');

      const result = await setDefaultApplicationForRFiles();

      assert.deepEqual(askedFor(), ['R', 'Rmd', 'Rproj']);
      assert.deepEqual(result, { elsewhere: ['R'], error: '' });
    });

    it('reports each failure alongside what is still elsewhere', async () => {
      ownedElsewhere('R', 'Rmd', 'Rproj');
      set.withArgs('Rmd').rejects(new Error('nope'));
      set.withArgs('Rproj').rejects(new Error('also nope'));

      const result = await setDefaultApplicationForRFiles();

      assert.deepEqual(result, { elsewhere: ['Rmd', 'Rproj'], error: '.Rmd: nope\n.Rproj: also nope' });
    });

    it('refuses when not packaged, since Electron.app would become the default', async () => {
      ownedElsewhere('R');
      sinon.stub(app, 'isPackaged').value(false);

      const result = await setDefaultApplicationForRFiles();

      assert.isTrue(set.notCalled);
      assert.deepEqual(result.elsewhere, []);
      assert.match(result.error, /installed copy/);
    });

    it('answers the startup offer when it asks about .R', async () => {
      ownedElsewhere('R');
      declines('R');

      await setDefaultApplicationForRFiles();
      set.resetHistory();
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it("leaves the startup offer alone when .R was already RStudio's", async () => {
      ownedElsewhere('Rmd');

      await setDefaultApplicationForRFiles();

      assert.isFalse(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('refuses off macOS', async () => {
      sinon.stub(process, 'platform').value('linux');

      const result = await setDefaultApplicationForRFiles();

      assert.isTrue(set.notCalled);
      assert.match(result.error, /macOS/);
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

    it('leaves the question to macOS from 26.4 on', async () => {
      ownedElsewhere('R');

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(showMessageBox.notCalled);
      assert.isTrue(set.calledOnceWithExactly('R'));
    });

    it('asks first before macOS 26.4, which would switch .R silently', async () => {
      ownedElsewhere('R');
      (process.getSystemVersion as sinon.SinonStub).returns('15.7.1');
      showMessageBox.resolves({ response: 0, checkboxChecked: false });

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(showMessageBox.calledOnce);
      assert.isTrue(set.calledOnceWithExactly('R'));
    });

    it('leaves .R alone, and does not ask again, when declined before macOS 26.4', async () => {
      ownedElsewhere('R');
      (process.getSystemVersion as sinon.SinonStub).returns('15.7.1');
      showMessageBox.resolves({ response: 1, checkboxChecked: false });

      await promptForDefaultRFileHandlerOnce();
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(showMessageBox.calledOnce);
      assert.isTrue(set.notCalled);
      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('does not ask again after a decline', async () => {
      ownedElsewhere('R');
      declines('R');

      await promptForDefaultRFileHandlerOnce();
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.calledOnceWithExactly('R'));
      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });

    it('keeps the offer while RStudio already owns .R', async () => {
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.notCalled);
      assert.isFalse(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());

      // e.g. Xcode was installed later and took .R
      ownedElsewhere('R');
      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(set.calledOnceWithExactly('R'));
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
      set.withArgs('R').rejects(new Error('nope'));

      await promptForDefaultRFileHandlerOnce();

      assert.isTrue(ElectronDesktopOptions().macOSDefaultFileHandlerPrompted());
    });
  });
});
