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
  branchSlug,
  channelForVersion,
  compareVersions,
  isUpdateAvailable,
  kDefaultReleaseNotesUrl,
  parseBuildIndex,
  parseReleaseIndex,
  parseVersion,
  platformKeys,
  squirrelFeedDocument,
  updateIndexUrl,
} from '../../../src/main/update-feed';

// shapes taken from https://dailies.rstudio.com/api/v1/openapi.json
const releaseIndex = {
  $schema: 'https://dailies.rstudio.com/api/v1/schema/release.json',
  released: [
    {
      version: '2026.10',
      name: 'Blue Mistflower',
      branch: 'blue-mistflower',
      published: [
        {
          build: '2026.10.1+239.pro3',
          builds_url: 'https://dailies.rstudio.com/api/v1/build/2026.10.1+239.pro3/index.json',
          notes_url: 'https://docs.posit.co/ide/news/#rstudio-2026.10.1',
        },
        {
          build: '2026.10.0+233.pro1',
          builds_url: 'https://dailies.rstudio.com/api/v1/build/2026.10.0+233.pro1/index.json',
        },
      ],
    },
  ],
  in_development: [],
  upcoming: [],
};

const macos = {
  name: 'macOS 15+',
  architecture: 'universal',
  link: 'https://dl.dailies.rstudio.com/electron/macos/RStudio-2026.10.1-239.dmg',
  filename: 'RStudio-2026.10.1-239.dmg',
  sha256: '20C3B5978E93004E10807A273ED2E98DAFF2C44710D43C44E70D17761147F75D',
  channel: 'Release',
  version: '2026.10.1+239',
  size: 966474254,
  commit: '0b06695c24e10f6de6b9166c35ae24804c21f95f',
  date: '2026-10-23T21:18:44Z',
};

