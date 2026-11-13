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

// The GWT bootstrap reads these cookies to pick the build to load (UI language, web dialogs),
// and the client reloads the page when they don't match the user's preferences. An instance
// with its own session data (see instance-session-data.ts) starts without them, so every
// instance records them when they change, and such an instance starts from the record. Each
// cookie has a file of its own, so instances changing different cookies never undo each other.
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

function isSharedCookie(value: unknown, name: string): value is SharedCookie {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const cookie = value as SharedCookie;
  return (
    cookie.name === name &&
    typeof cookie.value === 'string' &&
    typeof cookie.domain === 'string' &&
    typeof cookie.path === 'string' &&
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
    if (isSharedCookie(parsed, name)) {
      return parsed;
    }
  } catch {
    // reported below, like any other content that isn't the cookie
  }
  logger().logWarning(`Ignoring malformed shared cookie in ${file}`);
  return undefined;
}

function recordCookie(dir: string, name: string, cookie: SharedCookie | undefined): void {
  const file = cookieFile(dir, name);
  try {
    if (cookie === undefined) {
      rmSync(file, { force: true });
      return;
    }
    // instances read this while others write it, so replace it whole
    mkdirSync(dir, { recursive: true });
    const temp = `${file}.${process.pid}.tmp`;
    writeFileSync(temp, JSON.stringify(cookie));
    renameSync(temp, file);
  } catch (error: unknown) {
    logger().logWarning(`Unable to record shared cookie in ${file}: ${safeError(error).message}`);
  }
}

async function recordAll(session: Session, dir: string): Promise<void> {
  try {
    const cookies = await session.cookies.get({});
    for (const name of kSharedCookieNames) {
      const cookie = cookies.find((candidate) => candidate.name === name);
      recordCookie(dir, name, cookie && toShared(cookie));
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
 * Shares the GWT bootstrap cookies between instances through files in `dir`. An instance using
 * the shared session data records its cookies; any other instance starts from the record. Either
 * way, later changes are recorded. Resolves once the cookies are in place.
 */
export async function shareBootstrapCookies(session: Session, dir: string, sharedSessionData: boolean): Promise<void> {
  await (sharedSessionData ? recordAll(session, dir) : restore(session, dir));

  session.cookies.on('changed', (_event, cookie, cause, removed) => {
    // overwriting a cookie reports its removal, then the new one
    if (kSharedCookieNames.includes(cookie.name) && !(removed && cause === 'overwrite')) {
      recordCookie(dir, cookie.name, removed ? undefined : toShared(cookie));
    }
  });
}
