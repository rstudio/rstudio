/*
 * function.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

// Throttle function execution - see below for more information for things like scroll events
// https://developer.mozilla.org/en-US/docs/Web/API/Document/scroll_event


// TODO: My brain is broken what is the right way to think about this?
let timeout: number;
  export function debounce<F extends (...args: any[]) => any>(originalFunction: F, waitMilliseconds: number) : (...args: any[]) => void {
  const debounced = (...args: any[]) => {
    if (timeout) {
      window.clearTimeout(timeout);
    }
    timeout = window.setTimeout(() => {
      originalFunction(...args);
  
    }, waitMilliseconds);
  };
  return debounced;
}

