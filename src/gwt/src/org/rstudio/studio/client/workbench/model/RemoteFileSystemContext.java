/*
 * RemoteFileSystemContext.java
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
package org.rstudio.studio.client.workbench.model;

import java.util.ArrayList;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.PosixFileSystemContext;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;

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
   
   /**
    * A new set of callbacks is a new owner (a new dialog): results of the
    * old owner's pending navigations must not reach it, and its opening
    * navigation may fall back (see cd()).
    */
   @Override
   public void setCallbacks(Callbacks callbacks)
   {
      super.setCallbacks(callbacks);
      navigation_.invalidate();
      openingNavigation_ = true;
   }

   public void setwd(String relativeOrAbsolutePath)
   {
      navigation_.invalidate();
      workingDir_ = combine(pwd(), relativeOrAbsolutePath);
      contents_ = null;
      if (callbacks_ != null)
         callbacks_.onNavigated();
   }

   /**
    * Only a dialog's opening navigation falls back to the project (or
    * default working) directory, so the dialog can still open when the
    * directory it would start in is gone (e.g. R/ in a package without one).
    * A directory the user navigates to later fails outright instead, leaving
    * the dialog where it was: silently listing a fallback there let a Save,
    * Open, or Choose deferred behind the navigation complete in it (#18959).
    */
   public void cd(String relativeOrAbsolutePath)
   {
      String fallbackPath = null;
      if (openingNavigation_)
      {
         FileSystemItem projectDir =
               session_.getSessionInfo().getActiveProjectDir();
         if (projectDir == null)
            fallbackPath = session_.getSessionInfo().getDefaultWorkingDir();
         else
            fallbackPath = projectDir.getPath();
      }
      openingNavigation_ = false;

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
      // Only the latest navigation (or owner) may update the listing or notify
      // the dialog: onNavigated() can release an accept waiting for that
      // directory, and a closed or replaced dialog must get nothing.
      navigation_.invalidate();
      final Invalidation.Token token = navigation_.getInvalidationToken();

      final String newPath = combine(workingDir_, relativeOrAbsolutePath);

      final FileSystemItem newPathEntry = FileSystemItem.createDir(newPath);

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
                  if (token.isInvalid())
                     return;

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
                  if (token.isInvalid())
                     return;

                  final JsArray<FileSystemItem> files = response.getFiles();
                  ArrayList<FileSystemItem> fsi = new ArrayList<>();
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
            new ServerRequestCallback<org.rstudio.studio.client.server.VoidResponse>()
            {
               @Override
               public void onError(ServerError error)
               {
                  progress.onError(error.getUserMessage());
               }

               @Override
               public void onResponseReceived(VoidResponse response)
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

   private final Invalidation navigation_ = new Invalidation();
   private final FilesServerOperations server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;

   // see setCallbacks()
   private boolean openingNavigation_ = true;
}
