/*
 * ShinyDisconnectNotifier.java
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;

public class ShinyDisconnectNotifier
{
   public interface ShinyDisconnectSource
   {
      public String getShinyUrl();
      public void onShinyDisconnect();
   }

   public ShinyDisconnectNotifier(ShinyDisconnectSource source)
   {
      source_ = source;
      suppressUrl_ = null;
      initializeEvents();
   }
   
   /**
    * Begins suppressing disconnect notifications from the current URL.
    */
   public void suppress()
   {
      if (!StringUtil.isNullOrEmpty(suppressUrl_))
      {
         // should never happen in practice; if it does the safest behavior is
         // to respect the new suppress URL and discard the old one. warn that
         // we're doing this.
         Debug.logWarning("Replacing old Shiny disconnect suppress URL: " + suppressUrl_);
      }
      suppressUrl_ = source_.getShinyUrl();
   }
   
   /**
    * Ends suppression of disconnect notifications.
    */
   public void unsuppress()
   {
      suppressUrl_ = null;
   }

   private native void initializeEvents() /*-{  
      
      var self = this;
      var callback = $entry(function(event) {
         
         if (typeof event.data !== "string")
            return;
         
         self.@org.rstudio.studio.client.shiny.ShinyDisconnectNotifier::onMessage(*)(
            event.data,
            event.origin,
            event.target.name
         );
         
      });
      
      $wnd.addEventListener("message", callback, true);
      
   }-*/;
   
   private void onMessage(String data, String origin, String name)
   {
      // check to see if this is a 'disconnected' message
      if (!StringUtil.equals(data, "disconnected"))
         return;
      
      // check to see if the message originated from the same origin
      String url = source_.getShinyUrl();
      if (!url.startsWith(origin))
         return;
      
      // if we were suppressing disconnect notifications from this URL,
      // consume this disconnection and resume
      if (StringUtil.equals(url, suppressUrl_))
      {
         unsuppress();
         return;
      }
      
      // respond to Shiny disconnect
      source_.onShinyDisconnect();
   }

   private final ShinyDisconnectSource source_;
   private String suppressUrl_;
}
