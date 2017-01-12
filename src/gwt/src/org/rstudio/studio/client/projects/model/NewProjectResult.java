/*
 * NewProjectResult.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;

public class NewProjectResult
{
   public NewProjectResult(String projectFile, 
                           boolean createGitRepo,
                           boolean usePackrat,
                           String newDefaultProjectLocation,
                           VcsCloneOptions vcsCloneOptions,
                           NewPackageOptions newPackageOptions,
                           NewShinyAppOptions newShinyAppOptions,
                           ProjectTemplateOptions projectTemplateOptions)
   {
      projectFile_ = projectFile;
      createGitRepo_ = createGitRepo;
      usePackrat_ = usePackrat;
      openInNewWindow_ = false;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      vcsCloneOptions_ = vcsCloneOptions;
      newPackageOptions_ = newPackageOptions;
      newShinyAppOptions_ = newShinyAppOptions;
      projectTemplateOptions_ = projectTemplateOptions;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public boolean getCreateGitRepo()
   {
      return createGitRepo_;
   }
   
   public boolean getUsePackrat() 
   {
      return usePackrat_;
   }
   
   public boolean getOpenInNewWindow()
   {
      return openInNewWindow_;
   }
   
   public void setOpenInNewWindow(boolean openInNewWindow)
   {
      openInNewWindow_ = openInNewWindow;
   }
   
   public RVersionSpec getRVersion()
   {
      return rVersion_;
   }
   
   public void setRVersion(RVersionSpec rVersion)
   {
      rVersion_ = rVersion;
   }
   
   public String getNewDefaultProjectLocation()
   {
      return newDefaultProjectLocation_;
   }

   public VcsCloneOptions getVcsCloneOptions()
   {
      return vcsCloneOptions_;
   }
   
   public NewPackageOptions getNewPackageOptions()
   {
      return newPackageOptions_;
   }
   
   public NewShinyAppOptions getNewShinyAppOptions()
   {
      return newShinyAppOptions_;
   }
   
   public ProjectTemplateOptions getProjectTemplateOptions()
   {
      return projectTemplateOptions_;
   }

   private final boolean createGitRepo_;
   private final boolean usePackrat_;
   private boolean openInNewWindow_;
   private RVersionSpec rVersion_;
   private final String projectFile_;
   private final String newDefaultProjectLocation_;
   private final VcsCloneOptions vcsCloneOptions_;
   private final NewPackageOptions newPackageOptions_;
   private final NewShinyAppOptions newShinyAppOptions_;
   private final ProjectTemplateOptions projectTemplateOptions_;
}
