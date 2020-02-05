/*
 * soft_break.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { Extension } from '../api/extension';
import { PandocOutput, PandocToken, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'soft_break',
      spec: {
        inline: true,
        content: 'text*',
        group: 'inline',
        parseDOM: [{ tag: "span[class*='soft-break']" }],
        toDOM() {
          return ['span', { class: 'soft-break' }, 0];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.SoftBreak,
            node: 'soft_break',
            getText: (tok: PandocToken) => ' ',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.SoftBreak);
        },
      },
    },
  ],
};

export default extension;
