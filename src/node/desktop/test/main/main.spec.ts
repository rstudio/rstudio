/*
 * main.spec.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { expect } from 'chai';

import Main from '../../src/main/main';

interface Versions  {
  electron: string;
  rstudio: string;
  node: string;
  v8: string;
}

describe('Main', () => {
  describe('Static helpers', () => {
    it('getComponentVersions returns expected JSON', () => {
      const result = Main.getComponentVersions();
      const json: Versions = JSON.parse(result);
      expect(json.electron).length.is.greaterThan(0);
      expect(json.rstudio).length.is.greaterThan(0);
      expect(json.node).length.is.greaterThan(0);
      expect(json.v8).length.is.greaterThan(0);
    });
  });
});
