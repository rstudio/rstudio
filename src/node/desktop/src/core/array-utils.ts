/*
 * array-utils.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

/**
 * Return next-highest value from an array of choices
 * @param val Current value
 * @param choices Array of allowed values
 * @returns Next highest value, or the highest choice if no higher values
 */
export function nextHighest(val: number, choices: number[]): number {
  if (choices.length === 0) {
    return val;
  }
  let nextVal = choices.find((x) => x > val);
  if (!nextVal) {
    nextVal = choices.slice(-1)[0];
  }
  return nextVal;
}

/**
 * Return next-lowest value from an array of choices
 * @param val Current value
 * @param choices Array of allowed values
 * @returns Next lowest value, or the lowest choice if no lower values
 */
export function nextLowest(val: number, choices: number[]): number {
  if (choices.length === 0) {
    return val;
  }
  let index = choices.findIndex((x) => x >= val);
  index = index <= 0 ? 0 : --index;
  return choices[index];
}

/**
 * @param strArray Array to search
 * @param prefix String prefix to match
 * @returns Value of first array member starting with `prefix` (with `prefix` removed) or
 * empty string if no matches.
 */
export function firstStartingWith(strArray: string[], prefix: string): string {
  for (const item of strArray) {
    if (item.startsWith(prefix)) {
      return item.slice(prefix.length);
    }
  }
  return '';
}
