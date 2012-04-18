/*
 * NewProjectWizard.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class NewProjectWizard extends Wizard<FileSystemItem,NewProjectResult>
{
   public NewProjectWizard(
         SessionInfo sessionInfo,
         UIPrefs uiPrefs,
         FileSystemItem defaultNewProjectLocation,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super("New Project", 
            "Create project from:", 
            defaultNewProjectLocation, 
            operation);
    
      setOkButtonCaption("Create Project");
      
      addPage(new NewDirectoryPage());
      addPage(new ExistingDirectoryPage());
      addPage(new VersionControlNavigationPage(sessionInfo));
   }
}
