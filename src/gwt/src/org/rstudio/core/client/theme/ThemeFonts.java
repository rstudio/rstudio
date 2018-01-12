/*
 * ThemeFonts.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.theme;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.BrowseCap;


public class ThemeFonts
{
   private static final ThemeFontLoader fontLoader =
         GWT.create(ThemeFontLoader.class);
   
   public static String getProportionalFont()
   {
      return fontLoader.getProportionalFont();
   }

   public static String getFixedWidthFont()
   {
      return fontLoader.getFixedWidthFont();
   }

   static interface ThemeFontLoader
   {
      String getProportionalFont();
      String getFixedWidthFont();
   }

   static class DesktopThemeFontLoader implements ThemeFontLoader
   {
      public native final String getProportionalFont() /*-{
         // if we were opened by another window, use its desktopInfo to
         // populate our font cache (as our own might not have arrived yet)
         if ($wnd.opener && $wnd.opener.desktopInfo)
            return $wnd.opener.desktopInfo.proportionalFont;
              
         return $wnd.desktopInfo.proportionalFont;
      }-*/;

      public native final String getFixedWidthFont() /*-{
         if ($wnd.opener && $wnd.opener.desktopInfo)
            return $wnd.opener.desktopInfo.fixedWidthFont;
              
         return $wnd.desktopInfo.fixedWidthFont;
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
