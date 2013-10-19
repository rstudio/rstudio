/*
 * DesktopWindowOpener.java
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
package org.rstudio.studio.client.common.impl;

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.SatelliteUtils;

import com.google.gwt.core.client.GWT;

public class DesktopWindowOpener extends WebWindowOpener
{
   @Override
   public void openWindow(GlobalDisplay globalDisplay,
                          String url,
                          NewWindowOptions options)
   {  
      // open externally if we have a protocol and aren't an app url
      if (hasProtocol(url) && !isAppUrl(url))
      {
         Desktop.getFrame().browseUrl(url);

         assert options.getCallback() == null;
      }
      else
      {
         // if this is a relative url then prepend the host page base
         // url (so Qt correctly navigates)
         if (!hasProtocol(url))
         {
            if (url.startsWith("/"))
               url = url.substring(1);
            url = GWT.getHostPageBaseURL() + url;
         }
         
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
