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
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;

public class ProjectOpener
{
   public void showOpenProjectDialog(
                  FileSystemContext fsContext,
                  String defaultLocation,
                  ProgressOperationWithInput<FileSystemItem> onCompleted)
   {
      // choose project file
      RStudioGinjector.INSTANCE.getFileDialogs().openFile(
         "Open Project", 
         fsContext, 
         FileSystemItem.createDir(defaultLocation),
         "R Projects (*.Rproj)",
         onCompleted);  
   }
}
