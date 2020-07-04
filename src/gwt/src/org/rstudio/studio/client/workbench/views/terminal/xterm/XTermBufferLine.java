/*
 * XTermBufferLine.java
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
 * Represents a line in the terminal's buffer (IBufferLine).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermBufferLine
{
   /**
    * Whether the line is wrapped from the previous line.
    */
   @JsProperty public native boolean getIsWrapped();

   /**
    * The length of the line, all call to getCell beyond the length will result
    * in `undefined`.
    */
   @JsProperty public native int getLength();

   /**
    * Gets a cell from the line, or undefined if the line index does not exist.
    *
    * Note that the result of this function should be used immediately after
    * calling as when the terminal updates it could lead to unexpected
    * behavior.
    *
    * @param x    The character index to get.
    * @param cell Optional cell object to load data into for performance
    *             reasons. This is mainly useful when every cell in the buffer is being
    *             looped over to avoid creating new objects for every cell.
    */
   public native XTermBufferCell getCell(int x, XTermBufferCell cell);

   /**
    * Gets the line as a string. Note that this is gets only the string for the
    * line, not taking isWrapped into account.
    *
    * @param trimRight   Whether to trim any whitespace at the right of the line.
    * @param startColumn The column to start from (inclusive).
    * @param endColumn   The column to end at (exclusive).
    */
   public native String translateToString(boolean trimRight, int startColumn, int endColumn);
   public native String translateToString();
}

