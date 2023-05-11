/*
 * QuartoConnection.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer.quarto;


import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class QuartoConnection
{ 
   public QuartoConnection()
   {
      initializeEvents();
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(ApplicationServerOperations server,
                           FileTypeRegistry fileTypeRegistry)
   {
      server_ = server;
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
         int paramsLoc = url.indexOf("?capabilities=");
         if (paramsLoc != -1)
            url = url.substring(0, paramsLoc);
      }
      url_ = url;
      website_ = website;
      srcFile_ = null;
   }
   
   public String getUrl()
   {
      return url_;
   }
   
   public FileSystemItem getSrcFile()
   {
      if (srcFile_ != null)
         return FileSystemItem.createFile(srcFile_);
      else
         return null;
   }
   
   public boolean isWebsite()
   {
      return url_ != null && website_;
   }
   
   public void navigateBack()
   {
      postQuartoMessage("goback", null);
   }
   
   public void navigateForward()
   {
      postQuartoMessage("goforward", null);
   }
   

   public HandlerRegistration addQuartoNavigationHandler(
                                 QuartoNavigationEvent.Handler handler)
   {
      return handlerManager_.addHandler(QuartoNavigationEvent.TYPE, handler);
   }
      
   
   private native void initializeEvents() /*-{  
      var self = this;
      var callback = $entry(function(event) {
         self.@org.rstudio.studio.client.workbench.views.viewer.quarto.QuartoConnection::onMessage(*)(
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
      if (url_ == null || !url_.startsWith(origin))
         return;
        
      // record source and origin in case we want to send messages back
      source_ = source;
      origin_ = origin;
     
      // dispatch message
      if (message.type == "navigate") 
      {
         // record current navigation url
         setQuartoUrl(message.href, website_);
         
         // record current source file
         srcFile_ = StringUtil.isNullOrEmpty(message.file) ? null : message.file;
         
         // let quarto know we can handle devhost requests
         postQuartoMessage("devhost-init", null);
         
         // fire navigation event
         handlerManager_.fireEvent(new QuartoNavigationEvent(message.href, message.file));
        
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
      postQuartoMessage(source_, origin_, type, data);
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
   
   private JavaScriptObject source_ = null;
   private String origin_ = null;
   private String url_ = null;
   private String srcFile_ = null;
   private boolean website_ = false;
   private HandlerManager handlerManager_ = new HandlerManager(this);
   
   
}

