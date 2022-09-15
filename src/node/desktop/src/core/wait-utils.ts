/*
 * wait-utils.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import { Err, success } from './err';
import util from 'util';

export const setTimeoutPromise = util.promisify(setTimeout);

export type WaitResultType = 'WaitSuccess' | 'WaitContinue' | 'WaitError';

export class WaitResult {
  type: WaitResultType;
  error: Err;

  constructor(type: WaitResultType, error: Err = null) {
    this.type = type;
    this.error = error;
  }
}

export type WaitTimeoutFn = () => Promise<WaitResult>;

/**
 * Helper routine
 * @param waitFunction Function invoked on each retry
 * @param initialWaitMs Delay first first try
 * @param incrementWaitMs Delay between subsequent tries
 * @param maxWaitSec Maximum time to keep trying
 * @returns
 */
export async function waitWithTimeout(
  waitFunction: WaitTimeoutFn,
  initialWaitMs: number,
  incrementWaitMs: number,
  maxWaitSec: number,
): Promise<Err> {
  // wait for the session to be available

  // wait 30ms (this value is somewhat arbitrary -- ideally it would
  // be as close as possible to the expected total launch time of
  // rsession (observed to be ~35ms on a 3ghz MacPro w/ SSD disk)
  await setTimeoutPromise(initialWaitMs);

  // try the connection again and if we don't get it try to reconnect
  // every 10ms until a timeout occurs -- we use a low granularity
  // here because expect our initial guess of 30ms to be pretty close
  // to the total launch time
  const timeoutTime = Date.now() + maxWaitSec * 1000;
  while (Date.now() < timeoutTime) {
    const result = await waitFunction();
    if (result.type === 'WaitSuccess') {
      return success();
    }

    if (result.type === 'WaitContinue') {
      // try again after waiting a short while
      await setTimeoutPromise(incrementWaitMs);
      continue;
    } else {
      // unrecoverable error (return connectError below)
      return result.error;
    }
  }

  return Error('Exceeded timeout');
}
