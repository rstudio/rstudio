/*
 * FileExport.java
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

package org.rstudio.studio.client.common.fileexport;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import com.google.inject.Inject;

public class FileExport
{
   public FileExport()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   FilesServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   public void export(String caption, String description, FileSystemItem file)
   {
      ArrayList<FileSystemItem> files = new ArrayList<FileSystemItem>();
      files.add(file);
      export(caption, description, null, files);
   }
   
   public void export(String caption,
                      String description,
                      final FileSystemItem parentDir,
                      final ArrayList<FileSystemItem> files)
   {
      // validation: some files provided
      if  (files.size() == 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, "No Files Selected",
               "Please select one or more files to export.");
         return;
      }
         
      // case: single file which is not a folder 
      if ((files.size()) == 1 && !files.get(0).isDirectory())
      {
         final FileSystemItem file = files.get(0);
         
         showFileExport(caption,
                        description,
                        file.getStem(),
                        file.getExtension(),     
                        new ProgressOperationWithInput<String>(){
            public void execute(String name, ProgressIndicator progress)
            {
               // progress complete
               progress.onCompleted();
               
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(name, file));
               
            }
         });
      }
      
      // case: folder or multiple files
      else
      {
         // determine the default zip file name based on the selection
         String defaultArchiveName;
         if (files.size() == 1)
            defaultArchiveName = files.get(0).getStem();
         else
            defaultArchiveName = "rstudio-export";
         
         // prompt user
         final String ZIP = ".zip";
         showFileExport(caption,
                        description,
                        defaultArchiveName,
                        ZIP,
                        new ProgressOperationWithInput<String>(){
            
            public void execute(String archiveName, ProgressIndicator progress)
            {
               // progress complete
               progress.onCompleted();
               
               // force zip extension in case the user deleted it
               if (!archiveName.endsWith(ZIP))
                  archiveName += ZIP;
               
               // build list of filenames
               ArrayList<String> filenames = new ArrayList<String>();
               for (FileSystemItem file : files)
                  filenames.add(file.getName());
               
               // execute the download (open in a new window)
               globalDisplay_.openWindow(server_.getFileExportUrl(archiveName, 
                                                                  parentDir, 
                                                                  filenames));
            }
         });
         
      }
   }
   
   private void showFileExport(String caption,
                               String description,
                               String defaultName,
                               String defaultExtension,
                               ProgressOperationWithInput<String> operation)
   {
      globalDisplay_.promptForText(
            caption,
            "The " + description + " will be downloaded to your " +
            "computer. Please specify a name for the downloaded file:",
            defaultName + defaultExtension,
            -1, -1,
            "Download",
             operation);
   }
   
   private GlobalDisplay globalDisplay_;
   private FilesServerOperations server_;
}
