/*
 * Desktop.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.GWT;

public class Desktop
{
   public static native boolean isDesktop() /*-{
      return !!$wnd.desktop;
   }-*/;

   public static DesktopFrame getFrame()
   {
      return desktopFrame_;
   }

   private static final DesktopFrame desktopFrame_ = GWT.create(DesktopFrame.class);
}
