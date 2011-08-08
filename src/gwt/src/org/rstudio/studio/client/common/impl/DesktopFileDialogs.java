/*
 * DesktopFileDialogs.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

public class DesktopFileDialogs implements FileDialogs
{
   private class NullProgress implements ProgressIndicator
   {
      public void onProgress(String message)
      {
      }

      public void onCompleted()
      {
      }

      public void onError(String message)
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage("Error",
                                                                       message);
      }
   }

   private abstract class FileDialogOperation
   {
      abstract String operation(String caption, String dir);

      protected boolean shouldUpdateDetails()
      {
         return false;
      }

      public void execute(
            final String caption,
            FileSystemContext fsContext,
            FileSystemItem initialFilePath,
            final ProgressOperationWithInput<FileSystemItem> operation)
      {
         final String dir = initialFilePath == null
                            ? fsContext.pwd()
                            : initialFilePath.getPath();

         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               String file = operation(caption, dir);

               FileSystemItem item =
                     StringUtil.isNullOrEmpty(file)
                     ? null
                     : FileSystemItem.createFile(file);

               if (item != null && shouldUpdateDetails())
               {
                  server_.stat(item.getPath(), new ServerRequestCallback<FileSystemItem>()
                  {
                     @Override
                     public void onResponseReceived(FileSystemItem response)
                     {
                        operation.execute(response, new NullProgress());
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        globalDisplay_.showErrorMessage("Error",
                                                        error.getUserMessage());
                        operation.execute(null, new NullProgress());
                     }
                  });
               }
               else
               {
                  operation.execute(item, new NullProgress());
               }
            }
         });
      }
   }

   public DesktopFileDialogs()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(FilesServerOperations server,
                          GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }

   public void openFile(final String caption,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         protected boolean shouldUpdateDetails()
         {
            return true;
         }

         @Override
         String operation(String caption, String dir)
         {
            String fileName = Desktop.getFrame().getOpenFileName(caption, dir);
            if (fileName != null)
            {
               updateWorkingDirectory(fileName, fsContext);
            }
            return fileName;
         }
      }.execute(caption, fsContext, initialFilePath, operation);
   }

   public void saveFile(final String caption,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String defaultExtension,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         String operation(String caption, String dir)
         {
            String fileName = Desktop.getFrame().getSaveFileName(
                  caption, dir, defaultExtension);

            if (fileName != null)
            {
               updateWorkingDirectory(fileName, fsContext);
            }
            return fileName;
         }
      }.execute(caption,
                fsContext,
                initialFilePath,
                operation);
   }

   public void chooseFolder(String caption,
                            FileSystemContext fsContext,
                            final FileSystemItem initialDir,
                            ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         String operation(String caption, String dir)
         {
            return Desktop.getFrame().getExistingDirectory(
                  caption,
                  initialDir != null ? initialDir.getPath() : null);
         }
      }.execute(caption, fsContext, null, operation);
   }

   private void updateWorkingDirectory(String fileName,
                                       FileSystemContext fsContext)
   {
      if (fileName != null)
      {
         String parentPath =
               FileSystemItem.createFile(fileName).getParentPathString();
         if (!StringUtil.isNullOrEmpty(parentPath))
            fsContext.cd(parentPath);
      }
   }

   private FilesServerOperations server_;
   private GlobalDisplay globalDisplay_;
}
