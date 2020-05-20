/*
 * FilesUpload.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.FileUploadEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.files.model.FileUploadToken;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;

public class FilesUpload
{
   @Inject
   public FilesUpload(Files.Display display,
                      GlobalDisplay globalDisplay,
                      FilesServerOperations server,
                      EventBus eventBus)
   {
      display_ = display;
      globalDisplay_ = globalDisplay;
      server_ = server;
      eventBus_ = eventBus;
   }
   
   void execute(FileSystemItem targetDirectory, 
                RemoteFileSystemContext fileSystemContext)
   {
      // this can be invoked from the other side of the async shim
      // so make sure we come to the front whenever this is called
      display_.bringToFront();
      
      display_.showFileUpload(
         server_.getFileUploadUrl(),
         targetDirectory,
         fileSystemContext,
         new Operation() {
            public void execute()
            {
               FileUploadEvent event = new FileUploadEvent(true);
               eventBus_.fireEvent(event);
            }
         },
         new OperationWithInput<PendingFileUpload>() {
            public void execute(PendingFileUpload pendingUpload)
            {
               // confirm overwrites if necessary
               FileUploadToken token = pendingUpload.getToken();
               if (pendingUpload.getOverwrites().length() > 0)
               {
                  globalDisplay_.showYesNoMessage(
                       MessageDialog.WARNING, 
                       "Confirm Overwrite",
                       confirmFileUploadOverwriteMessage(pendingUpload), 
                       false, 
                       completeFileUploadOperation(token, true), 
                       completeFileUploadOperation(token, false), 
                       false);
               }
               else
               {
                  completeFileUploadOperation(token, true).execute();
               }
            }                     
        },
        new Operation() {
            public void execute()
            {
               FileUploadEvent event = new FileUploadEvent(false);
               eventBus_.fireEvent(event);
            }
        } );
   }
   

   private String confirmFileUploadOverwriteMessage(
                                              PendingFileUpload pendingUpload)
   {
      JsArray<FileSystemItem> overwrites = pendingUpload.getOverwrites();
      FileSystemItem firstFile = overwrites.get(0);
      boolean multiple = overwrites.length() > 1;
      StringBuilder msg = new StringBuilder();
      msg.append("The upload will overwrite ");
      if (multiple)
         msg.append("multiple files including ");
      else
         msg.append("the file ");
      msg.append("\"" + firstFile.getPath() + "\". ");

      msg.append("Are you sure you want to overwrite ");
      if (multiple)
         msg.append("these files?");
      else
         msg.append("this file?");
      return msg.toString();
   }

   private Operation completeFileUploadOperation(final FileUploadToken token, 
                                                 final boolean commit)
   {
      return new Operation() 
      {
         public void execute()
         {
            String msg = (commit ? "Completing" : "Cancelling") + 
            " file upload...";
            final Command dismissProgress = globalDisplay_.showProgress(msg);

            server_.completeUpload(token, 
                  commit, 
                  new ServerRequestCallback<Void>() {    
               @Override
               public void onResponseReceived(Void response)
               {
                  dismissProgress.execute();

                  FileUploadEvent event = new FileUploadEvent(false);
                  eventBus_.fireEvent(event);
               }

               @Override
               public void onError(ServerError error)
               {
                  dismissProgress.execute();  
                  globalDisplay_.showErrorMessage("File Upload Error",
                        error.getUserMessage());

                  FileUploadEvent event = new FileUploadEvent(false);
                  eventBus_.fireEvent(event);
               }
            });
         }
      };
   }

   private final Files.Display display_;
   private final GlobalDisplay globalDisplay_;
   private final FilesServerOperations server_;
   private final EventBus eventBus_;
}
