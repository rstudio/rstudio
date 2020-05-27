/*
 * RPubsUploader.java
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
package org.rstudio.studio.client.common.rpubs;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

public class RPubsUploader
{
   public RPubsUploader(
         RPubsServerOperations server,
         GlobalDisplay globalDisplay,
         EventBus eventBus,
         String contextId)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      contextId_ = contextId;
   }

   public void performUpload(final String title, final String rmdFile, 
         final String htmlFile, final String uploadId, final boolean modify)
   {
      // set state
      uploadInProgress_ = true;
    
      // do upload
      if (Desktop.hasDesktopFrame())
      {
         performUpload(title, rmdFile, htmlFile, uploadId, null, modify);
      }
      else
      {
         // randomize the name so Firefox doesn't prevent us from reactivating
         // the window programmatically 
         globalDisplay_.openProgressWindow(
               "_rpubs_upload" + (int)(Math.random() * 10000), 
               PROGRESS_MESSAGE, 
               new OperationWithInput<WindowEx>() {

                  @Override
                  public void execute(WindowEx window)
                  {
                     performUpload(title, rmdFile, htmlFile, uploadId,
                                   window, modify);
                  }
               });
      }
      
   }
   
   public void setOnUploadComplete(CommandWithArg<Boolean> cmd)
   {
      onUploadComplete_ = cmd;
   }
   
   public boolean isUploadInProgress()
   {
      return uploadInProgress_;
   }
   
   public void terminateUpload()
   {
      server_.rpubsTerminateUpload(contextId_,
                                   new VoidServerRequestCallback());
      
      if (uploadProgressWindow_ != null)
         uploadProgressWindow_.close();
   }
   
   // Private methods --------------------------------------------------------
   
   private void performUpload(final String title,
                              final String rmdFile,
                              final String htmlFile,
                              final String uploadId,
                              final WindowEx progressWindow,
                              final boolean modify)
   {  
      // record progress window
      uploadProgressWindow_ = progressWindow;
      
      // subscribe to notification of upload completion
      eventRegistrations_.add(
                        eventBus_.addHandler(RPubsUploadStatusEvent.TYPE, 
                        new RPubsUploadStatusEvent.Handler()
      {
         @Override
         public void onRPubsPublishStatus(RPubsUploadStatusEvent event)
         {
            // make sure it applies to our context
            RPubsUploadStatusEvent.Status status = event.getStatus();
            if (status.getContextId() != contextId_)
               return;
            
            uploadInProgress_ = false;
            onUploadComplete(true);

            if (!StringUtil.isNullOrEmpty(status.getError()))
            {
               if (progressWindow != null)
                  progressWindow.close();
               
               new ConsoleProgressDialog("Upload Error Occurred", 
                     status.getError(),
                     1).showModal();
            }
            else
            {
               if (progressWindow != null)
               {
                  progressWindow.replaceLocationHref(status.getContinueUrl());
               }
               else
               {
                  globalDisplay_.openWindow(status.getContinueUrl());
               }
            }

         }
      }));
      
      // initiate the upload
      server_.rpubsUpload(
         contextId_,
         title, 
         rmdFile == null ? "" : rmdFile,
         htmlFile,
         uploadId == null ? "" : uploadId,
         modify,
         new ServerRequestCallback<Boolean>() {
            @Override
            public void onResponseReceived(Boolean response)
            {
               if (!response.booleanValue())
               {
                  onUploadComplete(false);
                  globalDisplay_.showErrorMessage(
                         "Error",
                         "Unable to continue " +
                         "(another publish is currently running)");
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               onUploadComplete(false);
               globalDisplay_.showErrorMessage("Error",
                                               error.getUserMessage());
            }
        });
      
   }
   
   private void onUploadComplete(boolean succeeded)
   {
      if (onUploadComplete_ != null)
         onUploadComplete_.execute(succeeded);
      eventRegistrations_.removeHandler();
   }
   
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final RPubsServerOperations server_;
   
   private boolean uploadInProgress_;
   private String contextId_ = "";
   private WindowEx uploadProgressWindow_ = null;
   private CommandWithArg<Boolean> onUploadComplete_ = null;
   
   public static final String PROGRESS_MESSAGE = 
         "Uploading document to RPubs...";
   private HandlerRegistrations eventRegistrations_ = 
         new HandlerRegistrations();
}
