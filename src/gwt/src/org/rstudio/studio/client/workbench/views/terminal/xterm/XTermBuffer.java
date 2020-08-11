/*
 * XTermBuffer.java
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
package org.rstudio.studio.client.workbench.views.terminal.xterm;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Represents a terminal buffer (IBuffer).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermBuffer
{
   /**
    * The type of the buffer.
    * 'normal' or 'alternate'
    */
   @JsProperty public native String getType();

   /**
    * The y position of the cursor. This ranges between `0` (when the
    * cursor is at baseY) and `Terminal.rows - 1` (when the cursor is on the
    * last row).
    */
   @JsProperty public native int getCursorY();

   /**
    * The x position of the cursor. This ranges between `0` (left side) and
    * `Terminal.cols` (after last cell of the row).
    */
   @JsProperty public native int getCursorX();

   /**
    * The line within the buffer where the top of the viewport is.
    */
   @JsProperty public native int getViewportY();

   /**
    * The line within the buffer where the top of the bottom page is (when
    * fully scrolled down).
    */
   @JsProperty public native int getBaseY();

   /**
    * The amount of lines in the buffer.
    */
   @JsProperty public native int getLength();

   /**
    * Gets a line from the buffer, or undefined if the line index does not
    * exist.
    *
    * Note that the result of this function should be used immediately after
    * calling as when the terminal updates it could lead to unexpected
    * behavior.
    *
    * @param y The line index to get.
    */
   public native XTermBufferLine getLine(int y);

   /**
    * Creates an empty cell object suitable as a cell reference in
    * `line.getCell(x, cell)`. Use this to avoid costly recreation of
    * cell objects when dealing with tons of cells.
    */
   public native XTermBufferCell getNullCell();
}

