/*
 * core-state.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import { LogOptions, NullLogger } from './logger';

/**
 * Global singleton containing state for 'core' routines
 */
export interface CoreState {
  logOptions: LogOptions;
  instance: number; // for unit-testing
}

let core: CoreState | null = null;
let coreStateInstanceCounter = 0;

/**
 * @returns Global core state
 */
export function coreState(): CoreState {
  if (!core) {
    core = new CoreStateImpl();
  }
  return core;
}

/**
 * Clear core singleton; intended for unit tests only
 */
export function clearCoreSingleton(): void {
  core = null;
}

class CoreStateImpl implements CoreState {
  logOptions: LogOptions;
  instance: number;

  constructor() {
    this.instance = coreStateInstanceCounter++;
    this.logOptions = { logger: new NullLogger(), showDiagnostics: false };
  }
}
