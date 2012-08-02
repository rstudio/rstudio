/*
 * NewProjectResult.java
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
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.common.vcs.VcsCloneOptions;

public class NewProjectResult
{
   public NewProjectResult(String projectFile, 
                           boolean createGitRepo,
                           String newDefaultProjectLocation,
                           VcsCloneOptions vcsCloneOptions,
                           NewPackageOptions newPackageOptions)
   {
      projectFile_ = projectFile;
      createGitRepo_ = createGitRepo;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      vcsCloneOptions_ = vcsCloneOptions;
      newPackageOptions_ = newPackageOptions;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public boolean getCreateGitRepo()
   {
      return createGitRepo_;
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
   
   
   private final boolean createGitRepo_;
   private final String projectFile_;
   private final String newDefaultProjectLocation_;
   private final VcsCloneOptions vcsCloneOptions_;
   private final NewPackageOptions newPackageOptions_;
}
