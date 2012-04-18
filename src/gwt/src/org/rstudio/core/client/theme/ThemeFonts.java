/*
 * ThemeFonts.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.theme;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.BrowseCap;


public class ThemeFonts
{
   private static final ThemeFontLoader fontLoader =
         GWT.create(ThemeFontLoader.class);
   private static final String proportionalFont =
         fontLoader.getProportionalFont();
   private static final String fixedWidthFont = fontLoader.getFixedWidthFont();

   public static String getProportionalFont()
   {
      return proportionalFont;
   }

   public static String getFixedWidthFont()
   {
      return fixedWidthFont;
   }

   static interface ThemeFontLoader
   {
      String getProportionalFont();
      String getFixedWidthFont();
   }

   static class DesktopThemeFontLoader implements ThemeFontLoader
   {
      public native final String getProportionalFont() /*-{
         return $wnd.desktop.proportionalFont;
      }-*/;

      public native final String getFixedWidthFont() /*-{
         return $wnd.desktop.fixedWidthFont;
      }-*/;
   }

   static class WebThemeFontLoader implements ThemeFontLoader
   {
      public String getProportionalFont()
      {
         String font = BrowseCap.hasUbuntuFonts() ? "Ubuntu, " : "";
         return font + "\"Lucida Sans\", \"DejaVu Sans\", \"Lucida Grande\", \"Segoe UI\", Verdana, Helvetica, sans-serif"; 
      }

      public String getFixedWidthFont()
      {
         if (BrowseCap.isMacintosh())
            return "Monaco, monospace";
         else if (BrowseCap.isLinux())
            return "\"Ubuntu Mono\", \"Droid Sans Mono\", \"DejaVu Sans Mono\", monospace";
         else
            return "Consolas, \"Lucida Console\", monospace";
      }
   }
}
