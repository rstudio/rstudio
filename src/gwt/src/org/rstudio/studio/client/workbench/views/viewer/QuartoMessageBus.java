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


import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Provider;

import jsinterop.annotations.JsType;


public class QuartoMessageBus
{
   @JsType
   public static class QuartoMessage
   {
      public String type;
      public String href;
      public String file;
      public int line;
      public int column;
      public boolean highlight;
   }

   public QuartoMessageBus()
   {
      initializeEvents();
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(ApplicationServerOperations server,
                           Provider<UserState> pUserState,
                           FileTypeRegistry fileTypeRegistry)
   {
      server_ = server;
      pUserState_ = pUserState;
      fileTypeRegistry_ = fileTypeRegistry;
   }
   
   public void setQuartoUrl(String url, boolean website)
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
      quartoWebsite_ = website;
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
      if (message.type == "navigate") 
      {
         // record current navigation url
         setQuartoUrl(message.href, quartoWebsite_);
         
         // let quarto know we can handle devhost requests
         postQuartoMessage("devhost-init", null);
        
      } 
      else if (message.type == "navigate-src") 
      {
         if (quartoWebsite_ &&
             pUserState_.get().quartoWebsiteSyncEditor().getValue())
         {
            fileTypeRegistry_.editFile(FileSystemItem.createFile(message.file));
         }
      } 
      else if (message.type == "openfile") 
      {
         fileTypeRegistry_.editFile(
           FileSystemItem.createFile(message.file),
           FilePosition.create(message.line, message.column),
           message.highlight
         );
      }
   }
   
   private void postQuartoMessage(String type, JavaScriptObject data)
   {
      postQuartoMessage(quartoSource_, quartoOrigin_, type, data);
   }
   
   private native static void postQuartoMessage(JavaScriptObject source, String origin, 
                                                String type, JavaScriptObject data) /*-{
      source.postMessage({
         type: type,
         data: data
      }, origin);
   }-*/;
   
   private ApplicationServerOperations server_;
   private Provider<UserState> pUserState_;
   private FileTypeRegistry fileTypeRegistry_;
   
   private JavaScriptObject quartoSource_ = null;
   private String quartoOrigin_ = null;
   private String quartoUrl_ = null;
   private boolean quartoWebsite_ = false;
   
   
}

