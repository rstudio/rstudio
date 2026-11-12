/*
 * squirrel-relay.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import { createHash } from 'crypto';
import http from 'http';
import { FetchLike } from '../../../src/main/installer-staging';
import { SquirrelRelay, startSquirrelRelay } from '../../../src/main/squirrel-relay';
import { BuildArtifact } from '../../../src/main/update-feed';

// The relay, asked for its feed and its download the way Squirrel asks: with
// real requests to the loopback listener. Only the builds site is a fake.

const bytes = Buffer.from('this stands in for a zipped RStudio.app');
const notesUrl = 'https://docs.example.com/news/';

const entry: BuildArtifact = {
  version: '2026.12.0-daily+9',
  url: 'https://dl.example.com/RStudio-2026.12.0-daily-9.zip',
  filename: 'RStudio-2026.12.0-daily-9.zip',
  sha256: createHash('sha256').update(bytes).digest('hex'),
  size: bytes.length,
  commit: '',
  date: '2026-11-02T00:00:00Z',
};

/** A builds site that serves `chunks` for every url, remembering what was asked for. */
function serving(chunks: Buffer[], status = 200): { fetch: FetchLike; urls: string[] } {
  const urls: string[] = [];
  const fetch: FetchLike = async (url) => {
    urls.push(url);
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(new Uint8Array(chunk));
        }
        controller.close();
      },
    });
    const length = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
    return new Response(status === 200 ? body : null, { status, headers: { 'content-length': String(length) } });
  };
  return { fetch, urls };
}

interface Answer {
  status: number;
  headers: http.IncomingHttpHeaders;
  body: Buffer;
  /** the connection was dropped before the body was complete */
  truncated: boolean;
}

async function get(url: string): Promise<Answer> {
  return new Promise((resolve, reject) => {
    http
      .get(url, (response) => {
        const parts: Buffer[] = [];
        const answer = (truncated: boolean) =>
          resolve({
            status: response.statusCode ?? 0,
            headers: response.headers,
            body: Buffer.concat(parts),
            truncated,
          });
        response.on('data', (part: Buffer) => parts.push(part));
        response.on('end', () => answer(false));
        response.on('error', () => answer(true));
      })
      .on('error', reject);
  });
}

describe('squirrel relay', () => {
  let relay: SquirrelRelay | null = null;
  let progress: number[];
  let failures: Error[];

  beforeEach(() => {
    progress = [];
    failures = [];
  });

  afterEach(() => {
    relay?.close();
    relay = null;
  });

  async function start(fetch: FetchLike, idleTimeoutMs?: number): Promise<SquirrelRelay> {
    relay = await startSquirrelRelay(
      entry,
      notesUrl,
      fetch,
      (fraction) => progress.push(fraction),
      (error) => failures.push(error),
      idleTimeoutMs,
    );
    return relay;
  }

  /** the download url the feed gives Squirrel */
  async function downloadUrl(feedUrl: string): Promise<string> {
    const feed = await get(feedUrl);
    assert.equal(feed.status, 200);
    return (JSON.parse(feed.body.toString()) as { url: string }).url;
  }

  it('feeds Squirrel the build, with itself as where to download it', async () => {
    const { fetch, urls } = serving([bytes]);
    const { feedUrl } = await start(fetch);
    assert.match(feedUrl, /^http:\/\/127\.0\.0\.1:\d+\/[0-9a-f]{32}\/releases\.json$/);

    const feed = await get(feedUrl);
    assert.equal(feed.status, 200);
    assert.deepEqual(JSON.parse(feed.body.toString()), {
      url: feedUrl.replace('releases.json', entry.filename),
      name: entry.version,
      notes: notesUrl,
      pub_date: entry.date,
      sha256: entry.sha256,
      size: entry.size,
    });

    // the feed alone downloads nothing
    assert.deepEqual(urls, []);
  });

  it('relays the build from the builds site, reporting its progress', async () => {
    const { fetch, urls } = serving([bytes.subarray(0, 10), bytes.subarray(10)]);
    const { feedUrl } = await start(fetch);

    const download = await get(await downloadUrl(feedUrl));
    assert.equal(download.status, 200);
    assert.equal(download.headers['content-length'], String(bytes.length));
    assert.deepEqual(download.body, bytes);
    assert.deepEqual(urls, [entry.url]);

    assert.deepEqual(progress, [10 / bytes.length, 1]);
    assert.deepEqual(failures, []);
  });

  it('answers nothing but the two paths it gave Squirrel', async () => {
    const { fetch, urls } = serving([bytes]);
    const { feedUrl } = await start(fetch);
    const origin = new URL(feedUrl).origin;

    for (const path of ['/', '/releases.json', `/${entry.filename}`, `/${'0'.repeat(32)}/${entry.filename}`]) {
      assert.equal((await get(origin + path)).status, 404, path);
    }
    assert.deepEqual(urls, []);
  });

  it('says why when the builds site refuses the download', async () => {
    const { feedUrl } = await start(serving([bytes], 403).fetch);

    const download = await get(await downloadUrl(feedUrl));
    assert.equal(download.status, 502);
    assert.equal(failures.length, 1);
    assert.include(failures[0].message, 'HTTP 403');
  });

  it('drops the connection, and says why, when the download stalls part way', async () => {
    // a body that delivers its first bytes and then nothing more
    const stalling: FetchLike = async () => {
      const body = new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(new Uint8Array(bytes.subarray(0, 10)));
        },
      });
      return new Response(body, { status: 200, headers: { 'content-length': String(bytes.length) } });
    };
    const { feedUrl } = await start(stalling, 50);

    const download = await get(await downloadUrl(feedUrl));
    assert.isTrue(download.truncated);
    assert.deepEqual(download.body, bytes.subarray(0, 10));
    assert.equal(failures.length, 1);
    assert.include(failures[0].message, 'stalled');
  });

  it('gives up a download Squirrel abandons, and leaves it for Squirrel to report', async () => {
    // a body with no end, so the relay is still writing when the request is
    // dropped; the builds site hears of it as a cancelled body
    let cancel!: () => void;
    const cancelled = new Promise<void>((resolve) => (cancel = resolve));
    const endless: FetchLike = async () => {
      const body = new ReadableStream<Uint8Array>({
        pull(controller) {
          controller.enqueue(new Uint8Array(64 * 1024));
        },
        cancel,
      });
      return new Response(body, { status: 200, headers: { 'content-length': String(2 ** 30) } });
    };
    const { feedUrl } = await start(endless);
    const url = await downloadUrl(feedUrl);

    await new Promise<void>((resolve, reject) => {
      const request = http.get(url, (response) => {
        response.once('data', () => {
          request.destroy();
          resolve();
        });
        response.on('error', () => undefined);
      });
      request.on('error', reject);
    });

    await cancelled;
    await new Promise((resolve) => setImmediate(resolve));
    assert.deepEqual(failures, []);
  });

  it('stops listening once closed', async () => {
    const { feedUrl, close } = await start(serving([bytes]).fetch);
    close();

    let refused = false;
    await get(feedUrl).catch(() => (refused = true));
    assert.isTrue(refused);
  });
});
