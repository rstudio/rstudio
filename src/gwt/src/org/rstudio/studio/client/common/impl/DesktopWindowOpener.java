/*
 * DesktopWindowOpener.java
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
package org.rstudio.studio.client.common.impl;

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.SatelliteUtils;

public class DesktopWindowOpener extends WebWindowOpener
{
   @Override
   public void openWindow(GlobalDisplay globalDisplay,
                          String url,
                          NewWindowOptions options)
   {
      if (url.startsWith("file:") || options.alwaysUseBrowser())
      {
         Desktop.getFrame().browseUrl(url);

         assert options.getCallback() == null;
      }
      else
      {
         super.openWindow(globalDisplay, 
                          url,
                          options);
      }
   }

   @Override
   public void openMinimalWindow(GlobalDisplay globalDisplay,
                                 String url,
                                 NewWindowOptions options,
                                 int width,
                                 int height,
                                 boolean showLocation)
   {
      Desktop.getFrame().openMinimalWindow(options.getName(),
                                           url,
                                           width,
                                           height);
   }
   
   @Override
   public void openSatelliteWindow(GlobalDisplay globalDisplay,
                                   String mode,
                                   int width,
                                   int height)
   {  
      String windowName = SatelliteUtils.getSatelliteWindowName(mode);
      Desktop.getFrame().prepareForSatelliteWindow(windowName, width, height);
      super.openSatelliteWindow(globalDisplay, mode, width, height);
   }
   
   @Override
   protected boolean showPopupBlockedMessage()
   {
      return false;
   }
   
}