const buildIndex = {
  build: '2026.10.1+239.pro3',
  oss_build: '2026.10.1+239',
  products: {
    electron: {
      name: 'RStudio Desktop',
      platforms: {
        macos,
        'macos-zip': { ...macos, link: macos.link.replace('.dmg', '.zip'), filename: 'RStudio-2026.10.1-239.zip' },
        windows: { ...macos, link: 'https://dl.dailies.rstudio.com/electron/windows/RStudio-2026.10.1-239.exe' },
      },
    },
    server: { name: 'RStudio Server', platforms: {} },
  },
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

  describe('branchSlug', () => {
    it('slugs the codename the way the dailies site does', () => {
      assert.equal(branchSlug('Parrot Balsam'), 'parrot-balsam');
      assert.equal(branchSlug('  Blue Mistflower\n'), 'blue-mistflower');
    });
  });

  describe('updateIndexUrl', () => {
    const base = 'https://dailies.rstudio.com';

    it('uses the release API for releases and branch indexes otherwise', () => {
      assert.equal(updateIndexUrl(base, 'release', 'blue-mistflower'), `${base}/api/v1/release/index.json`);
      assert.equal(updateIndexUrl(base, 'daily', 'parrot-balsam'), `${base}/rstudio/parrot-balsam/index.json`);
      assert.equal(updateIndexUrl(base, 'preview', 'montauk-daisy'), `${base}/rstudio/montauk-daisy/index.json`);
      assert.equal(updateIndexUrl(base, 'hourly', 'parrot-balsam'), `${base}/rstudio-hourly/parrot-balsam/index.json`);
    });

    it('tolerates a trailing slash and refuses the dev channel', () => {
      assert.equal(updateIndexUrl(`${base}/`, 'release', 'x'), `${base}/api/v1/release/index.json`);
      assert.isNull(updateIndexUrl(base, 'dev', 'parrot-balsam'));
    });
  });

  describe('platformKeys', () => {
    it('names the installer and the install artifact per platform', () => {
      assert.deepEqual(platformKeys('darwin', 'arm64'), { download: 'macos', install: 'macos-zip' });
      assert.deepEqual(platformKeys('darwin', 'x64'), { download: 'macos', install: 'macos-zip' });
      assert.deepEqual(platformKeys('win32', 'x64'), { download: 'windows', install: 'windows' });
      assert.deepEqual(platformKeys('win32', 'arm64'), { download: 'windows-arm64', install: 'windows-arm64' });
      assert.isNull(platformKeys('linux', 'x64'));
    });
  });

  describe('parseReleaseIndex', () => {
    it('picks the current release and its newest published build', () => {
      assert.deepEqual(parseReleaseIndex(JSON.stringify(releaseIndex)), {
        buildsUrl: 'https://dailies.rstudio.com/api/v1/build/2026.10.1+239.pro3/index.json',
        notesUrl: 'https://docs.posit.co/ide/news/#rstudio-2026.10.1',
      });
    });

    it('rejects an index with nothing published', () => {
      assert.throws(() => parseReleaseIndex(JSON.stringify({ released: [] })), /no released versions/);
      assert.throws(() => parseReleaseIndex(JSON.stringify({ released: [{ published: [] }] })), /no published builds/);
      assert.throws(
        () => parseReleaseIndex(JSON.stringify({ released: [{ published: [{ builds_url: 'http://x' }] }] })),
        /https builds_url/,
      );
      assert.throws(() => parseReleaseIndex('[]'), /not a JSON object/);
    });
  });

  describe('parseBuildIndex', () => {
    it('reads one platform entry and lower-cases the checksum', () => {
      const entry = parseBuildIndex(JSON.stringify(buildIndex), 'macos-zip');
      assert.deepEqual(entry, {
        version: '2026.10.1+239',
        url: 'https://dl.dailies.rstudio.com/electron/macos/RStudio-2026.10.1-239.zip',
        filename: 'RStudio-2026.10.1-239.zip',
        sha256: macos.sha256.toLowerCase(),
        size: macos.size,
        commit: macos.commit,
        date: macos.date,
      });
    });

    it('is null when the platform (or product) is absent', () => {
      assert.isNull(parseBuildIndex(JSON.stringify(buildIndex), 'windows-arm64'));
      assert.isNull(parseBuildIndex(JSON.stringify({ products: { server: {} } }), 'macos'));
      assert.isNull(parseBuildIndex('{}', 'macos'));
    });

    it('derives the filename from the link when absent', () => {
      const { filename: _unused, ...noFilename } = macos;
      const doc = { products: { electron: { platforms: { macos: noFilename } } } };
      assert.equal(parseBuildIndex(JSON.stringify(doc), 'macos')?.filename, 'RStudio-2026.10.1-239.dmg');
    });

    it('rejects malformed entries', () => {
      const withMacos = (overrides: Record<string, unknown>) =>
        JSON.stringify({ products: { electron: { platforms: { macos: { ...macos, ...overrides } } } } });
      assert.throws(() => parseBuildIndex(withMacos({ version: undefined }), 'macos'), /missing "version"/);
      assert.throws(() => parseBuildIndex(withMacos({ version: 'latest' }), 'macos'), /unrecognized version/);
      assert.throws(() => parseBuildIndex(withMacos({ link: 'http://example.com/x.dmg' }), 'macos'), /https/);
      assert.throws(() => parseBuildIndex(withMacos({ sha256: 'abc' }), 'macos'), /sha256/);
      assert.throws(() => parseBuildIndex('not json', 'macos'), SyntaxError);
    });
  });

  describe('squirrelFeedDocument', () => {
    const artifact = parseBuildIndex(JSON.stringify(buildIndex), 'macos-zip');

    it('emits the Squirrel.Mac default server shape', () => {
      assert.isNotNull(artifact);
      const doc = JSON.parse(squirrelFeedDocument(artifact!, 'https://docs.posit.co/ide/news/#rstudio-2026.10.1'));
      assert.deepEqual(doc, {
        url: 'https://dl.dailies.rstudio.com/electron/macos/RStudio-2026.10.1-239.zip',
        name: '2026.10.1+239',
        notes: 'https://docs.posit.co/ide/news/#rstudio-2026.10.1',
        pub_date: macos.date,
        sha256: macos.sha256.toLowerCase(),
        size: macos.size,
      });
    });

    it('fills in notes and a date when the API has none', () => {
      const doc = JSON.parse(squirrelFeedDocument({ ...artifact!, date: '' }, ''));
      assert.equal(doc.notes, kDefaultReleaseNotesUrl);
      assert.match(doc.pub_date, /^\d{4}-\d{2}-\d{2}T/);
    });
  });

  describe('isUpdateAvailable', () => {
    it('is true only for a strictly newer version', () => {
      assert.isTrue(isUpdateAvailable('2026.10.0+100', '2026.10.1+239'));
      assert.isFalse(isUpdateAvailable('2026.10.1+239', '2026.10.1+239'));
      assert.isFalse(isUpdateAvailable('2026.11.0+1', '2026.10.1+239'));
    });
  });
});
