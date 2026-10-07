/*
 * gwt-callback.test.ts
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
import sinon from 'sinon';
import {
  app,
  BrowserWindow,
  clipboard,
  ClipboardItem,
  ipcMain,
  IpcMainInvokeEvent,
  nativeImage,
  webContents,
} from 'electron';
import { existsSync, mkdtempSync, realpathSync, rmSync, writeFileSync } from 'fs';
import os from 'os';
import path from 'path';
import { pathToFileURL } from 'url';
import { createSinonStubInstance, StubbedClass } from '../unit-utils';

import { FilePath } from '../../../src/core/file-path';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

import { GwtCallback } from '../../../src/main/gwt-callback';
import { MainWindow } from '../../../src/main/main-window';

function fakeBrowserWindow(state?: { visible?: boolean; minimized?: boolean; destroyed?: boolean }) {
  return {
    isDestroyed: sinon.stub().returns(state?.destroyed ?? false),
    isVisible: sinon.stub().returns(state?.visible ?? true),
    isMinimized: sinon.stub().returns(state?.minimized ?? false),
    show: sinon.stub(),
    showInactive: sinon.stub(),
    restore: sinon.stub(),
    moveTop: sinon.stub(),
    focus: sinon.stub(),
  };
}

describe('DesktopCallback', () => {
  // GwtCallback registers its ipcMain handlers in the constructor, and a channel
  // can only be handled once per process, so the instance is shared by the suite
  let mainWindow: StubbedClass<MainWindow>;
  let callback: GwtCallback;
  type InvokeHandler = (event: IpcMainInvokeEvent, ...args: unknown[]) => unknown;
  const invokeHandlers = new Map<string, InvokeHandler>();

  before(() => {
    mainWindow = createSinonStubInstance(MainWindow);
    const handle = sinon.spy(ipcMain, 'handle');
    callback = new GwtCallback(mainWindow);
    for (const call of handle.getCalls()) {
      invokeHandlers.set(call.args[0], call.args[1] as InvokeHandler);
    }
    handle.restore();
  });

  async function invoke(channel: string, ...args: unknown[]): Promise<unknown> {
    const handler = invokeHandlers.get(channel);
    assert.isDefined(handler, `no handler registered for '${channel}'`);
    return handler!({} as IpcMainInvokeEvent, ...args);
  }

  afterEach(() => {
    sinon.restore();
  });

  it('can be constructed', () => {
    assert.isNotEmpty(callback);
  });

  describe('dialogParentWindow', () => {
    // a parentless native dialog activates the app on macOS, so automation
    // runs must always get a parent even though the app is never focused
    it('prefers the requested window, then the focused window', () => {
      const preferred = fakeBrowserWindow() as unknown as BrowserWindow;
      const focused = fakeBrowserWindow() as unknown as BrowserWindow;
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(focused);

      assert.strictEqual(callback.dialogParentWindow(preferred), preferred);
      assert.strictEqual(callback.dialogParentWindow(), focused);
    });

    it('leaves the dialog parentless when nothing is focused', () => {
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(null);
      assert.isUndefined(callback.dialogParentWindow());
    });

    it('falls back to the main window in automation mode', () => {
      const main = fakeBrowserWindow() as unknown as BrowserWindow;
      mainWindow.window = main;
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(null);

      app.commandLine.appendSwitch('automation-agent');
      try {
        assert.strictEqual(callback.dialogParentWindow(), main);
      } finally {
        app.commandLine.removeSwitch('automation-agent');
      }
    });

    // a sheet on a window that is not on screen is held until the window comes
    // back, so the run would hang on a dialog it can never dismiss
    it('stays parentless in automation mode when the main window is off screen', () => {
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(null);

      app.commandLine.appendSwitch('automation-agent');
      try {
        for (const state of [{ minimized: true }, { visible: false }, { destroyed: true }]) {
          mainWindow.window = fakeBrowserWindow(state) as unknown as BrowserWindow;
          assert.isUndefined(callback.dialogParentWindow(), JSON.stringify(state));
        }
      } finally {
        app.commandLine.removeSwitch('automation-agent');
      }
    });
  });

  describe('desktop_bring_main_frame_behind_active', () => {
    // this callback means "make the main window as visible as possible, but leave
    // focus alone"; touching focus at all strands it on the main window under
    // window managers with focus-stealing prevention (#18635)
    function emit(main: ReturnType<typeof fakeBrowserWindow>, active: unknown) {
      mainWindow.window = main as unknown as BrowserWindow;
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(active as BrowserWindow | null);
      ipcMain.emit('desktop_bring_main_frame_behind_active');
    }

    it('restacks an already-visible main window beneath the active one', () => {
      const main = fakeBrowserWindow({ visible: true, minimized: false });
      const active = fakeBrowserWindow();
      emit(main, active);

      assert.isTrue(main.moveTop.calledOnce);
      assert.isTrue(active.moveTop.calledOnce);
      assert.isTrue(main.moveTop.calledBefore(active.moveTop));
      assert.isFalse(main.show.called);
      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
    });

    it('surfaces a hidden main window without activating it', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: false });
      emit(main, fakeBrowserWindow());

      assert.isTrue(main.showInactive.calledOnce);
      assert.isTrue(main.moveTop.calledOnce);
      assert.isFalse(main.show.called);
    });

    it('de-miniaturizes a minimized main window without activating it when it can', () => {
      // Windows: showInactive() is SW_SHOWNOACTIVATE, so the window is back on
      // screen and no longer minimized without focus having moved
      const main = fakeBrowserWindow({ visible: true, minimized: true });
      main.isMinimized = sinon.stub().onFirstCall().returns(true).returns(false);
      const active = fakeBrowserWindow();
      emit(main, active);

      assert.isTrue(main.showInactive.calledOnce);
      assert.isFalse(main.restore.called);
      assert.isFalse(active.focus.called);
      assert.isTrue(main.moveTop.calledOnce);
    });

    it('hands focus back when de-miniaturizing had to activate the main window', () => {
      // macOS and X11: showInactive() leaves the window minimized, and restore()
      // is an activating call there, so focus has to be returned explicitly
      const main = fakeBrowserWindow({ visible: true, minimized: true });
      const active = fakeBrowserWindow();
      emit(main, active);

      assert.isTrue(main.restore.calledOnce);
      assert.isTrue(main.moveTop.calledOnce);
      assert.isFalse(main.show.called);
      assert.isFalse(main.focus.called);
      assert.isTrue(active.focus.calledOnce);
      assert.isTrue(active.moveTop.calledBefore(active.focus));
    });

    it('never focuses either window unless the main window was minimized', () => {
      for (const state of [
        { visible: true, minimized: false },
        { visible: false, minimized: false },
      ]) {
        const main = fakeBrowserWindow(state);
        const active = fakeBrowserWindow();
        emit(main, active);

        assert.isFalse(main.focus.called, `main focused for ${JSON.stringify(state)}`);
        assert.isFalse(active.focus.called, `active focused for ${JSON.stringify(state)}`);
        sinon.restore();
      }
    });

    it('does nothing when the main window is itself the active window', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: true });
      emit(main, main);

      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
      assert.isFalse(main.moveTop.called);
    });

    it('does nothing when no window is focused', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: true });
      emit(main, null);

      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
      assert.isFalse(main.moveTop.called);
    });
  });

  describe('clipboard handlers', () => {
    let tempDir: string;
    let heldWrites: (() => void)[] = [];

    const tick = async () => new Promise((resolve) => setTimeout(resolve, 20));

    // Stub clipboard.writeText so each write stays pending until released,
    // standing in for a slow async write; onWrite runs when it completes.
    function holdTextWrites(onWrite?: (text: string) => void) {
      sinon.stub(clipboard, 'writeText').callsFake(async (text: string) => {
        await new Promise<void>((resolve) => heldWrites.push(resolve));
        onWrite?.(text);
      });
    }

    async function releaseWrites() {
      heldWrites.forEach((release) => release());
      heldWrites = [];
      await tick();
    }

    beforeEach(() => {
      tempDir = realpathSync(mkdtempSync(path.join(os.tmpdir(), 'gwt-callback-clipboard-')));
    });

    // the clipboard write queue is process-wide, so a write a failed test left
    // pending would stall every later read that waits on it
    afterEach(async () => {
      await releaseWrites();
      clipboard.clear();
      rmSync(tempDir, { recursive: true, force: true });
    });

    it('desktop_get_clipboard_text returns the clipboard text', async () => {
      await clipboard.writeText('clipboard text');
      assert.equal(await invoke('desktop_get_clipboard_text'), 'clipboard text');
    });

    // the renderer fires a write and may read straight back (an Emacs kill
    // followed by a yank), so a read must not overtake a pending async write
    it('desktop_get_clipboard_text waits for a pending write', async () => {
      let contents = 'old';
      holdTextWrites((text) => (contents = text));
      sinon.stub(clipboard, 'readText').callsFake(async () => contents);

      ipcMain.emit('desktop_set_clipboard_text', {}, 'new');
      const result = invoke('desktop_get_clipboard_text');
      await tick();
      await releaseWrites();
      assert.equal(await result, 'new');
    });

    it('desktop_get_clipboard_uris and _image wait for a pending write', async () => {
      holdTextWrites();
      const read = sinon.stub(clipboard, 'read').resolves([]);

      ipcMain.emit('desktop_set_clipboard_text', {}, 'new');
      const results = Promise.all([invoke('desktop_get_clipboard_uris'), invoke('desktop_get_clipboard_image')]);
      await tick();
      assert.isFalse(read.called, 'read before the write finished');

      await releaseWrites();
      assert.deepEqual(await results, [[], '']);
      assert.isTrue(read.calledTwice);
    });

    // a rejected IPC call never reaches the renderer's callback, so a failed
    // read must still answer or a visual-editor paste would hang
    it('desktop_get_clipboard_uris and _image treat a failed read as empty', async () => {
      sinon.stub(clipboard, 'read').rejects(new Error('clipboard unavailable'));
      assert.deepEqual(await invoke('desktop_get_clipboard_uris'), []);
      assert.equal(await invoke('desktop_get_clipboard_image'), '');
    });

    it('desktop_copy_page_region_to_clipboard writes after a pending write', async () => {
      const bitmap = Buffer.alloc(2 * 2 * 4, 0xff);
      const image = nativeImage.createFromBitmap(bitmap, { width: 2, height: 2 });
      mainWindow.window = { capturePage: sinon.stub().resolves(image) } as unknown as BrowserWindow;
      holdTextWrites();
      const write = sinon.spy(clipboard, 'write');

      ipcMain.emit('desktop_set_clipboard_text', {}, 'new');
      const copied = invoke('desktop_copy_page_region_to_clipboard', 0, 0, 2, 2);
      await tick();
      assert.isFalse(write.called, 'image written before the pending write finished');

      await releaseWrites();
      await copied;
      assert.isTrue(write.calledOnce);
      assert.isTrue(await clipboard.has('image/png'));
    });

    it('desktop_copy_page_region_to_clipboard keeps its place while capturing', async () => {
      const bitmap = Buffer.alloc(2 * 2 * 4, 0xff);
      const image = nativeImage.createFromBitmap(bitmap, { width: 2, height: 2 });
      let finishCapture: (() => void) | undefined;
      const capturePage = sinon.stub().callsFake(async () => {
        await new Promise<void>((resolve) => (finishCapture = resolve));
        return image;
      });
      mainWindow.window = { capturePage } as unknown as BrowserWindow;
      const order: string[] = [];
      sinon.stub(clipboard, 'writeText').callsFake(async () => {
        order.push('text');
      });
      sinon.stub(clipboard, 'write').callsFake(async () => {
        order.push('image');
      });

      const copied = invoke('desktop_copy_page_region_to_clipboard', 0, 0, 2, 2);
      await tick();
      ipcMain.emit('desktop_set_clipboard_text', {}, 'copied during capture');
      await tick();
      assert.isDefined(finishCapture, 'capture never started');
      finishCapture!();
      await copied;
      await tick();

      assert.deepEqual(order, ['image', 'text']);
    });

    // the screen can change while earlier writes finish, so the capture must
    // not wait for them the way the clipboard write does
    it('desktop_copy_page_region_to_clipboard captures without waiting for a pending write', async () => {
      const bitmap = Buffer.alloc(2 * 2 * 4, 0xff);
      const capturePage = sinon.stub().resolves(nativeImage.createFromBitmap(bitmap, { width: 2, height: 2 }));
      mainWindow.window = { capturePage } as unknown as BrowserWindow;
      holdTextWrites();

      ipcMain.emit('desktop_set_clipboard_text', {}, 'pending');
      const copied = invoke('desktop_copy_page_region_to_clipboard', 0, 0, 2, 2);
      await tick();
      assert.isTrue(capturePage.calledOnce, 'capture waited for the pending write');

      await releaseWrites();
      await copied;
    });

    it('desktop_copy_page_region_to_clipboard leaves the clipboard alone when capture fails', async () => {
      mainWindow.window = {
        capturePage: sinon.stub().rejects(new Error('capture failed')),
      } as unknown as BrowserWindow;
      const write = sinon.stub(clipboard, 'write').resolves();

      await invoke('desktop_copy_page_region_to_clipboard', 0, 0, 2, 2);
      assert.isFalse(write.called);
    });

    // the queued write only awaits the capture once earlier writes finish, so
    // a conversion failure before then must already be handled
    it('desktop_copy_page_region_to_clipboard handles a PNG conversion failure', async () => {
      const unhandled = sinon.spy();
      process.on('unhandledRejection', unhandled);
      try {
        const image = {
          toPNG: () => {
            throw new Error('encode failed');
          },
        };
        mainWindow.window = { capturePage: sinon.stub().resolves(image) } as unknown as BrowserWindow;
        holdTextWrites();
        const write = sinon.stub(clipboard, 'write').resolves();

        ipcMain.emit('desktop_set_clipboard_text', {}, 'pending');
        const copied = invoke('desktop_copy_page_region_to_clipboard', 0, 0, 2, 2);
        await tick();
        await releaseWrites();
        await copied;

        assert.isFalse(unhandled.called, 'conversion failure was not handled');
        assert.isFalse(write.called);
      } finally {
        process.off('unhandledRejection', unhandled);
      }
    });

    it('desktop_clipboard_paste waits for a pending write', async () => {
      let contents = 'old';
      let pasted: string | undefined;
      holdTextWrites((text) => (contents = text));
      const target = {
        isDestroyed: sinon.stub().returns(false),
        paste: sinon.stub().callsFake(() => (pasted = contents)),
      };
      sinon.stub(webContents, 'getFocusedWebContents').returns(target as unknown as Electron.WebContents);

      ipcMain.emit('desktop_set_clipboard_text', {}, 'new');
      ipcMain.emit('desktop_clipboard_paste', {});
      await tick();
      assert.isFalse(target.paste.called, 'pasted before the write finished');

      await releaseWrites();
      assert.equal(pasted, 'new');
    });

    // the window can close while the write is pending; pasting into its
    // destroyed WebContents would throw from a promise nothing awaits
    it('desktop_clipboard_paste skips a target destroyed during a pending write', async () => {
      holdTextWrites();
      const target = { isDestroyed: sinon.stub().returns(false), paste: sinon.stub() };
      sinon.stub(webContents, 'getFocusedWebContents').returns(target as unknown as Electron.WebContents);

      ipcMain.emit('desktop_set_clipboard_text', {}, 'new');
      ipcMain.emit('desktop_clipboard_paste', {});
      await tick();
      assert.lengthOf(heldWrites, 1, 'write never started');
      target.isDestroyed.returns(true);
      await releaseWrites();

      assert.isFalse(target.paste.called);
    });

    // file names that need percent-encoding in a URI must come back as the
    // plain filesystem paths
    it('desktop_get_clipboard_uris returns the paths of copied files', async () => {
      const files = ['plain.txt', 'with space.txt', 'café.txt'].map((name) => path.join(tempDir, name));
      files.forEach((file) => writeFileSync(file, ''));
      const uris = files.map((file) => pathToFileURL(file).href);
      await clipboard.write([new ClipboardItem({ 'text/uri-list': uris.join('\r\n') })]);

      const expected = process.platform === 'win32' ? files.map((file) => file.replace(/\\/g, '/')) : files;
      // macOS hands back file names in decomposed Unicode (NFD); the file
      // system treats both forms as the same file
      const actual = (await invoke('desktop_get_clipboard_uris')) as string[];
      assert.deepEqual(
        actual.map((file) => file.normalize('NFC')),
        expected.map((file) => file.normalize('NFC')),
      );
      actual.forEach((file) => assert.isTrue(existsSync(file), `${file} does not exist`));
    });

    it('desktop_get_clipboard_uris drops a file URI that has no valid path', async () => {
      // an encoded separator is rejected by fileURLToPath on every platform
      const valid = pathToFileURL(path.join(tempDir, 'kept.txt')).href;
      const item = new ClipboardItem({ 'text/uri-list': `file:///a%2Fb\r\n${valid}` });
      sinon.stub(clipboard, 'read').resolves([item]);

      const expected = path.join(tempDir, 'kept.txt');
      assert.deepEqual(await invoke('desktop_get_clipboard_uris'), [
        process.platform === 'win32' ? expected.replace(/\\/g, '/') : expected,
      ]);
    });

    it('desktop_get_clipboard_uris returns nothing without a URI list', async () => {
      await clipboard.writeText('not a uri list');
      assert.deepEqual(await invoke('desktop_get_clipboard_uris'), []);
    });

    it('desktop_get_clipboard_image saves a clipboard image to a PNG file', async () => {
      const application = new Application();
      application.setScratchTempDir(new FilePath(tempDir));
      setApplication(application);
      try {
        const bitmap = Buffer.alloc(2 * 3 * 4, 0xff);
        const png = nativeImage.createFromBitmap(bitmap, { width: 2, height: 3 }).toPNG();
        await clipboard.write([new ClipboardItem({ 'image/png': new Blob([new Uint8Array(png)]) })]);

        const pngPath = (await invoke('desktop_get_clipboard_image')) as string;
        assert.isTrue(pngPath.startsWith(tempDir), `${pngPath} is not under ${tempDir}`);
        assert.deepEqual(nativeImage.createFromPath(pngPath).getSize(), { width: 2, height: 3 });
      } finally {
        clearApplicationSingleton();
      }
    });

    it('desktop_get_clipboard_image returns nothing without an image', async () => {
      await clipboard.writeText('not an image');
      assert.equal(await invoke('desktop_get_clipboard_image'), '');
    });
  });
});
