/*
 * context-menu.test.ts
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
/* eslint-disable @typescript-eslint/no-explicit-any */

import { describe } from 'mocha';
import { assert } from 'chai';
import sinon from 'sinon';
import { NullLogger, setLogger } from '../../../src/core/logger';
import { _createContextMenuTemplate, _isOverTerminal, _selectAllInTerminal } from '../../../src/main/context-menu';

describe('ContextMenu', () => {
  it('Context menu is created correctly', () => {
    const paramsWithImage: any = {
      hasImageContents: true,
    };

    const contextMenuTemplateWithImage = _createContextMenuTemplate({} as any, paramsWithImage);

    console.log('length ', contextMenuTemplateWithImage.length);

    assert.isTrue(contextMenuTemplateWithImage.length == 6, 'Context Menu With Image should have 6 elements');

    const paramsWithoutImageA: any = {
      editFlags: {
        canCut: true,
        canCopy: false,
        canPaste: true,
        canSelectAll: false,
      },
    };

    const paramsWithoutImageB: any = {
      editFlags: {
        canCut: false,
        canCopy: false,
        canPaste: false,
        canSelectAll: false,
      },
    };

    const paramsWithoutImageC: any = {
      editFlags: {
        canCut: true,
        canCopy: true,
        canPaste: true,
        canSelectAll: true,
      },
    };

    const contextMenuTemplateWithoutImageA = _createContextMenuTemplate({} as any, paramsWithoutImageA);
    const contextMenuTemplateWithoutImageB = _createContextMenuTemplate({} as any, paramsWithoutImageB);
    const contextMenuTemplateWithoutImageC = _createContextMenuTemplate({} as any, paramsWithoutImageC);

    const checkParamsAndMenu = (contextMenu: any, params: any) => {
      Object.keys(params.editFlags).forEach((key, index) => {
        if (params.editFlags[key] == false) {
          assert.isFalse(
            contextMenu[index].enabled,
            'Context Menu Without Image is not disabled, or is being created in a different order',
          );
        }
      });
    };

    checkParamsAndMenu(contextMenuTemplateWithoutImageA, paramsWithoutImageA);
    checkParamsAndMenu(contextMenuTemplateWithoutImageB, paramsWithoutImageB);
    checkParamsAndMenu(contextMenuTemplateWithoutImageC, paramsWithoutImageC);
  });

  describe('terminal menu', () => {
    const allEnabled: any = {
      editFlags: { canCut: true, canCopy: true, canPaste: true, canSelectAll: true },
    };
    const [cut, copy, paste, selectAll] = [0, 1, 2, 3];

    beforeEach(() => {
      setLogger(new NullLogger());
    });

    afterEach(() => {
      sinon.restore();
    });

    const paramsWithFrame = (editFlags: any, executeJavaScript: sinon.SinonStub): any => ({
      editFlags,
      frame: { parent: null, executeJavaScript },
    });

    it('keeps the roles and their default accelerators outside the terminal', () => {
      for (const platform of ['win32', 'linux', 'darwin'] as const) {
        const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, false, platform);

        assert.strictEqual(menu[cut].role, 'cut');
        assert.strictEqual(menu[copy].role, 'copy');
        assert.isUndefined(menu[copy].accelerator);
        assert.strictEqual(menu[paste].role, 'paste');
        assert.isUndefined(menu[paste].accelerator);
        assert.strictEqual(menu[selectAll].role, 'selectAll');
      }
    });

    it('shows Ctrl+Shift+C and Ctrl+Shift+V for copy and paste, without registering them', () => {
      for (const platform of ['win32', 'linux'] as const) {
        const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, true, platform);

        assert.strictEqual(menu[copy].role, 'copy');
        assert.strictEqual(menu[copy].accelerator, 'Ctrl+Shift+C');
        assert.isFalse(menu[copy].registerAccelerator);
        assert.strictEqual(menu[paste].role, 'paste');
        assert.strictEqual(menu[paste].accelerator, 'Ctrl+Shift+V');
        assert.isFalse(menu[paste].registerAccelerator);
      }
    });

    it('keeps the copy and paste roles and their defaults over the terminal on macOS', () => {
      const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, true, 'darwin');

      assert.strictEqual(menu[cut].role, 'cut');
      assert.strictEqual(menu[copy].role, 'copy');
      assert.isUndefined(menu[copy].accelerator);
      assert.strictEqual(menu[paste].role, 'paste');
      assert.isUndefined(menu[paste].accelerator);
    });

    it('drops the Ctrl+X hint from Cut but keeps the action', () => {
      const sender = { cut: sinon.spy() };
      const menu: any[] = _createContextMenuTemplate(sender as any, allEnabled, true, 'linux');

      assert.isUndefined(menu[cut].role);
      assert.isUndefined(menu[cut].accelerator);
      menu[cut].click();
      assert.isTrue(sender.cut.calledOnce);
    });

    it("has Select All select the terminal's buffer, on every platform", () => {
      for (const platform of ['win32', 'linux', 'darwin'] as const) {
        const sender = { selectAll: sinon.spy() };
        const executeJavaScript = sinon.stub().resolves(true);
        const params = paramsWithFrame(allEnabled.editFlags, executeJavaScript);
        const menu: any[] = _createContextMenuTemplate(sender as any, params, true, platform);

        assert.isUndefined(menu[selectAll].role);
        menu[selectAll].click();
        assert.isTrue(sender.selectAll.notCalled);
        assert.include(executeJavaScript.firstCall.args[0], 'rstudio-terminal-select-all');
      }
    });

    it('shows Cmd+A for Select All over the terminal on macOS only, without registering it', () => {
      const mac: any[] = _createContextMenuTemplate({} as any, allEnabled, true, 'darwin');
      assert.strictEqual(mac[selectAll].accelerator, 'Cmd+A');
      assert.isFalse(mac[selectAll].registerAccelerator);

      for (const platform of ['win32', 'linux'] as const) {
        const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, true, platform);
        assert.isUndefined(menu[selectAll].accelerator);
      }
    });

    it('leaves Cut, Copy and Paste disabled when they are, but enables Select All', () => {
      const allDisabled: any = {
        editFlags: { canCut: false, canCopy: false, canPaste: false, canSelectAll: false },
      };
      const menu: any[] = _createContextMenuTemplate({} as any, allDisabled, true, 'linux');

      for (const index of [cut, copy, paste]) {
        assert.isFalse(menu[index].enabled);
        assert.isUndefined(menu[index].accelerator);
      }
      assert.isUndefined(menu[selectAll].enabled);
    });
  });

  describe('_selectAllInTerminal', () => {
    let errorLog: sinon.SinonSpy;

    beforeEach(() => {
      const log = new NullLogger();
      setLogger(log);
      errorLog = sinon.spy(log, 'logErrorMessage');
    });

    afterEach(() => {
      sinon.restore();
    });

    it('sends the select-all event to the focused terminal', async () => {
      const executeJavaScript = sinon.stub().resolves(true);

      await _selectAllInTerminal({ executeJavaScript } as any);

      assert.isTrue(executeJavaScript.calledOnce);
      assert.include(executeJavaScript.firstCall.args[0], 'xterm-helper-textarea');
      assert.isTrue(errorLog.notCalled);
    });

    it('logs when the terminal no longer has focus', async () => {
      await _selectAllInTerminal({ executeJavaScript: sinon.stub().resolves(false) } as any);

      assert.isTrue(errorLog.calledOnce);
      assert.include(errorLog.firstCall.args[0], 'no longer has focus');
    });

    it('logs when the frame is gone', async () => {
      await _selectAllInTerminal(null);

      assert.isTrue(errorLog.calledOnce);
      assert.include(errorLog.firstCall.args[0], 'frame is gone');
    });

    it('logs when the script fails', async () => {
      await _selectAllInTerminal({ executeJavaScript: sinon.stub().rejects(new Error('frame detached')) } as any);

      assert.isTrue(errorLog.calledOnce);
      assert.include(errorLog.firstCall.args[0], 'frame detached');
    });
  });

  describe('_isOverTerminal', () => {
    let errorLog: sinon.SinonSpy;

    beforeEach(() => {
      const log = new NullLogger();
      setLogger(log);
      errorLog = sinon.spy(log, 'logErrorMessage');
    });

    afterEach(() => {
      sinon.restore();
    });

    const textParams = (executeJavaScript: sinon.SinonStub, parent: unknown = null): any => ({
      hasImageContents: false,
      frame: { parent, executeJavaScript },
    });

    it("is true when xterm's helper textarea has focus", async () => {
      const params = textParams(sinon.stub().resolves(true));

      assert.isTrue(await _isOverTerminal(params));
      assert.include(params.frame.executeJavaScript.firstCall.args[0], 'xterm-helper-textarea');
    });

    it('is false when something else has focus', async () => {
      assert.isFalse(await _isOverTerminal(textParams(sinon.stub().resolves(false))));
    });

    it('is false for image menus without running the check', async () => {
      const params = { ...textParams(sinon.stub().resolves(true)), hasImageContents: true };

      assert.isFalse(await _isOverTerminal(params));
      assert.isTrue(params.frame.executeJavaScript.notCalled);
    });

    it('is false in an iframe without running the check', async () => {
      const params = textParams(sinon.stub().resolves(true), {});

      assert.isFalse(await _isOverTerminal(params));
      assert.isTrue(params.frame.executeJavaScript.notCalled);
    });

    it('is false when the frame is gone', async () => {
      assert.isFalse(await _isOverTerminal({ hasImageContents: false, frame: null } as any));
    });

    it('logs and is false when the check fails', async () => {
      const params = textParams(sinon.stub().rejects(new Error('frame detached')));

      assert.isFalse(await _isOverTerminal(params));
      assert.isTrue(errorLog.calledOnce);
      assert.include(errorLog.firstCall.args[0], 'frame detached');
    });
  });
});
