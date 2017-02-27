/*
 * ProjectOpener.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.projects;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.impl.WebFileDialogs;
import org.rstudio.studio.client.projects.model.OpenProjectParams;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class ProjectOpener
{
   public final static int PROJECT_TYPE_FILE   = 0;
   public final static int PROJECT_TYPE_SHARED = 1;
   
   private void initialize()
   {
      if (initialized_) return;
      initialized_ = true;
      server_ = RStudioGinjector.INSTANCE.getServer();
   }
   
   public void showOpenProjectDialog(
                  FileSystemContext fsContext,
                  ProjectsServerOperations server,
                  String defaultLocation,
                  int defaultType,
                  boolean showNewSession,
                  final ProgressOperationWithInput<OpenProjectParams> onCompleted)
   {
      initialize();
      
      // use the default dialog on desktop mode or single-session mode
      FileDialogs dialogs = RStudioGinjector.INSTANCE.getFileDialogs();
      if (Desktop.isDesktop() ||
          !RStudioGinjector.INSTANCE.getSession().getSessionInfo()
                                                 .getMultiSession())
      {
         dialogs.openFile(
            "Open Project", 
            fsContext, 
            FileSystemItem.createDir(defaultLocation),
            "R Projects (*.Rproj)",
            true,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               @Override
               public void execute(FileSystemItem input,
                                   final ProgressIndicator indicator)
               {
                  final CommandWithArg<FileSystemItem> onProjectFileReady =
                        new CommandWithArg<FileSystemItem>()
                  {
                     @Override
                     public void execute(FileSystemItem item)
                     {
                        onCompleted.execute(
                              new OpenProjectParams(item, null, false),
                              indicator);
                     }
                  };
                  
                  // null return values here imply a cancellation
                  if (input == null)
                     return;
                  
                  if (input.isDirectory())
                  {
                     final String rprojPath = input.completePath(input.getName() + ".Rproj");
                     final FileSystemItem rprojFile = FileSystemItem.createFile(rprojPath);
                     server_.createProjectFile(
                           rprojFile.getPath(),
                           new ServerRequestCallback<Boolean>()
                           {
                              @Override
                              public void onResponseReceived(Boolean success)
                              {
                                 onProjectFileReady.execute(rprojFile);
                              }

                              @Override
                              public void onError(ServerError error)
                              {
                                 Debug.logError(error);
                                 onProjectFileReady.execute(rprojFile);
                              }
                           });
                  }
                  else
                  {
                     onProjectFileReady.execute(input);
                  }
               }
            });  
      }
      else
      {
         // in multi-session mode, we have a special dialog for opening projects
         WebFileDialogs webDialogs = (WebFileDialogs)dialogs;
         webDialogs.openProject(fsContext, 
               FileSystemItem.createDir(defaultLocation), 
               defaultType, showNewSession, onCompleted);
      }
   }
   
   // Injected ----
   private boolean initialized_;
   private ProjectsServerOperations server_;
}
