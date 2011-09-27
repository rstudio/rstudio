/*
 * BrowseCap.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import org.rstudio.core.client.theme.ThemeFonts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;

public class BrowseCap
{
   public static double getFontSkew()
   {
      if (hasMetaKey())
         return -1;
      else if (FIXED_UBUNTU_MONO)
         return 0.4;
      else
         return 0;
   }

   public static final BrowseCap INSTANCE = GWT.create(BrowseCap.class);

   public boolean suppressBraceHighlighting()
   {
      return false;
   }

   public boolean aceVerticalScrollBarIssue()
   {
      return false;
   }

   public boolean suppressBrowserForwardBack()
   {
      return false;
   }
   
   public boolean hasWindowFind()
   {
      return true;
   }

   public static boolean hasMetaKey()
   {
      return OPERATING_SYSTEM.equals("macintosh");
   }

   public static boolean isLinux()
   {
      return OPERATING_SYSTEM.equals("linux");
   }
   
   public static boolean isChrome()
   {
      return isUserAgent("chrome");
   }
   
   private static native final boolean isUserAgent(String uaTest) /*-{
      var ua = navigator.userAgent.toLowerCase();
      if (ua.indexOf(uaTest) != -1)
         return true;
      else
         return false;      
   }-*/;

   private static native final String getOperatingSystem() /*-{
      var ua = navigator.userAgent.toLowerCase();
      if (ua.indexOf("linux") != -1) {
         return "linux";
      } else if (ua.indexOf("macintosh") != -1) {
         return "macintosh";
      }
      return "windows";
   }-*/;
   private static final String OPERATING_SYSTEM = getOperatingSystem();

   private static final boolean getFixedUbuntuMono()
   {
      if (isLinux())
      {
         String fixedWidthFont =  ThemeFonts.getFixedWidthFont();
         return (StringUtil.notNull(fixedWidthFont).equals("\"Ubuntu Mono\""));
      }
      else
      {
         return true;
      }
   }

   private static final boolean FIXED_UBUNTU_MONO = getFixedUbuntuMono();

   static
   {
      Document.get().getBody().addClassName(OPERATING_SYSTEM);

      if (FIXED_UBUNTU_MONO)
         Document.get().getBody().addClassName("ubuntu_mono");
   }
}
