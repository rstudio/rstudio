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
import { EventEmitter } from 'events';
import http from 'http';
import { FetchLike } from '../../../src/main/installer-staging';
import {
  downloadWithSquirrel,
  SquirrelRelay,
  SquirrelUpdater,
  startSquirrelRelay,
} from '../../../src/main/squirrel-relay';
import { BuildArtifact } from '../../../src/main/update-feed';

// The relay, asked for its feed and its download the way Squirrel asks: with
// real requests to the loopback listener. The builds site is a fake, and so
// is Squirrel where a whole download is driven.

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

/** A builds site whose body never ends; `cancelled` settles once the relay gives it up. */
function endless(): { fetch: FetchLike; cancelled: Promise<void> } {
  let cancel!: () => void;
  const cancelled = new Promise<void>((resolve) => (cancel = resolve));
  const fetch: FetchLike = async () => {
    const body = new ReadableStream<Uint8Array>({
      pull(controller) {
        controller.enqueue(new Uint8Array(64 * 1024));
      },
      cancel,
    });
    return new Response(body, { status: 200, headers: { 'content-length': String(2 ** 30) } });
  };
  return { fetch, cancelled };
}

/** Ask for `url` and drop the connection once the first bytes arrive. */
async function hangUp(url: string): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const request = http.get(url, (response) => {
      response.once('data', () => {
        request.destroy();
        resolve();
      });
      response.on('error', () => undefined);
    });
    request.on('error', reject);
  });
}

async function isRefused(url: string): Promise<boolean> {
  return get(url).then(
    () => false,
    () => true,
  );
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
    const site = endless();
    const { feedUrl } = await start(site.fetch);
    await hangUp(await downloadUrl(feedUrl));

    await site.cancelled;
    await new Promise((resolve) => setImmediate(resolve));
    assert.deepEqual(failures, []);
  });

  it('stops listening once closed', async () => {
    const { feedUrl, close } = await start(serving([bytes]).fetch);
    close();
    assert.isTrue(await isRefused(feedUrl));
  });
});

type SquirrelMode = 'stage' | 'refuse-bundle' | 'hang-up';

/**
 * Stands in for Squirrel.Mac: checkForUpdates() reads the feed, downloads
 * what it names and checks its size and SHA-256, then reports through the
 * events Electron's autoUpdater emits.
 */
class FakeSquirrel extends EventEmitter implements SquirrelUpdater {
  feedUrl = '';
  feedUrls: string[] = [];
  setFeedError: Error | null = null;
  /** errors reported while no download was listening */
  unheard: Error[] = [];
  /** the latest check, settled once its report is out */
  run: Promise<void> = Promise.resolve();

  constructor(public mode: SquirrelMode = 'stage') {
    super();
    // electronUpdaterHost() keeps a listener of its own, so an error no
    // download hears is logged rather than thrown
    this.on('error', (error: Error) => {
      if (this.listenerCount('error') === 1) {
        this.unheard.push(error);
      }
    });
  }

  setFeedURL(options: { url: string }): void {
    this.feedUrl = options.url;
    this.feedUrls.push(options.url);
    if (this.setFeedError) {
      throw this.setFeedError;
    }
  }

  checkForUpdates(): void {
    this.run = this.check().catch((error: Error) => {
      this.emit('error', error);
    });
  }

  /** the listeners a download left behind */
  leftListening(): number {
    return (
      this.listenerCount('update-downloaded') +
      this.listenerCount('update-not-available') +
      this.listenerCount('error') -
      1
    );
  }

  private async check(): Promise<void> {
    const feed = JSON.parse((await get(this.feedUrl)).body.toString()) as { url: string; sha256: string; size: number };
    if (this.mode === 'hang-up') {
      await hangUp(feed.url);
      throw new Error('The network connection was lost.');
    }

    const download = await get(feed.url);
    if (download.status !== 200 || download.truncated) {
      throw new Error('Update download failed');
    }
    const digest = createHash('sha256').update(download.body).digest('hex');
    if (download.body.length !== feed.size || digest !== feed.sha256) {
      throw new Error('The update does not match its checksum');
    }
    if (this.mode === 'refuse-bundle') {
      throw new Error('Could not locate update bundle');
    }

    // as Electron emits it: (event, releaseNotes, releaseName, releaseDate, updateURL)
    this.emit('update-downloaded', {}, 'notes', entry.version, new Date(), feed.url);
  }
}

