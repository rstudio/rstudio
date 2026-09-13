/*
 * process-diagnostics.test.ts
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

import { assert } from 'chai';
import { app, Details, RenderProcessGoneDetails } from 'electron';
import { EventEmitter } from 'events';
import { describe } from 'mocha';
import sinon from 'sinon';

import { NullLogger, setLogger } from '../../../src/core/logger';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { registerProcessDiagnostics } from '../../../src/main/process-diagnostics';

describe('Process diagnostics', () => {
  let events: EventEmitter;
  let errorLog: sinon.SinonSpy;
  let debugLog: sinon.SinonSpy;

  beforeEach(() => {
    const log = new NullLogger();
    setLogger(log);
    errorLog = sinon.spy(log, 'logErrorMessage');
    debugLog = sinon.spy(log, 'logDebug');

    // Route registrations to a separate emitter so simulated process exits
    // cannot trigger Electron's or the test runner's own handlers.
    events = new EventEmitter();
    sinon.stub(app, 'on').callsFake((event, listener) => {
      events.on(event, listener);
      return app;
    });
    registerProcessDiagnostics();
  });

  afterEach(() => {
    sinon.restore();
    clearCoreSingleton();
  });

  function loggedDetails(log: sinon.SinonSpy, eventName: string): object {
    assert.isTrue(log.calledOnce);
    const message = log.firstCall.args[0] as string;
    const prefix = `Electron ${eventName}: `;
    assert.isTrue(message.startsWith(prefix));
    return JSON.parse(message.slice(prefix.length)) as object;
  }

  it('logs renderer failures at error level with the affected webContents id', () => {
    const details: RenderProcessGoneDetails = { reason: 'crashed', exitCode: -1073741819 };
    events.emit('render-process-gone', {}, { id: 17 }, details);

    assert.deepEqual(loggedDetails(errorLog, 'render-process-gone'), { ...details, webContentsId: 17 });
    assert.isTrue(debugLog.notCalled);
  });

  it('preserves the GPU process type and signed exit code', () => {
    const details: Details = { type: 'GPU', reason: 'crashed', exitCode: -2147483645 };
    events.emit('child-process-gone', {}, details);

    assert.deepEqual(loggedDetails(errorLog, 'child-process-gone'), details);
    assert.isTrue(debugLog.notCalled);
  });

  it('includes utility service names and reports launch failures even with exit code zero', () => {
    const details: Details = {
      type: 'Utility',
      reason: 'launch-failed',
      exitCode: 0,
      name: 'Network Service',
      serviceName: 'network.mojom.NetworkService',
    };
    events.emit('child-process-gone', {}, details);

    assert.deepEqual(loggedDetails(errorLog, 'child-process-gone'), details);
    assert.isTrue(debugLog.notCalled);
  });

  it('keeps clean renderer exits at debug level', () => {
    const details: RenderProcessGoneDetails = { reason: 'clean-exit', exitCode: 0 };
    events.emit('render-process-gone', {}, { id: 23 }, details);

    assert.deepEqual(loggedDetails(debugLog, 'render-process-gone'), { ...details, webContentsId: 23 });
    assert.isTrue(errorLog.notCalled);
  });

  it('keeps clean utility exits at debug level', () => {
    const details: Details = { type: 'Utility', reason: 'clean-exit', exitCode: 0 };
    events.emit('child-process-gone', {}, details);

    assert.deepEqual(loggedDetails(debugLog, 'child-process-gone'), details);
    assert.isTrue(errorLog.notCalled);
  });
});
