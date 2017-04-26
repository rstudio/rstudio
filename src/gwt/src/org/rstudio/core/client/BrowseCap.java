/*
 * BrowseCap.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.core.client;

import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.FontDetector;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;

public class BrowseCap
{
   public static double getFontSkew()
   {
      if (hasMetaKey())
         return -1;
  
      else if (FIXED_UBUNTU_MONO)
      {
         if (isFirefox())
            return 1;
         else
            return 0.4;
      }
      else if (!Desktop.isDesktop() && isWindows())
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
      return !isInternetExplorer();
   }
   
   public boolean canCopyToClipboard()
   {
      return Desktop.isDesktop() || !isSafari();
   }
   
   
   public boolean isInternetExplorer()
   {
      return isUserAgent("trident");
   }
    
   public boolean isInternetExplorer10()
   {
      return false;
   }
   
   public static boolean hasMetaKey()
   {
      return isMacintosh();
   }
   
   public static boolean isMacintosh()
   {
      return OPERATING_SYSTEM.equals("macintosh");
   }
   
   public static boolean isMacintoshDesktop()
   {
      return Desktop.isDesktop() && isMacintosh();
   }
   
   public static boolean isCocoaDesktop()
   {
      return Desktop.isDesktop() && Desktop.getFrame().isCocoa();
   }
   
   public static boolean isWindows()
   {
      return OPERATING_SYSTEM.equals("windows");
   }
   
   public static boolean isWindowsDesktop()
   {
      return Desktop.isDesktop() && isWindows();
   }

   public static boolean isLinux()
   {
      return OPERATING_SYSTEM.equals("linux");
   }
   
   public static boolean isLinuxDesktop()
   {
      return Desktop.isDesktop() && isLinux();
   }
   
   public static boolean hasUbuntuFonts()
   {
      return FIXED_UBUNTU_MONO;
   }
   
   public static boolean isChrome()
   {
      return isUserAgent("chrome");
   }
   
   public static boolean isSafari()
   {
      return isUserAgent("safari") && !isChrome();
   }
   
   public static boolean isChromeLinux() 
   {
      return isChrome() && isLinux();
   }
      
   public static boolean isFirefox()
   {
      return isUserAgent("firefox");
   }
   
   public static boolean isChromeFrame()
   {
      return isUserAgent("chromeframe");
   }
   
   public static double devicePixelRatio() 
   {
      if (Desktop.isDesktop())
         return Desktop.getFrame().devicePixelRatio();
      else
         return getDevicePixelRatio();
   }

   public static String getPlatformName()
   {
      if (BrowseCap.isMacintosh())
         return "Mac";
      else if (BrowseCap.isLinux())
         return "Linux";
      else if (BrowseCap.isWindows())
         return "Windows";
      else
         return "Unknown";
   }

   public static String getBrowserName()
   {
      if (BrowseCap.isChrome())
         return "Chrome";
      else if (BrowseCap.isFirefox())
         return "Firefox";
      else if (BrowseCap.isSafari())
         return "Safari";
      else if (BrowseCap.INSTANCE.isInternetExplorer())
         return "IE";
      else
         return "Unknown";
   }
   
   private static native final double getDevicePixelRatio() /*-{
      try
      {
         if ('devicePixelRatio' in $wnd)
            return $wnd.devicePixelRatio;
         else
            return 1.0;
      }
      catch(ex)
      {
         return 1.0;
      }
   }-*/;
   
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
         // get fixed width font
         String fixedWidthFont = ThemeFonts.getFixedWidthFont();
         
         // in desktop mode we'll get an exact match whereas in web mode
         // we'll get a list of fonts so we need to do an additional probe
         if (Desktop.isDesktop())
            return StringUtil.notNull(fixedWidthFont).equals("\"Ubuntu Mono\"");
         else
            return FontDetector.isFontSupported("Ubuntu Mono");
      }
      else
      {
         return false;
      }
   }
   

   private static final boolean FIXED_UBUNTU_MONO = getFixedUbuntuMono();
   
   static
   {
      Document.get().getBody().addClassName(OPERATING_SYSTEM);

      if (isWindowsDesktop() && Desktop.getFrame().getDisplayDpi() >= 192)
      {
         Document.get().getBody().addClassName("windows-highdpi");
      }

      if (FIXED_UBUNTU_MONO)
      {
         Document.get().getBody().addClassName("ubuntu_mono");
         
         if (isFirefox())
            Document.get().getBody().addClassName("ubuntu_mono_firefox");
      }
   }
}
