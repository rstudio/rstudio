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
 *
 * Note: bellStyle, windowsMode, and rendererType were removed in xterm.js 6.0.
 * - Bell is now handled via the onBell event
 * - windowsMode was deprecated and removed
 * - rendererType is no longer an option; use @xterm/addon-webgl for GPU acceleration
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermOptions
{
   public boolean cursorBlink;
   public boolean screenReaderMode;
   public XTermTheme theme;
   public String fontFamily;
   public double fontSize;
   public double lineHeight;

   // Performance options
   public int smoothScrollDuration;
   public int minimumContrastRatio;
   public boolean allowTransparency;
   public int scrollback;

   // Rendering options
   public boolean customGlyphs;

   @JsOverlay public static XTermOptions create(
         boolean cursorBlink,
         boolean screenReaderMode,
         XTermTheme theme,
         String fontFamily,
         double fontSize,
         double lineHeight)
   {
      XTermOptions options = new XTermOptions();
      options.cursorBlink = cursorBlink;
      options.screenReaderMode = screenReaderMode;
      options.theme = theme;
      options.fontFamily = fontFamily;
      options.fontSize = fontSize;
      options.lineHeight = lineHeight;

      // Performance tuning
      options.smoothScrollDuration = 0;  // Disable smooth scrolling for instant response
      options.minimumContrastRatio = 1;  // Disable contrast adjustment (1 = no adjustment)
      options.allowTransparency = false; // Disable transparency for better performance
      options.scrollback = 1000;         // Default scrollback buffer size

      // Rendering - draw block/box characters programmatically for perfect alignment
      // Note: customGlyphs only works with WebGL renderer, not DOM renderer
      options.customGlyphs = true;

      return options;
   }
}
