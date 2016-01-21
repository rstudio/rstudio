/*
 * ShinyDisconnectNotifier.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
      initializeEvents();
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
            source_.onShinyDisconnect();
         }
      }
   }

   private ShinyDisconnectSource source_;
}
