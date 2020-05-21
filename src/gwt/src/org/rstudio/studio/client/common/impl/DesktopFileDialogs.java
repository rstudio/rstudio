/*
 * DesktopFileDialogs.java
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
package org.rstudio.studio.client.common.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.NullProgressIndicator;
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
   private abstract class FileDialogOperation
   {
      abstract void operation(String caption,
                              String dir,
                              CommandWithArg<String> onCompleted);

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
               operation(
                     caption,
                     dir,
                     new CommandWithArg<String>()
                     {
                        @Override
                        public void execute(String result)
                        {
                           onOperationCompleted(result, operation);
                        }
                     });
            }
         });
      }
      
      private void onOperationCompleted(
            String file,
            ProgressOperationWithInput<FileSystemItem> operation)
      {
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
                  operation.execute(response, new NullProgressIndicator());
               }

               @Override
               public void onError(ServerError error)
               {
                  globalDisplay_.showErrorMessage("Error",
                        error.getUserMessage());
                  operation.execute(null, new NullProgressIndicator());
               }
            });
         }
         else
         {
            operation.execute(item, new NullProgressIndicator());
         }
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
      openFile(caption, fsContext, initialFilePath, "", false, operation);
   }
   
   public void openFile(final String caption,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String filter,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      openFile(caption, fsContext, initialFilePath, filter, false, operation);
   }
   
   public void openFile(final String caption,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String filter,
                        final boolean canChooseDirectories,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      openFile(caption, "Open", fsContext, initialFilePath, filter, canChooseDirectories, operation);
   }
   
   public void openFile(final String caption,
                        final String label,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String filter,
                        final boolean canChooseDirectories,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      openFile(caption, label, fsContext, initialFilePath, filter, canChooseDirectories, true, operation);
   }
   
   public void openFile(final String caption,
                        final String label,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String filter,
                        final boolean canChooseDirectories,
                        final boolean focusOpener,
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
         void operation(final String caption,
                        final String dir,
                        final CommandWithArg<String> onCompleted)
         {
            Desktop.getFrame().getOpenFileName(
                  StringUtil.notNull(caption),
                  StringUtil.notNull(label),
                  StringUtil.notNull(dir),
                  StringUtil.notNull(filter),
                  canChooseDirectories,
                  focusOpener,
                  fileName -> 
                  {
                     if (fileName != null)
                     {
                        updateWorkingDirectory(fileName, fsContext);
                     }
                     onCompleted.execute(fileName);
                  });
            
         }
      }.execute(caption, fsContext, initialFilePath, operation);
   }

   public void saveFile(final String caption,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String defaultExtension,
                        final boolean forceDefaultExtension,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      saveFile(caption, "Save", fsContext, initialFilePath, defaultExtension, forceDefaultExtension, operation);
   }
   
   public void saveFile(final String caption,
                        final String buttonLabel,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String defaultExtension,
                        final boolean forceDefaultExtension,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      saveFile(caption, buttonLabel, fsContext, initialFilePath, defaultExtension, forceDefaultExtension, true, operation);
   }
   
   public void saveFile(final String caption,
                        final String buttonLabel,
                        final FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final String defaultExtension,
                        final boolean forceDefaultExtension,
                        final boolean focusOwner,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         void operation(final String caption,
                        final String dir,
                        final CommandWithArg<String> onCompleted)
         {
            Desktop.getFrame().getSaveFileName(
                  StringUtil.notNull(caption),
                  StringUtil.notNull(buttonLabel),
                  StringUtil.notNull(dir),
                  StringUtil.notNull(defaultExtension),
                  forceDefaultExtension,
                  focusOwner,
                  fileName ->
                  {
                     if (fileName != null)
                     {
                        updateWorkingDirectory(fileName, fsContext);
                     }
                     onCompleted.execute(fileName);
                  });
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
      chooseFolder(caption, "Open", fsContext, initialDir, operation);
   }
   
   public void chooseFolder(final String caption,
                            final String label,
                            final FileSystemContext fsContext,
                            final FileSystemItem initialDir,
                            ProgressOperationWithInput<FileSystemItem> operation)
   {
      chooseFolder(caption, label, fsContext, initialDir, true, operation);
   }
   
   public void chooseFolder(final String caption,
                            final String label,
                            final FileSystemContext fsContext,
                            final FileSystemItem initialDir,
                            final boolean focusOwner,
                            ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         void operation(final String caption,
                        final String dir,
                        final CommandWithArg<String> onCompleted)
         {
            Desktop.getFrame().getExistingDirectory(
                  StringUtil.notNull(caption),
                  StringUtil.notNull(label),
                  initialDir != null ? StringUtil.notNull(initialDir.getPath()) : "",
                  focusOwner,
                  directory -> 
                  {
                     onCompleted.execute(directory);
                  });
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
