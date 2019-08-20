/*
 * XTermTheme.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.dom.DomUtils;

/**
 * xterm.js ITheme
 */
public class XTermTheme extends JavaScriptObject
{
   public static double XTERM_SELECTION_ALPHA = 0.3;

   // Required by JavaScriptObject subclasses
   protected XTermTheme() {}

   /**
    * Create a terminal theme matching current editor theme. Unlike xterm 2.x,
    * xterm 3.x does not directly use css for styling, so we must sync the theme
    * with the css.
    */
   public final static XTermTheme terminalThemeFromEditorTheme()
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

   public final native static XTermTheme create(
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
         String brightWhite // ANSI bright white: \x1b[1;37m (xtermColor15)

   ) /*-{
      return {
         "background": background,
         "black": black,
         "blue": blue,
         "brightBlack": brightBlack,
         "brightBlue": brightBlue,
         "brightCyan": brightCyan,
         "brightGreen": brightGreen,
         "brightMagenta": brightMagenta,
         "brightRed": brightRed,
         "brightWhite": brightWhite,
         "brightYellow": brightYellow,
         "cursor": cursor,
         "cursorAccent": cursorAccent,
         "cyan": cyan,
         "foreground": foreground,
         "green": green,
         "magenta": magenta,
         "red": red,
         "selection": selection,
         "white": white,
         "yellow": yellow
     };
   }-*/;
}
