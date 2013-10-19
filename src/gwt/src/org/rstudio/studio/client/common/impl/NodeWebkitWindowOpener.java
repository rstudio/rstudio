/*
 * NodeWebkitWindowOpener.java
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

import org.rstudio.studio.client.application.NodeWebkit;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;

public class NodeWebkitWindowOpener extends WebWindowOpener
{
   @Override
   public void openWindow(GlobalDisplay globalDisplay,
                          String url,
                          NewWindowOptions options)
   {   
      // open externally if we have a protocol and aren't an app url
      if (hasProtocol(url) && !isAppUrl(url))
      {
         NodeWebkit.browseURL(url);
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
   protected boolean showPopupBlockedMessage()
   {
      return false;
   }
   
}
