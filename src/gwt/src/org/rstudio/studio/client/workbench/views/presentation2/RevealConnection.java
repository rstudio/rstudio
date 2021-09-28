/*
 * RevealConnection.java
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
package org.rstudio.studio.client.workbench.views.presentation2;


import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerManager;
import com.google.inject.Inject;

public class RevealConnection
{ 
   public RevealConnection()
   {
      initializeEvents();
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(ApplicationServerOperations server)
   {
      server_ = server;
   }
   
   
   public void setUrl(String url)
   {      
      if (url != null)
      {
         // provide base url if it's relative
         if (!url.startsWith("http"))
            url = server_.getApplicationURL(url);
      }
      url_ = url;
      source_ = null;
      origin_ = null;
   }
   
   
   public void next()
   {
      postRevealMessage("next");
   }
   
   public void previous()
   {
      postRevealMessage("previous");
   }
   
   public void reload()
   {
      postRevealMessage("reload");
   }
   

   private native void initializeEvents() /*-{  
      var self = this;
      var callback = $entry(function(event) {
         self.@org.rstudio.studio.client.workbench.views.presentation2.RevealConnection::onMessage(*)(
            event.source,
            event.origin,
            event.data
         );
      });
      $wnd.addEventListener("message", callback, true);
      
   }-*/;

   private void onMessage(JavaScriptObject source, String origin, RevealMessage message) 
   {    
      // check to see if the message originated from the same origin
      if (url_ == null || !url_.startsWith(origin))
         return;
        
      // record source and origin in case we want to send messages back
      source_ = source;
      origin_ = origin;
      
      final String kRevealMessagePrefix = "reveal-";
      if (message.getMessage().startsWith(kRevealMessagePrefix))
      {
         String type = message.getMessage().replaceFirst(kRevealMessagePrefix, "");
         Debug.logToConsole(type);
         
      }
    
   }
   
   private void postRevealMessage(String type)
   {
      postRevealMessage(type, null);
   }
   
   private void postRevealMessage(String type, JavaScriptObject data)
   {
      postRevealMessage(source_, origin_, type, data);
   }
   
   private native static void postRevealMessage(JavaScriptObject source, String origin, 
                                                String message, JavaScriptObject data) /*-{
      source.postMessage({
         message: "reveal-" + message,
         data: data
      }, origin);
   }-*/;
   
   
   private ApplicationServerOperations server_;
  
   private JavaScriptObject source_ = null;
   private String origin_ = null;
   private String url_ = null;
   private HandlerManager handlerManager_ = new HandlerManager(this);
   
   
}