describe('downloadWithSquirrel', () => {
  async function failure(download: Promise<void>): Promise<string> {
    return download.then(
      () => assert.fail('the download should have failed'),
      (error: Error) => error.message,
    );
  }

  it('resolves once Squirrel has the build, and leaves nothing behind', async () => {
    const squirrel = new FakeSquirrel();
    const progress: number[] = [];
    await downloadWithSquirrel(squirrel, entry, notesUrl, serving([bytes]).fetch, (f) => progress.push(f));

    assert.deepEqual(progress, [1]);
    assert.equal(squirrel.leftListening(), 0);
    assert.isTrue(await isRefused(squirrel.feedUrl));
  });

  it("fails with the relay's reason, and Squirrel's own report of it goes nowhere", async () => {
    const squirrel = new FakeSquirrel();
    const download = downloadWithSquirrel(squirrel, entry, notesUrl, serving([bytes], 403).fetch, () => undefined);

    assert.include(await failure(download), 'HTTP 403');
    await squirrel.run;
    assert.deepEqual(
      squirrel.unheard.map((error) => error.message),
      ['Update download failed'],
    );
    assert.equal(squirrel.leftListening(), 0);
    assert.isTrue(await isRefused(squirrel.feedUrl));
  });

  it("fails with Squirrel's reason when Squirrel refuses what arrived", async () => {
    const squirrel = new FakeSquirrel('refuse-bundle');
    const download = downloadWithSquirrel(squirrel, entry, notesUrl, serving([bytes]).fetch, () => undefined);

    assert.equal(await failure(download), 'Could not locate update bundle');
    assert.equal(squirrel.leftListening(), 0);
    assert.isTrue(await isRefused(squirrel.feedUrl));
  });

  it("fails with Squirrel's reason when Squirrel hangs up, and stops the download from the builds site", async () => {
    const squirrel = new FakeSquirrel('hang-up');
    const site = endless();
    const download = downloadWithSquirrel(squirrel, entry, notesUrl, site.fetch, () => undefined);

    assert.equal(await failure(download), 'The network connection was lost.');
    await site.cancelled;
    assert.deepEqual(squirrel.unheard, []);
    assert.equal(squirrel.leftListening(), 0);
  });

  it('fails, and cleans up, when Squirrel will not take the feed', async () => {
    const squirrel = new FakeSquirrel();
    squirrel.setFeedError = new Error('Could not get code signature for running application');
    const { fetch, urls } = serving([bytes]);
    const download = downloadWithSquirrel(squirrel, entry, notesUrl, fetch, () => undefined);

    assert.equal(await failure(download), 'Could not get code signature for running application');
    assert.equal(squirrel.leftListening(), 0);
    assert.isTrue(await isRefused(squirrel.feedUrl));
    assert.deepEqual(urls, []);
  });

  it('gives a download after a failed one a relay of its own', async () => {
    const squirrel = new FakeSquirrel();
    const failed = downloadWithSquirrel(squirrel, entry, notesUrl, serving([bytes], 503).fetch, () => undefined);
    assert.include(await failure(failed), 'HTTP 503');
    await squirrel.run;

    const progress: number[] = [];
    await downloadWithSquirrel(squirrel, entry, notesUrl, serving([bytes]).fetch, (f) => progress.push(f));
    assert.deepEqual(progress, [1]);

    const [first, second] = squirrel.feedUrls;
    assert.notEqual(new URL(first).port + new URL(first).pathname, new URL(second).port + new URL(second).pathname);
    assert.equal(squirrel.unheard.length, 1); // the first download's, after it settled
    assert.equal(squirrel.leftListening(), 0);
  });
});
