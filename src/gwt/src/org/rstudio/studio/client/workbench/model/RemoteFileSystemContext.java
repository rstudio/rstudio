/*
 * RemoteFileSystemContext.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.PosixFileSystemContext;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import java.util.ArrayList;

public class RemoteFileSystemContext extends PosixFileSystemContext
{
   @Inject
   public RemoteFileSystemContext(FilesServerOperations server,
                                  FileTypeRegistry fileTypeRegistry,
                                  GlobalDisplay globalDisplay,
                                  Session session)
   {
      super();
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      globalDisplay_ = globalDisplay;
      session_ = session;
   }

   public MessageDisplay messageDisplay()
   {
      return globalDisplay_;
   }

   public void cd(String relativeOrAbsolutePath)
   {
      // form the fallback path
      String fallbackPath;
      FileSystemItem projectDir = 
            session_.getSessionInfo().getActiveProjectDir();
      if (projectDir == null)
         fallbackPath = session_.getSessionInfo().getDefaultWorkingDir();
      else
         fallbackPath = projectDir.getPath();
      
      cd(relativeOrAbsolutePath, fallbackPath);
   }

   /**
    * Moves into the given directory (relative or absolute) and lists
    * the files in that directory.
    * 
    * @param relativeOrAbsolutePath Path to attempt to enter
    * @param fallbackPath Fallback path to attempt if first path cannot be
    *    entered
    */
   public void cd(String relativeOrAbsolutePath, final String fallbackPath)
   {
      final String newPath = combine(workingDir_, relativeOrAbsolutePath);

      final FileSystemItem newPathEntry = FileSystemItem.createDir(newPath);
      
      final ArrayList<FileSystemItem> fsi = new ArrayList<FileSystemItem>();

      server_.listFiles(
            newPathEntry,
            false, // since this is used for the file dialog don't 
                   // cause the call to reset the server monitoring state
            false, // don't show hidden files
            new ServerRequestCallback<DirectoryListing>()
            {
               @Override
               public void onError(ServerError error)
               {
                  if (fallbackPath != null)
                  {
                     // try fallback if supplied
                     cd(fallbackPath, null);
                  }
                  else if (callbacks_ != null)
                     callbacks_.onError(error.getUserMessage());
               }

               @Override
               public void onResponseReceived(final DirectoryListing response)
               { 
                  final JsArray<FileSystemItem> files = response.getFiles();
                  for (int i = 0; i < files.length(); i++)
                     fsi.add(files.get(i));
                  
                  workingDir_ = newPath;
                  contents_ = fsi.toArray(new FileSystemItem[0]);
                  
                  if (callbacks_ != null)
                     callbacks_.onNavigated();
               }
            });
   }
   
   public void refresh()
   {
      cd(workingDir_);
   }

   public void mkdir(final String directoryName, final ProgressIndicator progress)
   {
      String error;
      if (null != (error = validatePathElement(directoryName, true)))
      {
         progress.onError(error);
         return;
      }

      final String baseDir = workingDir_;
      String newPath = combine(baseDir, directoryName);
      final FileSystemItem newFolder = FileSystemItem.createDir(newPath);
      server_.createFolder(
            newFolder,
            new ServerRequestCallback<org.rstudio.studio.client.server.Void>()
            {
               @Override
               public void onError(ServerError error)
               {
                  progress.onError(error.getUserMessage());
               }

               @Override
               public void onResponseReceived(Void response)
               {
                  if (baseDir == workingDir_)
                  {
                     progress.onCompleted();
                     if (callbacks_ != null)
                        callbacks_.onDirectoryCreated(newFolder);
                  }
               }
            });
   }

   public FileIcon getIcon(FileSystemItem item)
   {
      return fileTypeRegistry_.getIconForFile(item);
   }

   private final FilesServerOperations server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
}
