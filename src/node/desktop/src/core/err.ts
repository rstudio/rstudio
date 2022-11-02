/*
 * err.ts
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

/**
 * Type to return from non-throwing functions. Enables returning null
 * to represent "no error" aka Success. Per existing pattern in RStudio code,
 * we expect an "Err" result to be falsy if there is no error, and truthy if
 * there is an error.
 *
 * For example:
 *
 * const error = doSomething();
 * if (error) {
 *   // log the error, or whatever
 * }
 */
export type Err = Error | null;

/**
 * Convenience function for returning "no error" state from a function that
 * can return an Error.
 */
export function success(): null {
  return null;
}

export function isSuccessful(error: Err): boolean {
  return !error;
}

export function isFailure(error: Err): boolean {
  return !!error;
}

export function safeError(error: unknown): Error {
  if (error instanceof Error) {
    return error;
  } else {
    return new Error('unknown error');
  }
}
