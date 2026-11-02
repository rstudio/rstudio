/*
 * update-feed.test.ts
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
import {
  channelForVersion,
  compareVersions,
  isUpdateAvailable,
  parseReleaseFeed,
  parseVersion,
  updateFeedUrl,
} from '../../../src/main/update-feed';

const feed = {
  version: '2026.10.1+239',
  name: 'RStudio 2026.10.1+239',
  notes: 'https://docs.posit.co/ide/news/',
  pub_date: '2026-10-20T18:51:38Z',
  url: 'https://download1.rstudio.org/electron/macos/RStudio-2026.10.1-239.zip',
  sha256: 'C5F52736D13ADA47E95412D2822DDCEC3D26560E73D3616F902122F6BD9E748C',
  size: 123456789,
  commit: '5dfc92424b4381ca7139f3f0a122a65abc2a9383',
};

describe('update feed', () => {
  describe('parseVersion', () => {
    it('parses release, tagged, and pro versions', () => {
      assert.deepEqual(parseVersion('2026.10.1+239'), { major: 2026, minor: 10, patch: 1, tag: '', build: 239 });
      assert.deepEqual(parseVersion('2026.12.0-daily+118'), {
        major: 2026,
        minor: 12,
        patch: 0,
        tag: 'daily',
        build: 118,
      });
      assert.deepEqual(parseVersion('2026.10.1+239.pro3'), { major: 2026, minor: 10, patch: 1, tag: '', build: 239 });
      assert.deepEqual(parseVersion('9999.9.9-dev+999'), { major: 9999, minor: 9, patch: 9, tag: 'dev', build: 999 });
    });

    it('rejects strings that are not versions', () => {
      assert.isNull(parseVersion(''));
      assert.isNull(parseVersion('latest'));
      assert.isNull(parseVersion('2026.10'));
    });
  });

  describe('compareVersions', () => {
    it('orders by major, minor, patch, then build', () => {
      assert.isBelow(compareVersions('2026.10.1+239', '2026.10.2+100'), 0);
      assert.isBelow(compareVersions('2026.09.9+999', '2026.10.0+1'), 0);
      assert.isAbove(compareVersions('2027.01.0+1', '2026.12.0+999'), 0);
      assert.equal(compareVersions('2026.10.1+239', '2026.10.1+239'), 0);
    });

    it('distinguishes builds that differ only in build number', () => {
      // semver would treat these as equal (build metadata), which is exactly
      // what dailies need to avoid
      assert.isBelow(compareVersions('2026.12.0-daily+117', '2026.12.0-daily+118'), 0);
    });

    it('ignores the pre-release tag', () => {
      assert.equal(compareVersions('2026.12.0-daily+118', '2026.12.0+118'), 0);
    });

    it('ranks unparseable versions lowest', () => {
      assert.isBelow(compareVersions('garbage', '2026.10.1+239'), 0);
      assert.equal(compareVersions('garbage', 'also garbage'), 0);
    });
  });

  describe('channelForVersion', () => {
    it('maps the tag to a channel', () => {
      assert.equal(channelForVersion('2026.10.1+239'), 'release');
      assert.equal(channelForVersion('2026.10.0-preview+120'), 'preview');
      assert.equal(channelForVersion('2026.12.0-daily+118'), 'daily');
      assert.equal(channelForVersion('2026.12.0-hourly+118'), 'hourly');
    });

    it('treats dev and unknown builds as dev', () => {
      assert.equal(channelForVersion('9999.9.9-dev+999'), 'dev');
      assert.equal(channelForVersion('2026.12.0-nightly+1'), 'dev');
      assert.equal(channelForVersion('not a version'), 'dev');
    });
  });

  describe('updateFeedUrl', () => {
    const base = 'https://cdn.posit.co/rstudio/updates';

    it('builds per-channel, per-platform feed URLs', () => {
      assert.equal(updateFeedUrl(base, 'release', 'darwin', 'arm64'), `${base}/release/macos/universal/releases.json`);
      assert.equal(updateFeedUrl(base, 'release', 'darwin', 'x64'), `${base}/release/macos/universal/releases.json`);
      assert.equal(updateFeedUrl(base, 'daily', 'win32', 'x64'), `${base}/daily/windows/x86_64/releases.json`);
      assert.equal(updateFeedUrl(base, 'daily', 'win32', 'arm64'), `${base}/daily/windows/arm64/releases.json`);
    });

    it('tolerates a trailing slash on the base URL', () => {
      assert.equal(
        updateFeedUrl(`${base}/`, 'release', 'darwin', 'arm64'),
        `${base}/release/macos/universal/releases.json`,
      );
    });

    it('has no feed for Linux', () => {
      assert.isNull(updateFeedUrl(base, 'release', 'linux', 'x64'));
    });
  });

  describe('parseReleaseFeed', () => {
    it('parses a complete feed and lower-cases the checksum', () => {
      const entry = parseReleaseFeed(JSON.stringify(feed));
      assert.equal(entry.version, feed.version);
      assert.equal(entry.name, feed.name);
      assert.equal(entry.notes, feed.notes);
      assert.equal(entry.pubDate, feed.pub_date);
      assert.equal(entry.url, feed.url);
      assert.equal(entry.sha256, feed.sha256.toLowerCase());
      assert.equal(entry.size, feed.size);
      assert.equal(entry.commit, feed.commit);
    });

    it('fills optional fields with defaults', () => {
      const entry = parseReleaseFeed(JSON.stringify({ version: feed.version, url: feed.url, sha256: feed.sha256 }));
      assert.equal(entry.name, feed.version);
      assert.equal(entry.notes, '');
      assert.equal(entry.pubDate, '');
      assert.equal(entry.size, 0);
      assert.equal(entry.commit, '');
    });

    it('rejects malformed feeds', () => {
      assert.throws(() => parseReleaseFeed('not json'), SyntaxError);
      assert.throws(() => parseReleaseFeed('[]'), /not a JSON object/);
      assert.throws(() => parseReleaseFeed(JSON.stringify({ ...feed, version: undefined })), /missing "version"/);
      assert.throws(() => parseReleaseFeed(JSON.stringify({ ...feed, version: 'latest' })), /unrecognized version/);
      assert.throws(() => parseReleaseFeed(JSON.stringify({ ...feed, url: 'http://example.com/x.zip' })), /https/);
      assert.throws(() => parseReleaseFeed(JSON.stringify({ ...feed, sha256: 'abc' })), /sha256/);
    });
  });

  describe('isUpdateAvailable', () => {
    const entry = parseReleaseFeed(JSON.stringify(feed));

    it('is true only for a strictly newer feed version', () => {
      assert.isTrue(isUpdateAvailable('2026.10.0+100', entry));
      assert.isFalse(isUpdateAvailable('2026.10.1+239', entry));
      assert.isFalse(isUpdateAvailable('2026.11.0+1', entry));
    });
  });
});
