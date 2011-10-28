/*
 * NewProjectResult.java
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
package org.rstudio.studio.client.projects.model;

public class NewProjectResult
{
   public NewProjectResult(String projectFile, 
                           String newDefaultProjectLocation,
                           String gitRepoUrl)
   {
      projectFile_ = projectFile;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      gitRepoUrl_ = gitRepoUrl;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public String getNewDefaultProjectLocation()
   {
      return newDefaultProjectLocation_;
   }

   public String getGitRepoUrl()
   {
      return gitRepoUrl_;
   }

   private final String projectFile_;
   private final String newDefaultProjectLocation_;
   private final String gitRepoUrl_;
}
