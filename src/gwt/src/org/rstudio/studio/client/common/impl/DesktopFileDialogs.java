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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.FilenameTransform;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;

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

      public void execute(
            final String caption,
            FileSystemContext fsContext,
            FileSystemItem initialFilePath,
            final FilenameTransform transform,
            final ProgressOperationWithInput<FileSystemItem> operation)
      {
         final String dir = initialFilePath == null
                            ? fsContext.pwd()
                            : initialFilePath.getPath();

         DeferredCommand.addCommand(new Command()
         {
            public void execute()
            {
               String file = operation(caption, dir);

               FileSystemItem item =
                     StringUtil.isNullOrEmpty(file)
                     ? null
                     : FileSystemItem.createFile(
                           transform == null ? file
                                             : transform.transform(file));

               operation.execute(item, new NullProgress());
            }
         });
      }
   }

   public void openFile(final String caption,
                        FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         String operation(String caption, String dir)
         {
            return Desktop.getFrame().getOpenFileName(caption, dir);
         }
      }.execute(caption, fsContext, initialFilePath, null, operation);
   }

   public void saveFile(final String caption,
                        FileSystemContext fsContext,
                        final FileSystemItem initialFilePath,
                        FilenameTransform filenameTransform,
                        final ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         String operation(String caption, String dir)
         {
            return Desktop.getFrame().getSaveFileName(caption, dir);
         }
      }.execute(caption,
                fsContext,
                initialFilePath,
                filenameTransform,
                operation);
   }

   public void chooseFolder(String caption,
                            FileSystemContext fsContext,
                            final boolean browseFromCurrentDir,
                            ProgressOperationWithInput<FileSystemItem> operation)
   {
      new FileDialogOperation()
      {
         @Override
         String operation(String caption, String dir)
         {
            return Desktop.getFrame().getExistingDirectory(
                  caption,
                  browseFromCurrentDir ? dir : null);
         }
      }.execute(caption, fsContext, null, null, operation);
   }
}
