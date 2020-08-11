/*
 * attr_edit.ts
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

import { Schema } from 'prosemirror-model';

import { EditorUI } from '../../api/ui';
import { pandocAttrEnabled } from '../../api/pandoc_attr';

import { AttrEditCommand } from './attr_edit-command';
import { AttrEditOptions } from '../../api/attr_edit';
import { PandocExtensions } from '../../api/pandoc';
import { AttrEditDecorationPlugin } from './attr_edit-decoration';
import { Extension } from '../../api/extension';
import { hasFencedCodeBlocks } from '../../api/pandoc_format';

export const kEditAttrShortcut = 'F4';

export function attrEditExtension(
  pandocExtensions: PandocExtensions,
  ui: EditorUI,
  editors: AttrEditOptions[],
): Extension {
  const hasAttr = pandocAttrEnabled(pandocExtensions) || hasFencedCodeBlocks(pandocExtensions);

  return {
    commands: (_schema: Schema) => {
      if (hasAttr) {
        return [new AttrEditCommand(ui, pandocExtensions, editors)];
      } else {
        return [];
      }
    },

    plugins: (_schema: Schema) => {
      if (hasAttr) {
        return [new AttrEditDecorationPlugin(ui, pandocExtensions, editors)];
      } else {
        return [];
      }
    },
  };
}
