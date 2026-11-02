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
import { parseRelaunchProject } from '../../../src/main/relaunch-project';

describe('relaunch project marker', () => {
  const now = 1_700_000_000_000;

  it('returns the project while the marker is fresh', () => {
    const text = JSON.stringify({ project: '/home/me/proj/proj.Rproj', expires: now + 1000 });
    assert.equal(parseRelaunchProject(text, now), '/home/me/proj/proj.Rproj');
    assert.equal(parseRelaunchProject(JSON.stringify({ project: 'none', expires: now }), now), 'none');
  });

  it('is null once expired', () => {
    const text = JSON.stringify({ project: '/home/me/proj/proj.Rproj', expires: now - 1 });
    assert.isNull(parseRelaunchProject(text, now));
  });

  it('is null for malformed markers', () => {
    assert.isNull(parseRelaunchProject('', now));
    assert.isNull(parseRelaunchProject('not json', now));
    assert.isNull(parseRelaunchProject(JSON.stringify({ project: '', expires: now + 1 }), now));
    assert.isNull(parseRelaunchProject(JSON.stringify({ project: '/p.Rproj' }), now));
    assert.isNull(parseRelaunchProject(JSON.stringify({ project: 42, expires: now + 1 }), now));
  });
});
