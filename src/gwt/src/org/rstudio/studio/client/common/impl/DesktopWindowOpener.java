/*
 * DesktopWindowOpener.java
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
package org.rstudio.studio.client.common.impl;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
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
         Desktop.getFrame().browseUrl(StringUtil.notNull(url));

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
   public void openWebMinimalWindow(GlobalDisplay globalDisplay,
                                    String url,
                                    NewWindowOptions options,
                                    int width,
                                    int height,
                                    boolean showLocation)
   {
      Desktop.getFrame().prepareForNamedWindow(StringUtil.notNull(options.getName()),
            options.allowExternalNavigation(),
            options.showDesktopToolbar(), () ->
               super.openWebMinimalWindow(globalDisplay, url, options, width, 
                                          height, showLocation));
   }

   @Override
   public void openMinimalWindow(GlobalDisplay globalDisplay,
                                 String url,
                                 NewWindowOptions options,
                                 int width,
                                 int height,
                                 boolean showLocation)
   {
      Desktop.getFrame().openMinimalWindow(StringUtil.notNull(options.getName()),
                                           StringUtil.notNull(url),
                                           width,
                                           height);
   }
   
   @Override
   public void openSatelliteWindow(GlobalDisplay globalDisplay,
                                   String mode,
                                   int width,
                                   int height, 
                                   NewWindowOptions options)
   {  
      String windowName = SatelliteUtils.getSatelliteWindowName(mode);

      // default to desktop-assigned location, but if a specific position was
      // assigned, use it
      int x = -1;
      int y = -1;
      Point pos = options.getPosition();
      if (pos != null)
      {
         x = pos.getX();
         y = pos.getY();
      }

      Desktop.getFrame().prepareForSatelliteWindow(StringUtil.notNull(windowName), x, y, 
            width, height, ()-> 
         super.openSatelliteWindow(globalDisplay, mode, width, height, options));
   }
   
   @Override
   protected boolean showPopupBlockedMessage()
   {
      return false;
   }
   
}
