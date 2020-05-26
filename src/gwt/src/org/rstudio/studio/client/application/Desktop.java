/*
 * Desktop.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;

public class Desktop
{
   public static native boolean isDesktop() /*-{
      // we're in desktop mode if the program mode is explicitly set that way;
      // as a fallback, check for the desktop object injected by Qt
      return ($wnd.program_mode === "desktop" || !!$wnd.desktop) &&
              !$wnd.remoteDesktop;
   }-*/;

   public static native boolean isRemoteDesktop() /*-{
      // we're in remote desktop mode if the remoteDesktop object was injected by Qt
      return !!$wnd.remoteDesktop;
   }-*/;
   
   public static native boolean isDesktopReady() /*-{
      return !!$wnd.desktop;
   }-*/;

   public static boolean hasDesktopFrame()
   {
      return isDesktop() || isRemoteDesktop();
   }
   
   public static DesktopFrame getFrame()
   {
      return desktopFrame_;
   }

   private static final DesktopFrame desktopFrame_ = GWT.create(DesktopFrame.class);
}
