/*
 * VisualModeUtil.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditorContainer;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

// shared static utility functions for visual mode

public class VisualModeUtil
{
   public static String getEditorCode(TextEditingTarget.Display view)
   {
      TextEditorContainer editorContainer = view.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
      return editor.getCode();
   }
   
   public static boolean isDocInProject(WorkbenchContext workbenchContext, 
                                        DocUpdateSentinel docUpdateSentinel)
   {  
      // if we are in a project
      if (workbenchContext.isProjectActive())
      {
         // if the doc path is  null let's assume it's going to be saved
         // within the current project
         String docPath = docUpdateSentinel.getPath();
         if (docPath != null)
         {
            // if the doc is in the project directory
            FileSystemItem docFile = FileSystemItem.createFile(docPath);
            FileSystemItem projectDir = workbenchContext.getActiveProjectDir();
            return docFile.getPathRelativeTo(projectDir) != null;
         }
         else
         {
            return true;
         }
      }
      else
      {
         return false;
      }
   }
   
}
