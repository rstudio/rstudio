/*
 * VersionControlNavigationPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import java.util.ArrayList;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;

public class VersionControlNavigationPage 
            extends WizardNavigationPage<NewProjectInput,NewProjectResult>
{
   public VersionControlNavigationPage()
   {
      super("Version Control", 
            "Checkout a project from a version control repository",
            "Create Project from Version Control",
            new ImageResource2x(NewProjectResources.INSTANCE.projectFromRepositoryIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.projectFromRepositoryIconLarge2x()),
            createPages());
   }

   private static ArrayList<WizardPage<NewProjectInput, NewProjectResult>> createPages()
   {   
      ArrayList<WizardPage<NewProjectInput, NewProjectResult>> pages = new  ArrayList<>();
      
      pages.add(new GitPage());
      pages.add(new SvnPage());
      
      return pages;
   }
}
