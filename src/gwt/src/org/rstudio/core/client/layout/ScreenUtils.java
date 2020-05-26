/*
 * ScreenUtils.java
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

package org.rstudio.core.client.layout;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.NativeScreen;
import org.rstudio.studio.client.application.Desktop;

public class ScreenUtils
{
   public static Size getAdjustedWindowSize(Size preferredSize)
   {
      // compute available height (trim to max)
      NativeScreen screen = NativeScreen.get();
      int height = Math.min(screen.getAvailHeight(), preferredSize.height);
      
      // trim height for large monitors
      if (screen.getAvailHeight() >= (preferredSize.height-100))
      {
         if (BrowseCap.isMacintosh())
            height = height - 107;
         else if (BrowseCap.isWindows())
            height = height - 89;
         else
            height = height - 80;
      }
      else
      {
         // adjust for window framing, etc.
         if (Desktop.hasDesktopFrame())
            height = height - 40;
         else
            height = height - 60;

         // extra adjustment for firefox on windows (extra chrome in url bar)
         if (BrowseCap.isWindows() && BrowseCap.isFirefox())
            height = height - 25;
      }
      
      // extra adjustment for chrome on linux (which misreports the 
      // available height, excluding the menubar/taskbar)
      if (BrowseCap.isLinux() && BrowseCap.isChrome())
         height = height - 50;

      // compute width (trim to max)
      int width = Math.min(preferredSize.width, screen.getAvailWidth() - 20);
      
      // return size
      return new Size(width, height);
   }

}
