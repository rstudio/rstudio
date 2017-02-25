/*
 * WebFileDialogs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.filedialog.ChooseFolderDialog2;
import org.rstudio.core.client.files.filedialog.FileDialog;
import org.rstudio.core.client.files.filedialog.OpenFileDialog;
import org.rstudio.core.client.files.filedialog.OpenProjectDialog;
import org.rstudio.core.client.files.filedialog.SaveFileDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.projects.model.OpenProjectParams;

public class WebFileDialogs implements FileDialogs
{
   public void openFile(String caption,
                        FileSystemContext fsContext,
                        FileSystemItem initialFilePath,
                        ProgressOperationWithInput<FileSystemItem> operation)
   {
      openFile(caption, fsContext, initialFilePath, "", false, operation);
   }
   
   public void openFile(String caption,
                        FileSystemContext fsContext,
                        FileSystemItem initialFilePath,
                        String filter,
                        ProgressOperationWithInput<FileSystemItem> operation)
   {
      openFile(caption, fsContext, initialFilePath, filter, false, operation);
   }
   
   
   public void openFile(String caption,
                        FileSystemContext fsContext,
                        FileSystemItem initialFilePath,
                        String filter,
                        boolean canChooseDirectories,
                        ProgressOperationWithInput<FileSystemItem> operation)
   {
      OpenFileDialog dialog = new OpenFileDialog(caption,
                                                 fsContext,
                                                 filter,
                                                 canChooseDirectories,
                                                 operation);
      
      dialog.setInvokeOperationEvenOnCancel(true);

      finishInit(fsContext, initialFilePath, dialog);
   }
   
   public void openProject(FileSystemContext fsContext,
                           FileSystemItem initialFilePath,
                           int defaultType, 
                           boolean showNewSession,
                           ProgressOperationWithInput<OpenProjectParams> operation)
   {
      OpenProjectDialog dialog = new OpenProjectDialog(fsContext, defaultType,
            showNewSession, operation);

      dialog.setInvokeOperationEvenOnCancel(true);
      
      finishInit(fsContext, initialFilePath, dialog);
   }

   public void saveFile(String caption,
                        FileSystemContext fsContext,
                        FileSystemItem initialFilePath,
                        String defaultExtension,
                        boolean forceDefaultExtension,
                        ProgressOperationWithInput<FileSystemItem> operation)
   {
      SaveFileDialog dialog = new SaveFileDialog(caption,
                                                 fsContext,
                                                 defaultExtension,
                                                 forceDefaultExtension,
                                                 operation);

      dialog.setInvokeOperationEvenOnCancel(true);

      finishInit(fsContext, initialFilePath, dialog);
   }

   public void chooseFolder(String caption,
                            FileSystemContext fsContext,
                            FileSystemItem initialDir,
                            ProgressOperationWithInput<FileSystemItem> operation)
   {
      ChooseFolderDialog2 dialog = new ChooseFolderDialog2(caption,
                                                           fsContext,
                                                           true,
                                                           operation);
      dialog.setInvokeOperationEvenOnCancel(true);
      if (initialDir != null)
         fsContext.cd(initialDir.getPath());
      else
         fsContext.refresh();
      dialog.showModal();
   }

   private void finishInit(FileSystemContext fsContext,
                           FileSystemItem initialFilePath,
                           FileDialog dialog)
   {
      if (initialFilePath != null)
      {
         if (initialFilePath.isDirectory())
         {
            fsContext.cd(initialFilePath.getPath());
         }
         else
         {
            dialog.setFilename(initialFilePath.getName());
            fsContext.cd(initialFilePath.getParentPathString());
         }
      }
      else
      {
         fsContext.refresh();
      }
      dialog.showModal();
   }
}
