/*
 * shared-cookies.ts
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

import { Cookie, Session } from 'electron';
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'fs';
import path from 'path';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { kLocalSessionHost } from '../core/system';

export const kLocaleCookie = 'LOCALE';
export const kWebDialogCookie = 'WEBDIALOG';

// The GWT bootstrap reads these cookies to pick the build to load (UI language, web dialogs),
// and the client reloads the page when they don't match the user's preferences. An instance
// with its own session data (see instance-session-data.ts) starts without them, so every
// instance records them when they change, and such an instance starts from the record. Each
// cookie has a file of its own, so instances changing different cookies never undo each other.
const kSharedCookieNames = [kLocaleCookie, kWebDialogCookie];

// only the desktop's own sessions' cookies are shared; other hosts (a remote server in Desktop
// Pro, say) have their own
const kSessionUrl = `http://${kLocalSessionHost}/`;

interface SharedCookie {
  value: string;
  expirationDate?: number;
}

function isShared(cookie: Cookie): boolean {
  return kSharedCookieNames.includes(cookie.name) && cookie.domain === kLocalSessionHost && cookie.path === '/';
}

function isSharedCookie(value: unknown): value is SharedCookie {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const cookie = value as SharedCookie;
  return (
    typeof cookie.value === 'string' &&
    (cookie.expirationDate === undefined || typeof cookie.expirationDate === 'number')
  );
}

function cookieFile(dir: string, name: string): string {
  return path.join(dir, `${name}.json`);
}

function readCookie(dir: string, name: string): SharedCookie | undefined {
  const file = cookieFile(dir, name);
  let content;
  try {
    content = readFileSync(file, 'utf8');
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
      logger().logError(error);
    }
    return undefined;
  }

  try {
    const parsed: unknown = JSON.parse(content);
    if (isSharedCookie(parsed)) {
      return parsed;
    }
  } catch {
    // reported below, like any other content that isn't a cookie
  }
  logger().logWarning(`Ignoring malformed shared cookie in ${file}`);
  return undefined;
}

function recordCookie(dir: string, name: string, cookie: Cookie | undefined): void {
  const file = cookieFile(dir, name);
  const temp = `${file}.${process.pid}.tmp`;
  try {
    if (cookie === undefined) {
      rmSync(file, { force: true });
      return;
    }
    // instances read this while others write it, so replace it whole
    mkdirSync(dir, { recursive: true });
    const shared: SharedCookie = { value: cookie.value, expirationDate: cookie.expirationDate };
    writeFileSync(temp, JSON.stringify(shared));
    renameSync(temp, file);
  } catch (error: unknown) {
    rmSync(temp, { force: true });
    logger().logWarning(`Unable to record shared cookie in ${file}: ${safeError(error).message}`);
  }
}

async function recordAll(session: Session, dir: string): Promise<void> {
  try {
    const cookies = await session.cookies.get({ url: kSessionUrl });
    for (const name of kSharedCookieNames) {
      recordCookie(
        dir,
        name,
        cookies.find((cookie) => cookie.name === name && isShared(cookie)),
      );
    }
  } catch (error: unknown) {
    logger().logError(error);
  }
}

async function restore(session: Session, dir: string): Promise<void> {
  for (const name of kSharedCookieNames) {
    const cookie = readCookie(dir, name);
    if (cookie === undefined) {
      continue;
    }
    try {
      await session.cookies.set({ url: kSessionUrl, name, ...cookie });
    } catch (error: unknown) {
      logger().logError(error);
    }
  }
}

/**
 * Shares the GWT bootstrap cookies between instances through files in `dir`. An instance using
 * the shared session data records its cookies; any other instance starts from the record. Either
 * way, later changes are recorded. Resolves once the cookies are in place.
 */
export async function shareBootstrapCookies(session: Session, dir: string, sharedSessionData: boolean): Promise<void> {
  await (sharedSessionData ? recordAll(session, dir) : restore(session, dir));

  session.cookies.on('changed', (_event, cookie, cause, removed) => {
    // overwriting a cookie reports its removal, then the new one
    if (isShared(cookie) && !(removed && cause === 'overwrite')) {
      recordCookie(dir, cookie.name, removed ? undefined : cookie);
    }
  });
}
