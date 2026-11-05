/*
 * installer-staging.test.ts
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
import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, utimesSync, writeFileSync } from 'fs';
import os from 'os';
import path from 'path';
import {
  FetchLike,
  kPartialDownloadTtlMs,
  pruneStagedInstallers,
  stageInstaller,
  verifyInstaller,
} from '../../../src/main/installer-staging';
import { BuildArtifact } from '../../../src/main/update-feed';

const bytes = Buffer.from('this stands in for an NSIS installer');

function artifact(filename: string, content: Buffer = bytes): BuildArtifact {
  return {
    version: '2026.12.0-daily+9',
    url: `https://dl.example.com/${filename}`,
    filename,
    sha256: createHash('sha256').update(content).digest('hex'),
    size: content.length,
    commit: '',
    date: '',
  };
}

/** A fetch that serves `content` for every url, counting the calls. */
function serving(content: Buffer, status = 200): { fetch: FetchLike; calls: () => number } {
  let calls = 0;
  const fetch: FetchLike = async () => {
    calls++;
    return new Response(status === 200 ? new Uint8Array(content) : null, {
      status,
      headers: { 'content-length': String(content.length) },
    });
  };
  return { fetch, calls: () => calls };
}

describe('installer staging', () => {
  let dir: string;
  beforeEach(() => {
    dir = mkdtempSync(path.join(os.tmpdir(), 'rstudio-updates-test-'));
  });
  afterEach(() => {
    rmSync(dir, { recursive: true, force: true });
  });

  describe('stageInstaller', () => {
    it('downloads, verifies and leaves only the final file', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      const progress: number[] = [];
      const file = await stageInstaller(dir, entry, serving(bytes).fetch, (f) => progress.push(f));

      assert.equal(file, path.join(dir, entry.filename));
      assert.deepEqual(readFileSync(file), bytes);
      assert.deepEqual(readdirSync(dir), [entry.filename]);
      assert.equal(progress[progress.length - 1], 1);
    });

    it('refuses, before downloading, any name but the installer of the version offered', async () => {
      const names = [
        'RStudio-2026.12.0-daily-9.dmg',
        'RStudio-2026.12.0-daily-10.exe',
        'RStudio-pro-2026.12.0-daily-9.pro1.exe',
        '..\\RStudio-2026.12.0-daily-9.exe',
        'RStudio-2026.12.0-daily-9.exe:stream',
        'RStudio’; Start-Process calc; ’.exe',
      ];
      const { fetch, calls } = serving(bytes);
      for (const name of names) {
        let failure = '';
        await stageInstaller(dir, artifact(name), fetch).catch((error: Error) => (failure = error.message));
        assert.include(failure, 'unexpected installer file', name);
      }
      assert.equal(calls(), 0);
      assert.deepEqual(readdirSync(dir), []);
    });

    it('rejects a download whose checksum does not match and leaves nothing behind', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      const tampered = Buffer.from('this stands in for an NSIS inst@ller');
      try {
        await stageInstaller(dir, entry, serving(tampered).fetch);
        assert.fail('should have rejected');
      } catch (error: unknown) {
        assert.match((error as Error).message, /checksum/);
      }
      assert.deepEqual(readdirSync(dir), []);
    });

    it('rejects an interrupted download (too few bytes) and leaves nothing behind', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      try {
        await stageInstaller(dir, entry, serving(bytes.subarray(0, 10)).fetch);
        assert.fail('should have rejected');
      } catch (error: unknown) {
        assert.match((error as Error).message, /checksum and size/);
      }
      assert.deepEqual(readdirSync(dir), []);
    });

    it('rejects an HTTP error', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      try {
        await stageInstaller(dir, entry, serving(bytes, 503).fetch);
        assert.fail('should have rejected');
      } catch (error: unknown) {
        assert.match((error as Error).message, /HTTP 503/);
      }
      assert.deepEqual(readdirSync(dir), []);
    });

    it('gives up on a download that stalls, and leaves nothing behind', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      // a few bytes arrive, then nothing, ever
      const stalling: FetchLike = async () =>
        new Response(
          new ReadableStream<Uint8Array>({
            start(controller) {
              controller.enqueue(new Uint8Array(bytes.subarray(0, 5)));
            },
          }),
          { status: 200, headers: { 'content-length': String(bytes.length) } },
        );

      let rejection: Error | null = null;
      try {
        await stageInstaller(dir, entry, stalling, () => undefined, 50);
      } catch (error: unknown) {
        rejection = error as Error;
      }
      assert.include(rejection?.message, 'stalled');
      assert.deepEqual(readdirSync(dir), []);
    });

    it('gives up when the response itself never arrives', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      let aborted = false;
      const silent: FetchLike = async (_url, signal) =>
        new Promise((_resolve, reject) => {
          signal?.addEventListener('abort', () => {
            aborted = true;
            reject(signal.reason as Error);
          });
        });

      let rejection: Error | null = null;
      try {
        await stageInstaller(dir, entry, silent, () => undefined, 50);
      } catch (error: unknown) {
        rejection = error as Error;
      }
      assert.isTrue(aborted);
      assert.include(rejection?.message, 'stalled');
      assert.deepEqual(readdirSync(dir), []);
    });

    it('reuses an installer already staged and verified, without fetching', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      writeFileSync(path.join(dir, entry.filename), bytes);
      const served = serving(bytes);

      const file = await stageInstaller(dir, entry, served.fetch);
      assert.equal(file, path.join(dir, entry.filename));
      assert.equal(served.calls(), 0);
    });

    it('replaces a staged file that no longer verifies', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      writeFileSync(path.join(dir, entry.filename), 'truncated');
      const served = serving(bytes);

      await stageInstaller(dir, entry, served.fetch);
      assert.equal(served.calls(), 1);
      assert.deepEqual(readFileSync(path.join(dir, entry.filename)), bytes);
    });
  });

  describe('verifyInstaller', () => {
    it('is false for a missing, resized or altered file', async () => {
      const entry = artifact('RStudio-2026.12.0-daily-9.exe');
      const file = path.join(dir, entry.filename);
      assert.isFalse(await verifyInstaller(file, entry));
      writeFileSync(file, bytes.subarray(1));
      assert.isFalse(await verifyInstaller(file, entry));
      writeFileSync(file, Buffer.from('this stands in for an NSIS inst@ller'));
      assert.isFalse(await verifyInstaller(file, entry));
      writeFileSync(file, bytes);
      assert.isTrue(await verifyInstaller(file, entry));
    });
  });

  describe('pruneStagedInstallers', () => {
    it('removes installers no newer than the running build and abandoned partial downloads', async () => {
      const now = Date.now();
      const keep = ['RStudio-2026.12.0-daily-10.exe', 'RStudio-2026.12.0-daily-9.exe.4242.part', 'notes.txt'];
      const drop = [
        'RStudio-2026.12.0-daily-8.exe',
        'RStudio-2026.11.0-200.exe',
        'RStudio-2026.12.0-daily-7.exe.17.part',
      ];
      for (const name of [...keep, ...drop]) {
        writeFileSync(path.join(dir, name), 'x');
      }
      const old = new Date(now - kPartialDownloadTtlMs - 60_000);
      utimesSync(path.join(dir, 'RStudio-2026.12.0-daily-7.exe.17.part'), old, old);

      const removed = await pruneStagedInstallers(dir, '2026.12.0-daily+8', now);

      assert.sameMembers(
        removed.map((f) => path.basename(f)),
        drop,
      );
      for (const name of keep) {
        assert.isTrue(existsSync(path.join(dir, name)), `${name} should survive`);
      }
    });

    it('is a no-op for a directory that does not exist', async () => {
      assert.deepEqual(await pruneStagedInstallers(path.join(dir, 'nope'), '2026.12.0+1'), []);
    });
  });
});
