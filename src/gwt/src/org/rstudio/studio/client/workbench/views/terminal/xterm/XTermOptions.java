/*
 * XTermOptions.java
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

/**
 * xterm.js ITerminalOptions
 */
public class XTermOptions extends JavaScriptObject
{
   public static final String[] stringOptions = {
         "bellStyle", "cursorStyle", "fontFamily", "fontWeight", "fontWeightBold", "rendererType"
   };

   public static final String[] boolOptions = {
         "allowTransparency", "cancelEvents", "convertEol", "cursorBlink", "disableStdin",
         "drawBoldTextInBrightColors", "macOptionClickForcesSelection", "macOptionIsMeta",
         "rightClickSelectsWord", "screenReaderMode", "windowsMode"
   };

   public static final String[] numberOptions = {
         "fontSize", "letterSpacing", "lineHeight", "tabStopWidth", "scrollback"
   };

   // Required by JavaScriptObject subclasses
   protected XTermOptions() {}

   public final native static XTermOptions create(
         String bellStyle,
         boolean cursorBlink,
         String rendererType,
         boolean windowsMode,
         XTermTheme theme,
         String fontFamily) /*-{
      return {
         "bellStyle": bellStyle,
         "cursorBlink": cursorBlink,
         "rendererType": rendererType,
         "windowsMode": windowsMode,
         "theme": theme,
         "fontFamily": fontFamily
     };
   }-*/;
}
