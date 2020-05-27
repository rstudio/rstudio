/*
 * tooltip.ts
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

import tlite from 'tlite';

import './tooltip.css';

export function showTooltip(
  el: Element,
  text: string,
  grav: 's' | 'n' | 'e' | 'w' | 'sw' | 'se' | 'nw' | 'ne' = 'n',
  timeout = 2000,
) {
  el.setAttribute('title', '');
  el.setAttribute('data-tlite', text);
  tlite.show(el, { grav });
  setTimeout(() => tlite.hide(el), timeout);
}
