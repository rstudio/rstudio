/*
 * program-status.ts
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

import { app } from 'electron';

export const EXIT_SUCCESS = 0;
export const EXIT_FAILURE = 1;

export interface ProgramStatus {
  exit: boolean;
  exitCode: number;
}

export function run(): ProgramStatus {
  return { exit: false, exitCode: EXIT_SUCCESS };
}

export function exitSuccess(): ProgramStatus {
  return { exit: true, exitCode: EXIT_SUCCESS };
}

export function exitFailure(): ProgramStatus {
  return { exit: true, exitCode: EXIT_FAILURE };
}

/**
 * Operate on a ProgramStatus result, exiting if requested.
 *
 * @param result result to check
 * @returns true if app should continue
 */
export function parseStatus(result: ProgramStatus): boolean {
  if (result.exit) {
    app.exit(result.exitCode);
    return false;
  }
  return true;
}
