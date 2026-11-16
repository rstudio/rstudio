/*
 * page-language.ts
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

import { ipcMain, Session } from 'electron';
import i18next from 'i18next';

import { kGetLanguageChannel } from '../core/desktop-language';
import { logger } from '../core/logger';
import { changeLanguage } from './i18n-manager';
import { kLocaleCookie } from './shared-cookies';

/**
 * Keeps the main process's UI language in step with the LOCALE cookie. Resolves once the
 * language stored by a previous run (if any) is in effect, so pages loaded afterwards use it.
 */
export async function trackLocaleCookie(session: Session): Promise<void> {
  session.cookies.on('changed', (_event, cookie, _cause, removed) => {
    if (!removed && cookie.name === kLocaleCookie) {
      void changeLanguage(cookie.value);
    }
  });

  try {
    const cookies = await session.cookies.get({ name: kLocaleCookie });
    await changeLanguage(cookies.length > 0 ? cookies[0].value : 'en');
  } catch (error: unknown) {
    logger().logError(error);
  }
}

/**
 * Answers the desktop's own pages (loading, error, connect, choose R), which read the
 * language synchronously from their preload.
 */
export function registerLanguageChannel(): void {
  ipcMain.on(kGetLanguageChannel, (event) => {
    event.returnValue = i18next.resolvedLanguage ?? 'en';
  });
}
