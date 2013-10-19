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

import com.google.gwt.core.client.GWT;

public class NodeWebkitWindowOpener extends WebWindowOpener
{
   @Override
   public void openWindow(GlobalDisplay globalDisplay,
                          String url,
                          NewWindowOptions options)
   {   
      // attempt to open windows externally -- this protects against
      // file downloads as well as browser content that might link to 
      // external pages. exceptions:
      //   (1) minimal windows (e.g. plot zoom window) a
      //   (2) windows with open callbacks
      if (options.getCallback() == null)
      {
         // if this is a relative url then prepend the host page base
         // url (so node sebkit correctly navigates)
         if (!hasProtocol(url))
         {
            if (url.startsWith("/"))
               url = url.substring(1);
            url = GWT.getHostPageBaseURL() + url;
         }
         
         NodeWebkit.browseURL(url);
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
