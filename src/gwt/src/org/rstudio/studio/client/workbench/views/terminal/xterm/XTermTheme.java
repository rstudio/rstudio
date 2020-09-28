/*
 * XTermTheme.java
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

import com.google.gwt.core.client.JsArrayString;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.FontSizer;

/**
 * Contains colors to theme the terminal with (ITheme).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermTheme
{
   @JsOverlay public static double XTERM_SELECTION_ALPHA = 0.3;

   public String foreground;
   public String background;
   public String cursor;
   public String cursorAccent;
   public String selection;
   public String black;
   public String red;
   public String green;
   public String yellow;
   public String blue;
   public String magenta;
   public String cyan;
   public String white;
   public String brightBlack;
   public String brightRed;
   public String brightGreen;
   public String brightYellow;
   public String brightBlue;
   public String brightMagenta;
   public String brightCyan;
   public String brightWhite;

   /**
    * Create a terminal theme matching current editor theme. Unlike xterm 2.x,
    * xterm 3.x does not directly use css for styling, so we must sync the theme
    * with the css.
    */
   @JsOverlay public static XTermTheme terminalThemeFromEditorTheme()
   {
      // extract terminal selection color from existing theme, and add alpha
      JsArrayString classes = JsArrayString.createArray().cast();
      classes.push("terminal");
      classes.push("xterm-selection");
      classes.push("");
      ColorUtil.RGBColor solidSelectionColor = ColorUtil.RGBColor.fromCss(
            DomUtils.extractCssValue(classes, "background-color"));
      ColorUtil.RGBColor selectionColor = new ColorUtil.RGBColor(
            solidSelectionColor.red(),
            solidSelectionColor.green(),
            solidSelectionColor.blue(),
            XTERM_SELECTION_ALPHA);

      return create(
            DomUtils.extractCssValue("terminal", "background-color"),
            DomUtils.extractCssValue("terminal", "color"),

            DomUtils.extractCssValue("ace_cursor", "color"),
            DomUtils.extractCssValue("ace_editor", "color"),

            selectionColor.asRgb(),

            DomUtils.extractCssValue("xtermColor0", "color"),
            DomUtils.extractCssValue("xtermColor1", "color"),
            DomUtils.extractCssValue("xtermColor2", "color"),
            DomUtils.extractCssValue("xtermColor3", "color"),
            DomUtils.extractCssValue("xtermColor4", "color"),
            DomUtils.extractCssValue("xtermColor5", "color"),
            DomUtils.extractCssValue("xtermColor6", "color"),
            DomUtils.extractCssValue("xtermColor7", "color"),

            DomUtils.extractCssValue("xtermColor8", "color"),
            DomUtils.extractCssValue("xtermColor9", "color"),
            DomUtils.extractCssValue("xtermColor10", "color"),
            DomUtils.extractCssValue("xtermColor11", "color"),
            DomUtils.extractCssValue("xtermColor12", "color"),
            DomUtils.extractCssValue("xtermColor13", "color"),
            DomUtils.extractCssValue("xtermColor14", "color"),
            DomUtils.extractCssValue("xtermColor15", "color")
      );
   }

   @JsOverlay public static String getFontFamily()
   {
      return DomUtils.extractCssValue("ace_editor", "font-family");
   }

   @JsOverlay private static boolean doubleEqualish(double d1, double d2)
   {
      return MathUtil.isEqual(d1, d2, 0.0001);
   }

   @JsOverlay public static double adjustFontSize(double size)
   {
      size += BrowseCap.getFontSkew();

      // standard values for sizes we expose in preferences
      if (doubleEqualish(size, 7.0))
         return 9.0;
      else if (doubleEqualish(size, 8.0))
         return 11.0;
      else if (doubleEqualish(size, 9.0))
         return 12.0;
      else if (doubleEqualish(size, 10.0))
         return 13.0;
      else if (doubleEqualish(size, 11.0))
         return 15.0;
      else if (doubleEqualish(size, 12.0))
         return 16.0;
      else if (doubleEqualish(size, 13.0))
         return 17.0;
      else if (doubleEqualish(size, 14.0))
         return 19.0;
      else if (doubleEqualish(size, 16.0))
         return 22.0;
      else if (doubleEqualish(size, 18.0))
         return 24.0;
      else if (doubleEqualish(size, 24.0))
         return 32.0;
      else if (doubleEqualish(size, 36.0))
         return 48.0;
      else
         return Math.round(size * 1.3333333);
   }

   @JsOverlay public static double computeLineHeight()
   {
      double lineHeight = FontSizer.getNormalLineHeight();

      // due to units oddity, have to scale down before passing to xterm.js
      // (pixels vs. pts); don't go below 1.0 as lines will begin to overlap
      return Math.max(lineHeight > 1.0 ? lineHeight * 0.75 : 1.0, 1.0);
   }

   @JsOverlay public static XTermTheme create(
         String background, // default background color
         String foreground, // default foreground color
         String cursor, // cursor color
         String cursorAccent, // cursor accent color (foreground color for a block cursor)
         String selection, // selection color (can be transparent)
         String black, // ANSI black: \x1b[30m (xtermColor0)
         String red, // ANSI red: \x1b[31m (xtermColor1)
         String green, // ANSI green: \x1b[32m (xtermColor2)
         String yellow, // ANSI yellow: \x1b[33m (xtermColor3)
         String blue, // ANSI blue: \x1b[34m) (xtermColor4)
         String magenta, // ANSI magenta: \x1b[35 (xtermColor5)
         String cyan, // ANSI cyan: \x1b[36m (xtermColor6)
         String white, // ANSI white: \x1b[37m (xtermColor7)
         String brightBlack, // ANSI bright black: \x1b[1;30m (xtermColor8)
         String brightRed, // ANSI bright red: \x1b[1;31m (xtermColor9)
         String brightGreen, // ANSI bright green: \x1b[1;32m (xtermColor10)
         String brightYellow, // ANSI bright yellow: \x1b[1;33m (xtermColor11)
         String brightBlue, // ANSI bright blue: \x1b[1;34m (xtermColor12)
         String brightMagenta, // ANSI bright magenta: \x1b[1;35m (xtermColor13)
         String brightCyan, // ANSI bright cyan: \x1b[1;36m (xtermColor14)
         String brightWhite) // ANSI bright white: \x1b[1;37m (xtermColor15)
   {
      XTermTheme theme = new XTermTheme();

      theme.background = background;
      theme.black = black;
      theme.blue = blue;
      theme.brightBlack = brightBlack;
      theme.brightBlue = brightBlue;
      theme.brightCyan = brightCyan;
      theme.brightGreen = brightGreen;
      theme.brightMagenta = brightMagenta;
      theme.brightRed = brightRed;
      theme.brightWhite = brightWhite;
      theme.brightYellow = brightYellow;
      theme.cursor = cursor;
      theme.cursorAccent = cursorAccent;
      theme.cyan = cyan;
      theme.foreground = foreground;
      theme.green = green;
      theme.magenta = magenta;
      theme.red = red;
      theme.selection = selection;
      theme.white = white;
      theme.yellow = yellow;

      return theme;
   }
}
