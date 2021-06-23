/*
 * application.test.ts
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
import { assert } from 'chai';

import { Application, kRunDiagnosticsOption } from '../../../src/main/application';

describe('Application', () => {
  describe('Command line switches', () => {
    it('run-diagnostics sets diag mode and continues', () => {
      const app = new Application();
      assert.isFalse(app.runDiagnostics);
      const argv = [kRunDiagnosticsOption];
      const result = app.initCommandLine(argv);
      assert.isFalse(result.exit);
      assert.isTrue(app.runDiagnostics);
    });
  });
  describe('Assorted helpers', () => {
    it('generates and stores port', () => {
      const app = new Application();
      const port = app.port;
      assert.isAbove(port, 0);
      assert.strictEqual(app.port, port);
    });
    it('generates new port', () => {
      const app = new Application();
      const origPort = app.port;
      app.generateNewPort();
      assert.notStrictEqual(origPort, app.port);
    });
  });
});
