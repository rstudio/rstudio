/*
 * context-menu.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import { _createContextMenuTemplate } from '../../../src/main/context-menu';

describe('ContextMenu', () => {
  it('Context menu is created correctly', () => {
    const paramsWithImage: any = {
      hasImageContents: true,
    };

    const contextMenuTemplateWithImage = _createContextMenuTemplate({} as any, paramsWithImage);

    console.log('length ', contextMenuTemplateWithImage.length);

    assert.isTrue(contextMenuTemplateWithImage.length == 6, 'Context Menu With Image should have 6 elements');

    const paramsWithoutImageA: any = {
      editFlags: {
        canCut: true,
        canCopy: false,
        canPaste: true,
        canSelectAll: false,
      },
    };

    const paramsWithoutImageB: any = {
      editFlags: {
        canCut: false,
        canCopy: false,
        canPaste: false,
        canSelectAll: false,
      },
    };

    const paramsWithoutImageC: any = {
      editFlags: {
        canCut: true,
        canCopy: true,
        canPaste: true,
        canSelectAll: true,
      },
    };

    const contextMenuTemplateWithoutImageA = _createContextMenuTemplate({} as any, paramsWithoutImageA);
    const contextMenuTemplateWithoutImageB = _createContextMenuTemplate({} as any, paramsWithoutImageB);
    const contextMenuTemplateWithoutImageC = _createContextMenuTemplate({} as any, paramsWithoutImageC);

    const checkParamsAndMenu = (contextMenu: any, params: any) => {
      Object.keys(params.editFlags).forEach((key, index) => {
        if (params.editFlags[key] == false) {
          assert.isFalse(
            contextMenu[index].enabled,
            'Context Menu Without Image is not disabled, or is being created in a different order',
          );
        }
      });
    };

    checkParamsAndMenu(contextMenuTemplateWithoutImageA, paramsWithoutImageA);
    checkParamsAndMenu(contextMenuTemplateWithoutImageB, paramsWithoutImageB);
    checkParamsAndMenu(contextMenuTemplateWithoutImageC, paramsWithoutImageC);
  });
});
