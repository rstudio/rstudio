/*
 * RPubsPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.htmlpreview;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.htmlpreview.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.inject.Inject;

public class RPubsPresenter
{
   public interface Binder extends CommandBinder<Commands, RPubsPresenter>
   {}
   
   public interface Context
   {
      String getTitle();
      String getHtmlFile();
   }
   
   @Inject
   public RPubsPresenter(Binder binder,
                         Commands commands,
                         final GlobalDisplay globalDisplay,
                         EventBus eventBus,
                         HTMLPreviewServerOperations server)
   {
      binder.bind(commands, this);  
      
      server_ = server;
      progressIndicator_ = globalDisplay.getProgressIndicator("Error");
      
      eventBus.addHandler(RPubsUploadStatusEvent.TYPE, 
                          new RPubsUploadStatusEvent.Handler()
      {
         @Override
         public void onRPubsPublishStatus(RPubsUploadStatusEvent event)
         {
            progressIndicator_.clearProgress();
            
            RPubsUploadStatusEvent.Status status = event.getStatus();
            if (!StringUtil.isNullOrEmpty(status.getError()))
            {
               new ConsoleProgressDialog("Upload Error Occurred", 
                                         status.getError(),
                                         1).showModal();
            }
            else
            {
               NewWindowOptions options = new NewWindowOptions();
               options.setAlwaysUseBrowser(true);
               globalDisplay.openWindow(status.getContinueUrl(), options);
            }
            
         }
      });
   }
   
   public void setContext(Context context)
   {
      context_ = context;
   }
   
   @Handler
   public void onPublishHTML()
   {
      progressIndicator_.onProgress("Preparing to publish document...");
      
      server_.rpubsUpload(
            context_.getTitle(), 
            context_.getHtmlFile(), 
            new ServerRequestCallback<Boolean>() {

               @Override
               public void onResponseReceived(Boolean response)
               {
                  if (!response.booleanValue())
                  {
                     progressIndicator_.onError(
                      "Unable to publish (another publish is already running)");
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  progressIndicator_.onError(error.getUserMessage());
               }
      });
   }
   
   private Context context_;
   private final HTMLPreviewServerOperations server_;
   private final ProgressIndicator progressIndicator_;
}
