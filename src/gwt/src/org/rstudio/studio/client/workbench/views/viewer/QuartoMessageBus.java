/*
 * QuartoMessageBus.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.viewer;


import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;

import jsinterop.annotations.JsType;


public class QuartoMessageBus
{
   @JsType
   public static class QuartoMessage
   {
      public String type;
      public String href;
   }


   public QuartoMessageBus()
   {
      initializeEvents();
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(ApplicationServerOperations server)
   {
      server_ = server;
   }
   
   public void setQuartoUrl(String url)
   {
      if (url != null)
      {
         // provide base url if it's relative
         if (!url.startsWith("http"))
            url = server_.getApplicationURL(url);
         
         // clean out viewer_pane params
         int paramsLoc = url.indexOf("?viewer_pane=");
         if (paramsLoc != -1)
            url = url.substring(0, paramsLoc);
      }
      quartoUrl_ = url;
   }
   
   public String getQuartoUrl()
   {
      return quartoUrl_;
   }

   
   private native void initializeEvents() /*-{  
      var self = this;
      var callback = $entry(function(event) {
         self.@org.rstudio.studio.client.workbench.views.viewer.QuartoMessageBus::onMessage(*)(
            event.source,
            event.origin,
            event.data
         );
      });
      $wnd.addEventListener("message", callback, true);
      
   }-*/;

   private void onMessage(JavaScriptObject source, String origin, QuartoMessage message) 
   {
      // check to see if the message originated from the same origin
      if (quartoUrl_ == null || !quartoUrl_.startsWith(origin))
         return;
        
      // record source and origin in case we want to send messages back
      quartoSource_ = source;
      quartoOrigin_ = origin;
     
      // dispatch message
      if (message.type == "navigate") {
         setQuartoUrl(message.href);
      } 
   }
   
   private ApplicationServerOperations server_;
   
   private JavaScriptObject quartoSource_ = null;
   private String quartoOrigin_ = null;
   private String quartoUrl_ = null;
   
   
}

