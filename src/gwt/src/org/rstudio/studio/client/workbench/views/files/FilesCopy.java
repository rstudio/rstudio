/*
 * FilesCopy.java
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

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import java.util.ArrayList;

public class FilesCopy
{
   @Inject
   public FilesCopy(FilesServerOperations server,
                    GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
    
   public void execute(ArrayList<FileSystemItem> files, 
                       FileSystemItem targetDirectory,
                       Command completedCommand)
   {
      // copy list so we don't modify passed list
      ArrayList<FileSystemItem> filesQueue = new ArrayList<FileSystemItem>(
                                                               files);
      
      // begin copy sequence (this method keeps calling itself until
      // the queue of files is empty)
      copyNextFile(filesQueue, targetDirectory, completedCommand);
   }
   
   private void copyNextFile(final ArrayList<FileSystemItem> filesQueue, 
                             final FileSystemItem targetDirectory,
                             final Command completedCommand)
   {
      // terminate if there are no files left
      if (filesQueue.size() == 0)
      {
         if (completedCommand != null)
            completedCommand.execute();

         return;
      }
      
      // remove the first file from the list
      final FileSystemItem sourceFile = filesQueue.remove(0);
      
      // determine the default name and default selection
      final String COPY_PREFIX = "CopyOf";
      String defaultName = COPY_PREFIX + sourceFile.getName();
      int defaultSelectionLength = COPY_PREFIX.length()
                                   + sourceFile.getStem().length();
      
      // show prompt for new filename
      final String objectName = sourceFile.isDirectory() ? "Folder" : "File";
      globalDisplay_.promptForText(
             "Copy " + objectName, 
             "Enter a name for the copy of '" + sourceFile.getName() + "':",
             defaultName,
             0,
             defaultSelectionLength,
             null,
             new ProgressOperationWithInput<String>() {

         public void execute(String input, ProgressIndicator progress)
         {
            progress.onProgress("Copying " + objectName.toLowerCase() + "...");

            String targetFilePath = targetDirectory.completePath(input);
            final FileSystemItem targetFile = FileSystemItem.createFile(
                                                            targetFilePath);

            server_.copyFile(sourceFile,
                             targetFile,
                             false,
                             new VoidServerRequestCallback(progress) {
                                 @Override
                                 protected void onSuccess()
                                 {
                                    // copy the next file in the queue
                                    copyNextFile(filesQueue,
                                                 targetDirectory,
                                                 completedCommand);
                                 }
            });      
         }                                
      });        
   }
   
   private final FilesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
}
