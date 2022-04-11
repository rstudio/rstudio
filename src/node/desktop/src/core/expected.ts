/*
 * expected.ts
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

export type Expected<T> = [T, Error | null];

export function ok<T>(value: T): Expected<T> {
  return [value, null];
}

// NOTE: we intentionally lie to the type checker here; the intention is that
// users of Expected should only use the resulting value T if error is null or
// unset. Effectively, this implies that "correct" usages of Expected will never
// give the user a non-T value.
export function err<T>(error?: Error): Expected<T> {
  return [null as unknown as T, error ?? new Error(error)];
}

