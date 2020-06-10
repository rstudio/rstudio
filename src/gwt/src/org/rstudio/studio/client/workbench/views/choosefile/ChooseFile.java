/*
 * ChooseFile.java
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
package org.rstudio.studio.client.workbench.views.choosefile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.choosefile.events.ChooseFileEvent;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;

@Singleton
public class ChooseFile implements ChooseFileEvent.Handler
{
   @Inject
   public ChooseFile(EventBus events,
                     ChooseFileServerOperations server,
                     RemoteFileSystemContext fsContext,
                     WorkbenchContext workbenchContext,
                     FileDialogs fileDialogs)
   {
      server_ = server;
      fsContext_ = fsContext;
      workbenchContext_ = workbenchContext;
      fileDialogs_ = fileDialogs;

      events.addHandler(ChooseFileEvent.TYPE, this);

   }

   public void onChooseFile(ChooseFileEvent event)
   {
      ProgressOperationWithInput<FileSystemItem> operation = new ProgressOperationWithInput<FileSystemItem>()
      {
         public void execute(FileSystemItem input,
                             ProgressIndicator progress)
         {
            String message, path;
            if (input != null)
            {
               message = "Saving...";
               path = input.getPath();
            }
            else
            {
               message = "Cancelling...";
               path = null;
            }

            progress.onProgress(message);
            server_.chooseFileCompleted(
                  path,
                  new VoidServerRequestCallback(
                        progress));
         }
      };

      if (event.getNewFile())
      {
         fileDialogs_.saveFile(
               "Choose File",
               fsContext_,
               workbenchContext_.getCurrentWorkingDir(),
               "",
               false,
               operation);
      }
      else
      {
         fileDialogs_.openFile(
               "Choose File",
               fsContext_,
               workbenchContext_.getCurrentWorkingDir(),
               operation);
      }
   }

   private final ChooseFileServerOperations server_;
   private final RemoteFileSystemContext fsContext_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;
}
