/*
 * squirrel-relay.ts
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

import { randomBytes } from 'crypto';
import http from 'http';
import { AddressInfo } from 'net';
import { PassThrough } from 'stream';
import { safeError } from '../core/err';
import { downloadArtifact, FetchLike, kDownloadIdleTimeoutMs } from './installer-staging';
import { BuildArtifact, squirrelFeedDocument } from './update-feed';

// The loopback listener Squirrel.Mac updates from. Squirrel wants its own
// feed format from an update server, so the listener serves a one-entry feed
// for the build already chosen. Squirrel then downloads whatever that feed
// names without saying how far along it is (Electron's autoUpdater has no
// progress event), so the feed names the listener itself, which relays the
// build from the builds site and counts the bytes as they pass. Squirrel
// still checks what arrives against the feed's checksum and size, and the
// new bundle's code signature. Nothing here touches Electron: the fetch is
// injected, so the relay is unit-tested with real requests.

export interface SquirrelRelay {
  /** what autoUpdater.setFeedURL() is given */
  feedUrl: string;
  /** stop listening, and drop a download still being relayed */
  close(): void;
}

/**
 * Start a listener that feeds Squirrel `entry`. `onProgress` hears the
 * fraction of the build relayed so far. `onFailed` hears why a download the
 * builds site did not deliver was cut short: Squirrel only sees a dropped
 * connection.
 */
export async function startSquirrelRelay(
  entry: BuildArtifact,
  notesUrl: string,
  fetch: FetchLike,
  onProgress: (fraction: number) => void,
  onFailed: (error: Error) => void,
  idleTimeoutMs = kDownloadIdleTimeoutMs,
): Promise<SquirrelRelay> {
  // any local process can reach the listener, but only Squirrel, which is
  // handed these paths, should be able to start a download through it
  const root = `/${randomBytes(16).toString('hex')}`;
  const feedPath = `${root}/releases.json`;
  const downloadPath = `${root}/${encodeURIComponent(entry.filename)}`;

  // plain http is fine here: the listener is loopback-only, and what it
  // relays was fetched over https
  const server = new http.Server();

  const relay = (response: http.ServerResponse) => {
    // the download is piped through `body` rather than straight into the
    // response, which a failed download would take down with it: Squirrel
    // would then learn of the failure, and might report it, before
    // `onFailed` could say why
    const body = new PassThrough();

    // Squirrel hanging up is its own failure (or the application quitting),
    // which it reports itself; the download from the builds site goes too
    let abandoned = false;
    response.once('close', () => {
      abandoned = !response.writableFinished;
      body.destroy();
    });

    const sink = (total: number) => {
      const length = total > 0 ? { 'Content-Length': total } : {};
      response.writeHead(200, { 'Content-Type': 'application/zip', 'Cache-Control': 'no-store', ...length });
      body.pipe(response);
      return body;
    };

    downloadArtifact(entry, sink, fetch, onProgress, idleTimeoutMs).catch((error: unknown) => {
      if (abandoned) {
        return;
      }

      onFailed(safeError(error));
      if (response.headersSent) {
        response.destroy();
      } else {
        response.writeHead(502).end();
      }
    });
  };

  server.on('request', (request, response) => {
    if (request.method === 'GET' && request.url === feedPath) {
      const { port } = server.address() as AddressInfo;
      const url = `http://127.0.0.1:${port}${downloadPath}`;
      response.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      response.end(squirrelFeedDocument({ ...entry, url }, notesUrl));
    } else if (request.method === 'GET' && request.url === downloadPath) {
      relay(response);
    } else {
      response.writeHead(404).end();
    }
  });

  return new Promise((resolve, reject) => {
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address() as AddressInfo;
      resolve({
        feedUrl: `http://127.0.0.1:${port}${feedPath}`,
        close: () => {
          server.close();
          server.closeAllConnections();
        },
      });
    });
  });
}
