/*
 * Desktop.java
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
package org.rstudio.studio.client.application;

import org.rstudio.studio.client.RStudioGinjector;
import com.google.gwt.core.client.GWT;

public class Desktop
{
   public static native boolean isDesktop() /*-{
      // we're in desktop mode if the program mode is explicitly set that way;
      // as a fallback, check for the desktop object injected by Electron
      return ($wnd.program_mode === "desktop" || !!$wnd.desktop);
   }-*/;

   public static native boolean isDesktopReady() /*-{
      return !!$wnd.desktop;
   }-*/;

   public static boolean hasDesktopFrame()
   {
      return isDesktop();
   }
   
   public static DesktopFrame getFrame()
   {
      return desktopFrame_;
   }

   /**
    * @return true if using web-based file dialogs
    */
   public static boolean isUsingWebFileDialogs() {
      return !isDesktop() || !RStudioGinjector.INSTANCE.getUserPrefs().nativeFileDialogs().getValue();
   }

   private static final DesktopFrame desktopFrame_ = GWT.create(DesktopFrame.class);
}
