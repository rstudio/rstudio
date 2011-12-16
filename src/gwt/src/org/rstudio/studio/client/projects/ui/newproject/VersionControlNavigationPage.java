/*
 * VersionControlNavigationPage.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class VersionControlNavigationPage 
            extends WizardNavigationPage<FileSystemItem,NewProjectResult>
{

   public VersionControlNavigationPage()
   {
      super("Version Control", 
            "Checkout a project from a version control repository",
            "Create Project from Version Control",
            NewProjectResources.INSTANCE.projectFromRepositoryIcon(),
            NewProjectResources.INSTANCE.projectFromRepositoryIconLarge(),
            createPages());
   }

  
   private static ArrayList<WizardPage<FileSystemItem, NewProjectResult>>
                                                                  createPages()
   {
      SessionInfo sessionInfo = 
                     RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      
      ArrayList<WizardPage<FileSystemItem, NewProjectResult>> pages = 
            new  ArrayList<WizardPage<FileSystemItem, NewProjectResult>>();
      pages.add(new GitPage(sessionInfo.isVcsAvailable(VCSConstants.GIT_ID)));
      if (sessionInfo.isSvnFeatureEnabled())
         pages.add(new SvnPage(sessionInfo.isVcsAvailable(VCSConstants.SVN_ID)));
      return pages;
   }
   

}
