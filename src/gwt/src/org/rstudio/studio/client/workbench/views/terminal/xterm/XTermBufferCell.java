/*
 * XTermBufferCell.java
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
import jsinterop.annotations.JsType;

/**
 * Represents a single cell in the terminal's buffer (IBufferCell).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermBufferCell
{
   /**
    * The width of the character. Some examples:
    *
    * - `1` for most cells.
    * - `2` for wide character like CJK glyphs.
    * - `0` for cells immediately following cells with a width of `2`.
    */
   public native int getWidth();

   /**
    * The character(s) within the cell. Examples of what this can contain:
    *
    * - A normal width character
    * - A wide character (eg. CJK)
    * - An emoji
    */
   public native String getChars();

   /**
    * Gets the UTF32 codepoint of single characters, if content is a combined
    * string it returns the codepoint of the last character in the string.
    */
   public native int getCode();

   /**
    * Gets the number representation of the foreground color mode, this can be
    * used to perform quick comparisons of 2 cells to see if they're the same.
    * Use `isFgRGB`, `isFgPalette` and `isFgDefault` to check what color mode
    * a cell is.
    */
   public native int getFgColorMode();

   /**
    * Gets the number representation of the background color mode, this can be
    * used to perform quick comparisons of 2 cells to see if they're the same.
    * Use `isBgRGB`, `isBgPalette` and `isBgDefault` to check what color mode
    * a cell is.
    */
   public native int getBgColorMode();

   /**
    * Gets a cell's foreground color number, this differs depending on what the
    * color mode of the cell is:
    *
    * - Default: This should be 0, representing the default foreground color
    *   (CSI 39 m).
    * - Palette: This is a number from 0 to 255 of ANSI colors (CSI 3(0-7) m,
    *   CSI 9(0-7) m, CSI 38 ; 5 ; 0-255 m).
    * - RGB: A hex value representing a 'true color': 0xRRGGBB.
    *   (CSI 3 8 ; 2 ; Pi ; Pr ; Pg ; Pb)
    */
   public native int getFgColor();

   /**
    * Gets a cell's background color number, this differs depending on what the
    * color mode of the cell is:
    *
    * - Default: This should be 0, representing the default background color
    *   (CSI 49 m).
    * - Palette: This is a number from 0 to 255 of ANSI colors
    *   (CSI 4(0-7) m, CSI 10(0-7) m, CSI 48 ; 5 ; 0-255 m).
    * - RGB: A hex value representing a 'true color': 0xRRGGBB
    *   (CSI 4 8 ; 2 ; Pi ; Pr ; Pg ; Pb)
    */
   public native int getBgColor();

   /** Whether the cell has the bold attribute (CSI 1 m). */
   public native int isBold();

   /** Whether the cell has the inverse attribute (CSI 3 m). */
   public native int isItalic();

   /** Whether the cell has the inverse attribute (CSI 2 m). */
   public native int isDim();

   /** Whether the cell has the underline attribute (CSI 4 m). */
   public native int isUnderline();

   /** Whether the cell has the inverse attribute (CSI 5 m). */
   public native int isBlink();

   /** Whether the cell has the inverse attribute (CSI 7 m). */
   public native int isInverse();

   /** Whether the cell has the inverse attribute (CSI 8 m). */
   public native int isInvisible();

   /** Whether the cell is using the RGB foreground color mode. */
   public native boolean isFgRGB();

   /** Whether the cell is using the RGB background color mode. */
   public native boolean isBgRGB();

   /** Whether the cell is using the palette foreground color mode. */
   public native boolean isFgPalette();

   /** Whether the cell is using the palette background color mode. */
   public native boolean isBgPalette();

   /** Whether the cell is using the default foreground color mode. */
   public native boolean isFgDefault();

   /** Whether the cell is using the default background color mode. */
   public native boolean isBgDefault();

   /** Whether the cell has the default attribute (no color or style). */
   public native boolean isAttributeDefault();
}

