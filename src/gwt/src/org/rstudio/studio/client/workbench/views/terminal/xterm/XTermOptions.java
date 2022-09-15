/*
 * XTermOptions.java
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
package org.rstudio.studio.client.workbench.views.terminal.xterm;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * xterm.js ITerminalOptions
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermOptions
{
   public String bellStyle;
   public boolean cursorBlink;
   public boolean screenReaderMode;
   public String rendererType;
   public boolean windowsMode;
   public XTermTheme theme;
   public String fontFamily;
   public double fontSize;
   public double lineHeight;

   @JsOverlay public static XTermOptions create(
         String bellStyle,
         boolean cursorBlink,
         boolean screenReaderMode,
         String rendererType,
         boolean windowsMode,
         XTermTheme theme,
         String fontFamily,
         double fontSize,
         double lineHeight)
   {
      XTermOptions options = new XTermOptions();
      options.bellStyle = bellStyle;
      options.cursorBlink = cursorBlink;
      options.screenReaderMode = screenReaderMode;
      options.rendererType = rendererType;
      options.windowsMode = windowsMode;
      options.theme = theme;
      options.fontFamily = fontFamily;
      options.fontSize = fontSize;
      options.lineHeight = lineHeight;
      return options;
   }
}
