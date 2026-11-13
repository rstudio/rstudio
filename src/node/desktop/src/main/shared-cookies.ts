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
import { readFileSync, renameSync, writeFileSync } from 'fs';
import { safeError } from '../core/err';
import { logger } from '../core/logger';

// The GWT bootstrap reads these cookies to pick the build to load (UI language, web dialogs),
// and the client reloads the page when they don't match the user's preferences. An instance
// with its own session data (see instance-session-data.ts) starts without them, so every
// instance records them in a file when they change, and such an instance starts from that.
const kSharedCookieNames = ['LOCALE', 'WEBDIALOG'];

interface SharedCookie {
  name: string;
  value: string;
  domain: string;
  path: string;
  hostOnly?: boolean;
  secure?: boolean;
  expirationDate?: number;
}

function toShared(cookie: Cookie): SharedCookie {
  const { name, value, domain = '', path = '/', hostOnly, secure, expirationDate } = cookie;
  return { name, value, domain, path, hostOnly, secure, expirationDate };
}

function isSharedCookie(value: unknown): value is SharedCookie {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const cookie = value as SharedCookie;
  return (
    kSharedCookieNames.includes(cookie.name) &&
    typeof cookie.value === 'string' &&
    typeof cookie.domain === 'string' &&
    typeof cookie.path === 'string' &&
    (cookie.expirationDate === undefined || typeof cookie.expirationDate === 'number')
  );
}

function readRecord(file: string): SharedCookie[] {
  let content;
  try {
    content = readFileSync(file, 'utf8');
  } catch (error: unknown) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
      logger().logError(error);
    }
    return [];
  }

  try {
    const parsed: unknown = JSON.parse(content);
    if (Array.isArray(parsed)) {
      return parsed.filter(isSharedCookie);
    }
  } catch {
    // reported below, like any other content that isn't a list of cookies
  }
  logger().logWarning(`Ignoring malformed shared cookies in ${file}`);
  return [];
}

function writeRecord(file: string, cookies: SharedCookie[]): void {
  // instances read this while others write it, so replace it whole
  const temp = `${file}.${process.pid}.tmp`;
  try {
    writeFileSync(temp, JSON.stringify(cookies));
    renameSync(temp, file);
  } catch (error: unknown) {
    logger().logWarning(`Unable to record shared cookies in ${file}: ${safeError(error).message}`);
  }
}

async function recordAll(session: Session, file: string): Promise<void> {
  try {
    const cookies = await session.cookies.get({});
    writeRecord(file, cookies.filter((cookie) => kSharedCookieNames.includes(cookie.name)).map(toShared));
  } catch (error: unknown) {
    logger().logError(error);
  }
}

function recordChange(file: string, cookie: Cookie, removed: boolean): void {
  const others = readRecord(file).filter((shared) => shared.name !== cookie.name);
  writeRecord(file, removed ? others : [...others, toShared(cookie)]);
}

async function restore(session: Session, file: string): Promise<void> {
  for (const cookie of readRecord(file)) {
    const host = cookie.domain.replace(/^\./, '');
    try {
      await session.cookies.set({
        url: `${cookie.secure ? 'https' : 'http'}://${host}${cookie.path}`,
        name: cookie.name,
        value: cookie.value,
        // a host-only cookie is set by leaving out the domain
        domain: cookie.hostOnly ? undefined : cookie.domain,
        path: cookie.path,
        secure: cookie.secure,
        expirationDate: cookie.expirationDate,
      });
    } catch (error: unknown) {
      logger().logError(error);
    }
  }
}

/**
 * Shares the GWT bootstrap cookies between instances through `file`. An instance using the
 * shared session data records its cookies; any other instance starts from the record. Either
 * way, later changes are recorded. Resolves once the cookies are in place.
 */
export async function shareBootstrapCookies(session: Session, file: string, sharedSessionData: boolean): Promise<void> {
  await (sharedSessionData ? recordAll(session, file) : restore(session, file));

  session.cookies.on('changed', (_event, cookie, cause, removed) => {
    // overwriting a cookie reports its removal, then the new one
    if (kSharedCookieNames.includes(cookie.name) && !(removed && cause === 'overwrite')) {
      recordChange(file, cookie, removed);
    }
  });
}
