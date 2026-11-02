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
import { _createContextMenuTemplate, _showTerminalShortcuts } from '../../../src/main/context-menu';

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

  describe('terminal shortcuts', () => {
    const allEnabled: any = {
      editFlags: { canCut: true, canCopy: true, canPaste: true, canSelectAll: true },
    };
    const [cut, copy, paste, selectAll] = [0, 1, 2, 3];

    it('keeps the role default accelerators outside the terminal', () => {
      const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, false);

      assert.strictEqual(menu[cut].role, 'cut');
      assert.strictEqual(menu[copy].role, 'copy');
      assert.isUndefined(menu[copy].accelerator);
      assert.strictEqual(menu[paste].role, 'paste');
      assert.isUndefined(menu[paste].accelerator);
      assert.strictEqual(menu[selectAll].role, 'selectAll');
    });

    it('shows Ctrl+Shift+C and Ctrl+Shift+V for copy and paste, without registering them', () => {
      const menu: any[] = _createContextMenuTemplate({} as any, allEnabled, true);

      assert.strictEqual(menu[copy].role, 'copy');
      assert.strictEqual(menu[copy].accelerator, 'Ctrl+Shift+C');
      assert.isFalse(menu[copy].registerAccelerator);
      assert.strictEqual(menu[paste].role, 'paste');
      assert.strictEqual(menu[paste].accelerator, 'Ctrl+Shift+V');
      assert.isFalse(menu[paste].registerAccelerator);
    });

    it('drops the Ctrl+X and Ctrl+A hints from Cut and Select All but keeps the actions', () => {
      const sender = { cut: sinon.spy(), selectAll: sinon.spy() };
      const menu: any[] = _createContextMenuTemplate(sender as any, allEnabled, true);

      for (const index of [cut, selectAll]) {
        assert.isUndefined(menu[index].role);
        assert.isUndefined(menu[index].accelerator);
        menu[index].click();
      }
      assert.isTrue(sender.cut.calledOnce);
      assert.isTrue(sender.selectAll.calledOnce);
    });

    it('leaves disabled items disabled', () => {
      const allDisabled: any = {
        editFlags: { canCut: false, canCopy: false, canPaste: false, canSelectAll: false },
      };
      const menu: any[] = _createContextMenuTemplate({} as any, allDisabled, true);

      for (const index of [cut, copy, paste, selectAll]) {
        assert.isFalse(menu[index].enabled);
        assert.isUndefined(menu[index].accelerator);
      }
    });
  });

  describe('_showTerminalShortcuts', () => {
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

    it('is true over the terminal on Windows and Linux', async () => {
      for (const platform of ['win32', 'linux'] as const) {
        const params = textParams(sinon.stub().resolves(true));

        assert.isTrue(await _showTerminalShortcuts(params, platform));
        assert.include(params.frame.executeJavaScript.firstCall.args[0], 'xterm-helper-textarea');
      }
    });

    it('is false when something else has focus', async () => {
      assert.isFalse(await _showTerminalShortcuts(textParams(sinon.stub().resolves(false)), 'win32'));
    });

    it('is false on macOS without running the check', async () => {
      const params = textParams(sinon.stub().resolves(true));

      assert.isFalse(await _showTerminalShortcuts(params, 'darwin'));
      assert.isTrue(params.frame.executeJavaScript.notCalled);
    });

    it('is false for image menus without running the check', async () => {
      const params = { ...textParams(sinon.stub().resolves(true)), hasImageContents: true };

      assert.isFalse(await _showTerminalShortcuts(params, 'win32'));
      assert.isTrue(params.frame.executeJavaScript.notCalled);
    });

    it('is false in an iframe without running the check', async () => {
      const params = textParams(sinon.stub().resolves(true), {});

      assert.isFalse(await _showTerminalShortcuts(params, 'linux'));
      assert.isTrue(params.frame.executeJavaScript.notCalled);
    });

    it('is false when the frame is gone', async () => {
      assert.isFalse(await _showTerminalShortcuts({ hasImageContents: false, frame: null } as any, 'win32'));
    });

    it('logs and is false when the check fails', async () => {
      const params = textParams(sinon.stub().rejects(new Error('frame detached')));

      assert.isFalse(await _showTerminalShortcuts(params, 'win32'));
      assert.isTrue(errorLog.calledOnce);
      assert.include(errorLog.firstCall.args[0], 'frame detached');
    });
  });
});
