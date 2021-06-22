/*
 * unit-utils.ts
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

/**
 * save and clear specific env vars
 */
export function saveAndClear(vars: Record<string, string>): void {
  for (const name in vars) {
    vars[name] = process.env[name] ?? '';
    delete process.env[name];
  }
}

/**
 * put back original env vars
 */
export function restore(vars: Record<string, string>): void {
  for (const name in vars) {
    if (vars[name]) {
      process.env[name] = vars[name];
      vars[name] = '';
    } else {
      delete process.env[name];
    }
  }
}