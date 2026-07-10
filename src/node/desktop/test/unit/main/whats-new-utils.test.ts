/*
 * whats-new-utils.test.ts
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

import { describe, it, before, after } from 'mocha';
import { assert } from 'chai';
import { mkdtempSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join, resolve } from 'path';

import {
  DEV_RELEASE_NAME,
  toReleaseSlug,
  isValidSlug,
  isReleaseBuild,
  resolveReleaseName,
  resolveWhatsNewContentPath,
  createLocalUrlChecker,
  evaluateWhatsNewVisibility,
  WhatsNewVisibilityInputs,
} from '../../../src/main/whats-new-utils';

// Unix-style file URLs (no drive letter) are not valid Windows paths,
// so fileURLToPath throws on Windows. Skip the corresponding describes
// there without invoking their bodies; the Windows drive-letter cases
// are exercised in a dedicated describe.
function describePosix(title: string, fn: () => void): void {
  if (process.platform === 'win32') {
    describe.skip(title, () => { /* skipped on Windows */ });
  } else {
    describe(title, fn);
  }
}

describe('whats-new-utils', () => {
  describe('toReleaseSlug', () => {
    it('lowercases and replaces spaces with hyphens', () => {
      assert.equal(toReleaseSlug('Globemaster Allium'), 'globemaster-allium');
    });

    it('removes apostrophes before hyphenizing', () => {
      assert.equal(toReleaseSlug("King's Crown"), 'kings-crown');
    });

    it('replaces non-alphanumeric runs with a single hyphen', () => {
      assert.equal(toReleaseSlug('Sea Holly (v2)'), 'sea-holly-v2');
    });

    it('strips leading and trailing hyphens', () => {
      assert.equal(toReleaseSlug('  --Hello World--  '), 'hello-world');
    });

    it('collapses consecutive hyphens', () => {
      assert.equal(toReleaseSlug('foo---bar'), 'foo-bar');
    });

    it('returns empty string for empty input', () => {
      assert.equal(toReleaseSlug(''), '');
    });
  });

  describe('isValidSlug', () => {
    it('accepts lowercase alphanumeric with hyphens', () => {
      assert.isTrue(isValidSlug('globemaster-allium'));
    });

    it('rejects empty string', () => {
      assert.isFalse(isValidSlug(''));
    });

    it('rejects path traversal', () => {
      assert.isFalse(isValidSlug('../etc'));
    });

    it('rejects uppercase', () => {
      assert.isFalse(isValidSlug('Globemaster'));
    });

    it('rejects spaces', () => {
      assert.isFalse(isValidSlug('foo bar'));
    });
  });

  describe('isReleaseBuild', () => {
    it('returns true for release version', () => {
      assert.isTrue(isReleaseBuild('2026.04.0+123'));
    });

    it('returns false for dev version', () => {
      assert.isFalse(isReleaseBuild('9999.9.9-dev+999'));
    });

    it('returns false for daily version', () => {
      assert.isFalse(isReleaseBuild('2026.04.0-daily+123'));
    });

    it('returns false for hourly version', () => {
      assert.isFalse(isReleaseBuild('2026.04.0-hourly+456'));
    });

    it('returns false for preview version', () => {
      assert.isFalse(isReleaseBuild('2026.04.0-preview+789'));
    });

    it('returns false for version with no build metadata and prerelease', () => {
      assert.isFalse(isReleaseBuild('2026.04.0-dev'));
    });

    it('returns false for version with no build metadata at all', () => {
      // Malformed — not a well-formed release version without +BUILD
      assert.isFalse(isReleaseBuild('2026.04.0'));
    });

    it('returns true for version with only build metadata', () => {
      assert.isTrue(isReleaseBuild('2026.04.0+456'));
    });

    // Pro builds append .proN to the build metadata (e.g. +460.pro4)
    it('returns true for Pro release version', () => {
      assert.isTrue(isReleaseBuild('2026.04.0+460.pro4'));
    });

    it('returns false for Pro daily version', () => {
      assert.isFalse(isReleaseBuild('2026.04.0-daily+460.pro4'));
    });
  });

  describe('resolveReleaseName', () => {
    let repoRoot: string;

    before(() => {
      repoRoot = mkdtempSync(join(tmpdir(), 'whats-new-release-'));
      mkdirSync(join(repoRoot, 'version'));
      writeFileSync(join(repoRoot, 'version', 'RELEASE'), 'Pacific Dogwood\n');
    });

    after(() => {
      rmSync(repoRoot, { recursive: true, force: true });
    });

    it('reads version/RELEASE for a dev build', () => {
      assert.equal(resolveReleaseName(DEV_RELEASE_NAME, repoRoot), 'Pacific Dogwood');
    });

    it('returns the baked-in name for a packaged build (empty repo root)', () => {
      assert.equal(resolveReleaseName('Globemaster Allium', ''), 'Globemaster Allium');
    });

    it('does not override a real baked-in release name', () => {
      assert.equal(resolveReleaseName('Globemaster Allium', repoRoot), 'Globemaster Allium');
    });

    it('falls back to the placeholder when version/RELEASE is missing', () => {
      const emptyRoot = mkdtempSync(join(tmpdir(), 'whats-new-empty-'));
      try {
        assert.equal(resolveReleaseName(DEV_RELEASE_NAME, emptyRoot), DEV_RELEASE_NAME);
      } finally {
        rmSync(emptyRoot, { recursive: true, force: true });
      }
    });

    it('falls back to the placeholder when version/RELEASE is blank', () => {
      const blankRoot = mkdtempSync(join(tmpdir(), 'whats-new-blank-'));
      try {
        mkdirSync(join(blankRoot, 'version'));
        writeFileSync(join(blankRoot, 'version', 'RELEASE'), '   \n');
        assert.equal(resolveReleaseName(DEV_RELEASE_NAME, blankRoot), DEV_RELEASE_NAME);
      } finally {
        rmSync(blankRoot, { recursive: true, force: true });
      }
    });
  });

  describe('resolveWhatsNewContentPath', () => {
    it('returns null for a non-existent slug', () => {
      assert.isNull(resolveWhatsNewContentPath('does-not-exist'));
    });

    it('returns null for an invalid slug (path traversal)', () => {
      assert.isNull(resolveWhatsNewContentPath('../etc'));
    });
  });

  describePosix('createLocalUrlChecker (file mode)', () => {
    const host = 'file:///app/.webpack/renderer/whats_new/index.html';
    const isLocal = createLocalUrlChecker(host, 'globemaster-allium');

    it('allows the host page directory', () => {
      assert.isTrue(isLocal('file:///app/.webpack/renderer/whats_new/index.js'));
    });

    it('allows the release content subtree', () => {
      assert.isTrue(isLocal(
        'file:///app/.webpack/renderer/assets/whats-new/globemaster-allium/index.html',
      ));
    });

    it('rejects arbitrary file:// paths', () => {
      assert.isFalse(isLocal('file:///etc/passwd'));
    });

    it('rejects file:// paths outside the content subtree', () => {
      assert.isFalse(isLocal(
        'file:///app/.webpack/renderer/assets/whats-new/other-release/index.html',
      ));
    });

    it('rejects sibling directory sharing the slug prefix', () => {
      assert.isFalse(isLocal(
        'file:///app/.webpack/renderer/assets/whats-new/globemaster-allium-old/index.html',
      ));
    });

    it('rejects sibling directory sharing the host dir prefix', () => {
      assert.isFalse(isLocal('file:///app/.webpack/renderer/whats_new_evil/payload.html'));
    });

    it('rejects http URLs in file mode', () => {
      assert.isFalse(isLocal('https://evil.example.com'));
    });

    it('rejects invalid URLs', () => {
      assert.isFalse(isLocal('not a url'));
    });
  });

  describePosix('createLocalUrlChecker (invalid slug)', () => {
    const host = 'file:///app/.webpack/renderer/whats_new/index.html';
    const isLocal = createLocalUrlChecker(host, '../etc');

    it('still allows the host page directory', () => {
      assert.isTrue(isLocal('file:///app/.webpack/renderer/whats_new/index.js'));
    });

    it('rejects arbitrary file:// paths', () => {
      assert.isFalse(isLocal('file:///etc/passwd'));
    });

    it('rejects content subtree paths since slug is invalid', () => {
      assert.isFalse(isLocal(
        'file:///app/.webpack/renderer/assets/whats-new/globemaster-allium/index.html',
      ));
    });
  });

  describe('createLocalUrlChecker (dev mode)', () => {
    const host = 'http://localhost:3000/whats_new/index.html';
    const isLocal = createLocalUrlChecker(host, 'globemaster-allium');

    it('allows same-origin URLs', () => {
      assert.isTrue(isLocal('http://localhost:3000/assets/whats-new/foo/index.html'));
    });

    it('rejects different origin', () => {
      assert.isFalse(isLocal('http://localhost:4000/foo'));
    });

    it('rejects external URLs', () => {
      assert.isFalse(isLocal('https://evil.example.com'));
    });
  });

  describe('createLocalUrlChecker (Windows drive-letter URLs)', () => {
    const host = 'file:///C:/Program%20Files/RStudio/.webpack/renderer/whats_new/index.html';
    const isLocal = createLocalUrlChecker(host, 'globemaster-allium');

    it('allows files in the host page directory', () => {
      assert.isTrue(isLocal(
        'file:///C:/Program%20Files/RStudio/.webpack/renderer/whats_new/index.js',
      ));
    });

    it('allows files in the release content subtree', () => {
      assert.isTrue(isLocal(
        'file:///C:/Program%20Files/RStudio/.webpack/renderer/assets/whats-new/globemaster-allium/index.html',
      ));
    });

    it('rejects paths outside the allowed tree', () => {
      assert.isFalse(isLocal('file:///C:/Windows/System32/config/SAM'));
    });

    it('rejects sibling directory sharing the slug prefix', () => {
      assert.isFalse(isLocal(
        'file:///C:/Program%20Files/RStudio/.webpack/renderer/assets/whats-new/globemaster-allium-old/index.html',
      ));
    });
  });

  describe('whats-new content validation', () => {
    const assetsDir = resolve(__dirname, '..', '..', '..', 'src', 'assets', 'whats-new');
    const expectedCsp = 'Content-Security-Policy';
    const entries = readdirSync(assetsDir, { withFileTypes: true })
      .filter((e) => e.isDirectory());

    for (const entry of entries) {
      it(`${entry.name}/index.html includes CSP meta tag`, () => {
        const indexPath = join(assetsDir, entry.name, 'index.html');
        const content = readFileSync(indexPath, 'utf-8');
        assert.include(content, expectedCsp,
          `${entry.name}/index.html is missing the Content-Security-Policy meta tag`);
      });

      it(`${entry.name}/index.html includes base stylesheet`, () => {
        const indexPath = join(assetsDir, entry.name, 'index.html');
        const content = readFileSync(indexPath, 'utf-8');
        assert.include(content, 'whats-new-base.css',
          `${entry.name}/index.html is missing the base stylesheet link`);
      });
    }
  });

  describe('evaluateWhatsNewVisibility', () => {
    // A baseline where every gate says "show"; individual tests flip one field.
    const shows: WhatsNewVisibilityInputs = {
      contentAvailable: true,
      forceShow: false,
      forceDisable: false,
      userPrefEnabled: true,
      isRelease: true,
      alreadySeen: false,
    };

    it('shows when all conditions are met', () => {
      assert.isTrue(evaluateWhatsNewVisibility(shows));
    });

    it('never shows when content is unavailable, even if force-shown', () => {
      assert.isFalse(evaluateWhatsNewVisibility({ ...shows, contentAvailable: false }));
      assert.isFalse(
        evaluateWhatsNewVisibility({ ...shows, contentAvailable: false, forceShow: true }),
      );
    });

    it('force-show overrides the user preference, build type, and seen state', () => {
      assert.isTrue(
        evaluateWhatsNewVisibility({
          ...shows,
          forceShow: true,
          userPrefEnabled: false,
          isRelease: false,
          alreadySeen: true,
        }),
      );
    });

    it('force-show takes precedence over force-disable', () => {
      assert.isTrue(evaluateWhatsNewVisibility({ ...shows, forceShow: true, forceDisable: true }));
    });

    it('force-disable overrides an enabled preference on an unseen release', () => {
      assert.isFalse(evaluateWhatsNewVisibility({ ...shows, forceDisable: true }));
    });

    it('does not show when the user preference is disabled', () => {
      assert.isFalse(evaluateWhatsNewVisibility({ ...shows, userPrefEnabled: false }));
    });

    it('does not show on non-release builds', () => {
      assert.isFalse(evaluateWhatsNewVisibility({ ...shows, isRelease: false }));
    });

    it('does not show a release that has already been seen', () => {
      assert.isFalse(evaluateWhatsNewVisibility({ ...shows, alreadySeen: true }));
    });
  });
});
