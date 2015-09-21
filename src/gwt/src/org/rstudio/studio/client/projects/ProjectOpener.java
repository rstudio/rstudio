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

public class ProjectOpener
{
   public final static int PROJECT_TYPE_FILE   = 0;
   public final static int PROJECT_TYPE_SHARED = 1;

   public void showOpenProjectDialog(
                  FileSystemContext fsContext,
                  ProjectsServerOperations server,
                  String defaultLocation,
                  int defaultType,
                  final ProgressOperationWithInput<OpenProjectParams> onCompleted)
   {
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
            new ProgressOperationWithInput<FileSystemItem>()
            {
               @Override
               public void execute(FileSystemItem input,
                     ProgressIndicator indicator)
               {
                  onCompleted.execute(new OpenProjectParams(input, false), 
                        indicator);
               }
            });  
      }
      else
      {
         // in multi-session mode, we have a special dialog for opening projects
         WebFileDialogs webDialogs = (WebFileDialogs)dialogs;
         webDialogs.openProject(fsContext, 
               FileSystemItem.createDir(defaultLocation), 
               defaultType, onCompleted);
      }
   }
}
