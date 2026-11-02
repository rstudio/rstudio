/*
 * relaunch-project.test.ts
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
import { existsSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'fs';
import os from 'os';
import path from 'path';
import {
  forgetRelaunchProject,
  installationKey,
  markerExpired,
  markerMatches,
  parseRelaunchMarker,
  RelaunchMarker,
  rememberRelaunchProject,
  takeRelaunchProject,
} from '../../../src/main/relaunch-project';

describe('relaunch project marker', () => {
  const now = 1_700_000_000_000;

  // posix paths throughout: path.dirname() only splits on the running
  // platform's separator, and these tests run on every platform
  const identity = { version: '2026.12.0-daily+9', installKey: installationKey('/opt/rstudio/bin/rstudio') };
  const marker: RelaunchMarker = { ...identity, project: '/home/me/proj/proj.Rproj', expires: now + 1000 };

  it('round-trips through JSON', () => {
    assert.deepEqual(parseRelaunchMarker(JSON.stringify(marker)), marker);
  });

  it('rejects malformed markers', () => {
    assert.isNull(parseRelaunchMarker(''));
    assert.isNull(parseRelaunchMarker('not json'));
    assert.isNull(parseRelaunchMarker(JSON.stringify({ ...marker, project: '' })));
    assert.isNull(parseRelaunchMarker(JSON.stringify({ ...marker, expires: 'soon' })));
    assert.isNull(parseRelaunchMarker(JSON.stringify({ project: marker.project, expires: now })));
  });

  it('expires', () => {
    assert.isFalse(markerExpired(marker, now));
    assert.isTrue(markerExpired(marker, now + 1001));
  });

  it('is only for the build that was installed, in the same place', () => {
    assert.isTrue(markerMatches(marker, identity));
    // the old build still running (or launched again) before the relaunch
    assert.isFalse(markerMatches(marker, { ...identity, version: '2026.12.0-daily+8' }));
    // another installation of the same build, e.g. an unpacked zip
    assert.isFalse(markerMatches(marker, { ...identity, installKey: installationKey('/tmp/rstudio/bin/rstudio') }));
  });

  describe('remember / take', () => {
    let dir: string;
    beforeEach(() => {
      dir = mkdtempSync(path.join(os.tmpdir(), 'rstudio-relaunch-test-'));
    });
    afterEach(() => {
      rmSync(dir, { recursive: true, force: true });
    });

    it('hands the project to exactly one consumer', () => {
      rememberRelaunchProject(marker.project, identity, dir);
      // two launches of the updated build racing for the same marker
      assert.equal(takeRelaunchProject(identity, now, dir), marker.project);
      assert.isNull(takeRelaunchProject(identity, now, dir));
      assert.deepEqual(readdirSync(dir), []);
    });

    it('leaves a marker meant for another build or installation in place', () => {
      rememberRelaunchProject(marker.project, identity, dir);
      assert.isNull(takeRelaunchProject({ ...identity, version: '2026.12.0-daily+8' }, now, dir));
      const other = { ...identity, installKey: installationKey('/tmp/rstudio/bin/rstudio') };
      assert.isNull(takeRelaunchProject(other, now, dir));
      assert.equal(readdirSync(dir).length, 1);
      // and then the intended reader still gets it
      assert.equal(takeRelaunchProject(identity, now, dir), marker.project);
    });

    it('removes an expired or malformed marker without reopening anything', () => {
      writeFileSync(path.join(dir, `${identity.installKey}.json`), JSON.stringify({ ...marker, expires: now - 1 }));
      assert.isNull(takeRelaunchProject(identity, now, dir));
      assert.deepEqual(readdirSync(dir), []);
      writeFileSync(path.join(dir, `${identity.installKey}.json`), 'garbage');
      assert.isNull(takeRelaunchProject(identity, now, dir));
      assert.deepEqual(readdirSync(dir), []);
    });

    it('keeps one marker per installation, and forgets only its own', () => {
      const other = { ...identity, installKey: installationKey('/tmp/rstudio/bin/rstudio') };
      rememberRelaunchProject('/a/a.Rproj', identity, dir);
      rememberRelaunchProject('/b/b.Rproj', other, dir);
      forgetRelaunchProject(other.installKey, dir);
      forgetRelaunchProject(other.installKey, dir); // idempotent
      assert.isFalse(existsSync(path.join(dir, `${other.installKey}.json`)));
      assert.equal(takeRelaunchProject(identity, now, dir), '/a/a.Rproj');
    });
  });

  it('keys an installation by its directory, case-insensitively', () => {
    const app = '/Applications/RStudio.app/Contents/MacOS/RStudio';
    assert.equal(installationKey(app), installationKey(app.toLowerCase()));
    assert.notEqual(installationKey(app), installationKey('/tmp/RStudio.app/Contents/MacOS/RStudio'));
  });
});
