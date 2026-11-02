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
import {
  installationKey,
  markerExpired,
  markerMatches,
  parseRelaunchMarker,
  RelaunchMarker,
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

  it('keys an installation by its directory, case-insensitively', () => {
    const app = '/Applications/RStudio.app/Contents/MacOS/RStudio';
    assert.equal(installationKey(app), installationKey(app.toLowerCase()));
    assert.notEqual(installationKey(app), installationKey('/tmp/RStudio.app/Contents/MacOS/RStudio'));
  });
});
