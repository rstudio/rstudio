/*
 * FilesUpload.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.gwt.core.client.GWT;
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
               FileUploadToken token = pendingUpload.getToken();
               boolean unzipFound = token.getUnzipFound();
               boolean isZip = token.getIsZip();

               // confirm unzip is installed
               if (!unzipFound && isZip)
               {
                  // Warn user unzip is not installed
                  globalDisplay_.showYesNoMessage(
                          MessageDialog.WARNING,
                          constants_.unzipNotFoundCaption(),
                          constants_.unzipNotFoundMessage(),
                          false,
                          checkForFileUploadOverwrite(pendingUpload, token),
                          completeFileUploadOperation(token, false),
                          true
                          );
               }
               else
               {
                  // Upload and warn of overwrites, if any
                  checkForFileUploadOverwrite(pendingUpload, token).execute();
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
      msg.append(constants_.uploadOverwriteMessage());
      if (multiple)
         msg.append(constants_.multipleFilesMessage());
      else
         msg.append(constants_.theFileMessage());
      msg.append("\"" + firstFile.getPath() + "\". ");

      msg.append(constants_.overwriteQuestion());
      if (multiple)
         msg.append(constants_.filesLabel());
      else
         msg.append(constants_.thisFileLabel());
      return msg.toString();
   }

   private Operation completeFileUploadOperation(final FileUploadToken token, 
                                                 final boolean commit)
   {
      return new Operation() 
      {
         public void execute()
         {
            String msg = constants_.fileUploadMessage((commit ? constants_.completingLabel() : constants_.cancellingLabel()));
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
                  globalDisplay_.showErrorMessage(constants_.fileUploadErrorMessage(),
                        error.getUserMessage());

                  FileUploadEvent event = new FileUploadEvent(false);
                  eventBus_.fireEvent(event);
               }
            });
         }
      };
   }

   private Operation checkForFileUploadOverwrite(
           PendingFileUpload pendingUpload,
           FileUploadToken token
   )
   {
      return new Operation() {
         @Override
         public void execute() {
            if (pendingUpload.getOverwrites().length() > 0)
            {
               globalDisplay_.showYesNoMessage(
                       MessageDialog.WARNING,
                       constants_.confirmOverwriteCaption(),
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
      };
   }

   private final Files.Display display_;
   private final GlobalDisplay globalDisplay_;
   private final FilesServerOperations server_;
   private final EventBus eventBus_;
   private static final FilesConstants constants_ = GWT.create(FilesConstants.class);
}
