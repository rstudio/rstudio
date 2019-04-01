/*
 * ShinyDisconnectNotifier.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
      var thiz = this;   
      $wnd.addEventListener(
            "message",
            $entry(function(e) {
               if (typeof e.data != 'string')
                  return;
               thiz.@org.rstudio.studio.client.shiny.ShinyDisconnectNotifier::onMessage(Ljava/lang/String;Ljava/lang/String;)(e.data, e.origin);
            }),
            true);
   }-*/;
   
   private void onMessage(String data, String origin)
   {  
      if ("disconnected".equals(data))
      {
         if (source_.getShinyUrl().startsWith(origin)) 
         {
            if (StringUtil.equals(source_.getShinyUrl(), suppressUrl_))
            {
               // we were suppressing disconnect notifications from this URL;
               // consume this disconnection and resume
               unsuppress();
               return;
            }
            source_.onShinyDisconnect();
         }
      }
   }

   private final ShinyDisconnectSource source_;
   private String suppressUrl_;
}
