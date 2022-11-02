/*
 * desktop-browser-window.test.ts
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

import { assert } from 'chai';
import { describe } from 'mocha';

import { setVars } from '../../../src/core/environment';
import { DesktopBrowserWindow } from '../../../src/main/desktop-browser-window';
import { isWindowsDocker, restore, saveAndClear } from '../unit-utils';

if (!isWindowsDocker()) {
  describe('DesktopBrowserWindow', () => {
    const vars: Record<string, string> = {
      NODE_ENV: '',
    };
    const baseUrl = 'http://127.0.0.1:8080';

    before(() => {
      saveAndClear(vars);
      setVars({ NODE_ENV: 'TEST' });
    });

    after(() => {
      restore(vars);
    });

    it('construction creates a hidden BrowserWindow', () => {
      const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });
      assert.isObject(win);
      assert.isObject(win.window);
      assert.isFalse(win.window.isVisible());
    });

    it('allows navigation to presentation url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const presentationUrl = 'http://127.0.0.1:123';

      win.setPresentationUrl(presentationUrl);
      assert.isTrue(win.allowNavigation(presentationUrl));
    });

    it('allows navigation to tutorial url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const tutorialUrl = 'http://127.0.0.1:123';

      win.setTutorialUrl(tutorialUrl);
      assert.isTrue(win.allowNavigation(tutorialUrl));
    });

    it('allows navigation to viewer url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const viewerUrl = 'http://127.0.0.1:123';

      win.setViewerUrl(viewerUrl);
      assert.isTrue(win.allowNavigation(viewerUrl));
    });

    it('allows navigation to Shiny dialog url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const shinyDialogUrl = 'http://127.0.0.1:123';

      win.setShinyDialogUrl(shinyDialogUrl);
      assert.isTrue(win.allowNavigation(shinyDialogUrl));
    });

    it('isSafeHost detects unsafe host that looks safe', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      win.setViewerUrl('http://127.0.0.1:123');
      const unsafeUrl = 'http://www.example.com/127.0.0.1:123';

      assert.isFalse(win.allowNavigation(unsafeUrl));
    });

    it('set viewer URL checks for local URL', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl:  baseUrl,
        allowExternalNavigate: false,
      });

      const unsafeUrl = 'http://www.example.com';
      win.setViewerUrl(unsafeUrl);
      win.setTutorialUrl(unsafeUrl);
      win.setPresentationUrl(unsafeUrl);
      win.setShinyDialogUrl(unsafeUrl);
      assert.isFalse(win.allowNavigation(unsafeUrl));
    });
  });
}
