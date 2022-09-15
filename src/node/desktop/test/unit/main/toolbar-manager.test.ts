/*
 * toolbar-manager.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import { ToolbarManager } from '../../../src/main/toolbar-manager';

describe('ToolbarManager', () => {
  const toolbarManager = new ToolbarManager();
  const buttons = [
    {
      tooltip: 'Go Back',
      iconPath: __dirname + '/../../../src/assets/img/back.png',
      onClick: `()=> {
          history.back();
        }`,
    },
    {
      tooltip: 'Go Forward',
      iconPath: __dirname + '/../../../src/assets/img/forward.png',
      onClick: `()=> {
          history.forward();
        }`,
    },
    {
      tooltip: 'Refresh Page',
      iconPath: __dirname + '/../../../src/assets/img/reload.png',
      onClick: `()=> {
          window.location.reload();
        }`,
    },
  ];

  it('Image Path is correctly converted to base64', () => {
    const base64stringArr = buttons.map((button) => toolbarManager.convertImagePathToBase64(button.iconPath));

    base64stringArr.forEach((base64string) => {
      const base64PngPrefix = 'data:image/png;base64, ';
      const base64StringPrefix = base64string.substring(0, base64PngPrefix.length);

      assert.strictEqual(base64StringPrefix, base64PngPrefix);
    });
  });

  it('Toolbar contains correct data', () => {
    const toolbarDataString = toolbarManager.createToolbarData(buttons);

    const toolbarJSString = toolbarManager.addToolbarJsAsText();
    const toolbarStylesString = toolbarManager.addStylesJsAsText();
    const buttonsJsString = toolbarManager.addButtonsJsAsText(buttons);

    assert.isTrue(toolbarDataString.includes(toolbarJSString), 'Toolbar JS is not included in Toolbar Data');

    assert.isTrue(toolbarDataString.includes(toolbarStylesString), 'Toolbar Styles are not included in Toolbar Data');

    assert.isTrue(toolbarDataString.includes(buttonsJsString), 'Buttons JS is not included in Toolbar Data');
  });
});
