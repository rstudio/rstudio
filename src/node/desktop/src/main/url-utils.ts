/*
 * url-utils.ts
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

import { Err } from '../core/err';
import { logger } from '../core/logger';
import { WaitResult, WaitTimeoutFn, waitWithTimeout } from '../core/wait-utils';

/**
 *
 * @param url the URL to check
 * @returns true if is an about: url
 */
export function isAboutUrl(url: string): boolean {
  return url === 'about:blank';
}

export function getAuthority(url: string): string {
  if (isAboutUrl(url)) {
    return url;
  }

  try {
    // uses 127.0.0.1 for host if url is just a path
    const targetUrl = new URL(url, 'http://127.0.0.1');
    return `${targetUrl.protocol}//${targetUrl.host}`;
  } catch (error: unknown) {
    logger().logError(error);
    return '';
  }
}

export function isChromeGpuUrl(url: string): boolean {
  return url.replace(/\/$/, '') === 'chrome://gpu';
}

/**
 * Checks the `url` if it is local
 *
 * @param url the URL to check
 * @returns true if it is local
 */
export function isLocalUrl(url: URL) {
  const host = url.hostname;

  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]';
}

/**
 * Checks the `url` if it is allowed
 *
 * @param url the URL to check
 * @returns true if the protocol is allowed
 */
export function isAllowedProtocol(url: URL) {
  const protocol = url.protocol;
  const allowedProtocols = ['http:', 'https:', 'mailto:', 'data:'];
  return allowedProtocols.includes(protocol);
}

/**
 * Determine if given host is considered safe to load in an IDE window.
 */
export function isSafeHost(host: string): boolean {
  const safeHosts = ['.youtube.com', '.vimeo.com', '.c9.ms', '.google.com'];

  for (const safeHost of safeHosts) {
    if (host.endsWith(safeHost)) {
      return true;
    }
  }
  return false;
}

// a probe that connects but never receives a response (e.g. another process
// shadowing the port) must not hang its caller's retry loop
const kProbeRequestTimeoutMs = 2500;

/**
 * Resolves true once something answers an HTTP request to `url` with any
 * status. The session's listener rejects this unauthenticated probe, but a
 * rejection still means it is up.
 */
export async function probeUrl(url: string): Promise<boolean> {
  try {
    const response = await fetch(url, {
      redirect: 'manual',
      signal: AbortSignal.timeout(kProbeRequestTimeoutMs),
    });
    await response.body?.cancel(); // release the connection without reading the body
    return true;
  } catch (error: unknown) {
    logger().logDebug(`Connection to ${url} failed: ${error instanceof Error ? error.message : String(error)}`);
    return false;
  }
}

/**
 * Wait for a URL to respond, with retries and timeout
 */
export async function waitForUrlWithTimeout(
  url: string,
  initialWaitMs: number,
  incrementWaitMs: number,
  maxWaitSec: number,
): Promise<Err> {
  const checkReady: WaitTimeoutFn = async () => {
    const reachable = await probeUrl(url);
    return new WaitResult(reachable ? 'WaitSuccess' : 'WaitContinue');
  };

  return waitWithTimeout(checkReady, initialWaitMs, incrementWaitMs, maxWaitSec);
}

export function makeAbsoluteUrl(url: string): URL {
  const link = new URL(url, 'http://127.0.0.1');
  return link;
}
