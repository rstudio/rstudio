/*
 * ThemeFonts.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.core.client.GWT;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;


public class ThemeFonts
{
   private static final ThemeFontLoader fontLoader =
         GWT.create(ThemeFontLoader.class);
   
   public static String getProportionalFont()
   {
      return fontLoader.getProportionalFont() + ", serif";
   }

   public static String getFixedWidthFont()
   {
      return fontLoader.getFixedWidthFont() + ", monospace";
   }

   static interface ThemeFontLoader
   {
      String getProportionalFont();
      String getFixedWidthFont();
   }
   
   public static String getFixedWidthClass()
   {
      return "rstudio-fixed-width-font";
   }

   static class DesktopThemeFontLoader implements ThemeFontLoader
   {
      public final String getProportionalFont()
      {
         return getDesktopInfo("proportionalFont");
      }
      
      public final String getFixedWidthFont()
      {
         return getDesktopInfo("fixedWidthFont");
      }
      
      private static final native String getDesktopInfo(String property)
      /*-{
         
         // NOTE: because this is called very early during the startup process
         // (GWT needs to generate CSS based on the value of these entries),
         // it's possible (for satellite windows) that the 'desktopInfo' object
         // will not yet be initialized -- so attempt to read it from an opener
         // directly.
         var window = $wnd;
         while (window) {
            if (window.desktopInfo)
               break;
            window = window.opener;
         }
         
         return window.desktopInfo[property];
         
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
         // First choice is the font the user specified (not available until
         // session info is loaded)
         String font = "";
         if (RStudioGinjector.INSTANCE.getSession().getSessionInfo() != null)
         {
            UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();
            if (prefs.serverEditorFontEnabled().getValue())
            {
               font = prefs.serverEditorFont().getValue();
            }

            if (StringUtil.isNullOrEmpty(font))
            {
               // No user preference registered
               font = "";
            }
            else
            {
               // User preference registered; remaining fonts are fallbacks
               font = "\"" + font + "\", ";
            }
         }
         
         if (BrowseCap.isMacintosh())
            font += "Monaco, monospace";
         else if (BrowseCap.isLinux())
            font += "\"Ubuntu Mono\", \"Droid Sans Mono\", \"DejaVu Sans Mono\", monospace";
         else
            font += "Consolas, \"Lucida Console\", monospace";
         
         return font;
      }
   }

   /**
    * Empty implementation of theme font loader used for test mock
    */
   static class EmptyThemeFontLoader implements ThemeFontLoader
   {

      @Override
      public String getProportionalFont()
      {
         return "";
      }

      @Override
      public String getFixedWidthFont()
      {
         return "";
      }
      
   }
}
