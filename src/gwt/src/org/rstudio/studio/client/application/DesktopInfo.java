/*
 * DesktopInfo.java
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
package org.rstudio.studio.client.application;

public class DesktopInfo
{
   public static final native String getPlatform()
   /*-{
      return $wnd.desktopInfo.platform;
   }-*/;
   
   public static final native String getVersion()
   /*-{
      return $wnd.desktopInfo.version;
   }-*/;
   
   public static final native String getScrollingCompensationType()
   /*-{
      return $wnd.desktopInfo.scrollingCompensationType;
   }-*/;
   
   public static final native String getFixedWidthFontList()
   /*-{
      return $wnd.desktopInfo.fixedWidthFontList;
   }-*/;
   
   public static final native String getFixedWidthFont()
   /*-{
      return $wnd.desktopInfo.fixedWidthFont;
   }-*/;
}
