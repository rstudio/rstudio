/*
 * NewProjectResult.java
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
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.TutorialApiCallContext;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;

public class NewProjectResult
{
   public NewProjectResult(String projectFile, 
                           boolean createGitRepo,
                           boolean useRenv,
                           String newDefaultProjectLocation,
                           VcsCloneOptions vcsCloneOptions,
                           NewPackageOptions newPackageOptions,
                           NewShinyAppOptions newShinyAppOptions,
                           ProjectTemplateOptions projectTemplateOptions,
                           TutorialApiCallContext callContext)
   {
      projectFile_ = projectFile;
      createGitRepo_ = createGitRepo;
      useRenv_ = useRenv;
      openInNewWindow_ = false;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      vcsCloneOptions_ = vcsCloneOptions;
      newPackageOptions_ = newPackageOptions;
      newShinyAppOptions_ = newShinyAppOptions;
      projectTemplateOptions_ = projectTemplateOptions;
      callContext_ = callContext;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public void setProjectFile(String projectFile)
   {
      projectFile_ = projectFile;
   }
   
   public boolean getCreateGitRepo()
   {
      return createGitRepo_;
   }
   
   public boolean getUseRenv() 
   {
      return useRenv_;
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

   /**
    * @return If invoked via Tutorial Api, context info about the call, otherwise null
    */
   public TutorialApiCallContext getCallContext()
   {
      return callContext_;
   }

   private final boolean createGitRepo_;
   private final boolean useRenv_;
   private boolean openInNewWindow_;
   private RVersionSpec rVersion_;
   private String projectFile_;
   private final String newDefaultProjectLocation_;
   private final VcsCloneOptions vcsCloneOptions_;
   private final NewPackageOptions newPackageOptions_;
   private final NewShinyAppOptions newShinyAppOptions_;
   private final ProjectTemplateOptions projectTemplateOptions_;
   private final TutorialApiCallContext callContext_;
}
