/*
 * fragment.ts
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

import { Fragment } from 'prosemirror-model';

export function fragmentText(fragment: Fragment, unemoji = false) {
  let text = '';
  fragment.forEach(node => {
    const emjojiMark = node.marks.find(mark => mark.type === node.type.schema.marks.emoji);
    if (unemoji && emjojiMark) {
      return text = text + (emjojiMark.attrs.emojihint || node.textContent);
    } else {
      return text = text + node.textContent;
    }
  });
  return text;
}
