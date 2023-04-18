/*
 * window-utils.test.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */

import { assert } from 'chai';
import { Rectangle } from 'electron';
import { describe } from 'mocha';
import { intersects } from '../../../src/main/window-utils';

function rec(height = 10, width = 10, x = 0, y = 0): Rectangle {
  return { height: height, width: width, x: x, y: y };
}

/**
 * A note on Electron's rectangle/display coordinate system:
 * (x, y) coord is top left corner of a Rectangle or Display object
 * (x + width, y + height) is bottom right corner
 *
 * x increases to the right, decreases to the left
 * y increases downwards, decreases upwards
 *
 * primary display's (x, y) coord is always (0, 0)
 * negative values are legal
 * external display to the right of primary display could be (primary.width, 0) ex. (1920, 0)
 * external display to the left of primary display could be (-secondary.width, 0) ex. (-1200, 0)
 */
describe('intersects', () => {
  const FIRST_WIDTH = 10;
  const FIRST_HEIGHT = 10;
  const FIRST_X = 0;
  const FIRST_Y = 0;

  const SECOND_WIDTH = 20;
  const SECOND_HEIGHT = 20;
  const SECOND_X = 0;
  const SECOND_Y = 0;

  const X_FAR_OUT_WEST = -100;
  const X_FAR_BACK_EAST = 100;
  const Y_FAR_UP_NORTH = -100;
  const Y_FAR_DOWN_SOUTH = 100;

  it('basic case', () => {
    assert.isTrue(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, SECOND_X + 1, SECOND_Y + 1),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    ); // entirely inside

    // top and left boarders shared
    assert.isTrue(intersects(rec(), rec(SECOND_WIDTH, SECOND_HEIGHT, SECOND_X, SECOND_Y)));

    // same size rectangles is valid
    assert.isTrue(intersects(rec(), rec()));
  });
  it('backwards case', () => {
    assert.isTrue(intersects(rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y), rec()));
  });
  it('partially outside', () => {
    assert.isTrue(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, FIRST_X + 11, FIRST_Y),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isTrue(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, FIRST_X, FIRST_Y + 11),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isTrue(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, FIRST_X - 1, FIRST_Y),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isTrue(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, FIRST_X, FIRST_Y - 1),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
  });
  it('entirely outside', () => {
    assert.isFalse(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, X_FAR_BACK_EAST, Y_FAR_DOWN_SOUTH),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isFalse(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, X_FAR_OUT_WEST, Y_FAR_DOWN_SOUTH),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isFalse(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, X_FAR_BACK_EAST, Y_FAR_UP_NORTH),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
    assert.isFalse(
      intersects(
        rec(FIRST_HEIGHT, FIRST_WIDTH, X_FAR_OUT_WEST, Y_FAR_UP_NORTH),
        rec(SECOND_HEIGHT, SECOND_WIDTH, SECOND_X, SECOND_Y),
      ),
    );
  });
});
