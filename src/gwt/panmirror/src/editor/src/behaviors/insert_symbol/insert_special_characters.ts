/*
 * insert_special_characters.ts
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


import { EditorCommandId, InsertCharacterCommand } from "../../api/command";

const extension = {

  commands: () => {
    return [
      new InsertCharacterCommand(EditorCommandId.EmDash, '—', []),
      new InsertCharacterCommand(EditorCommandId.EnDash, '–', []),
      new InsertCharacterCommand(EditorCommandId.HardLineBreak, '\n', ['Shift-Enter'])
    ];
  },
};

export default extension;
