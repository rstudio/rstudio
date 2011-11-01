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
                           VcsCloneOptions vcsCloneOptions)
   {
      projectFile_ = projectFile;
      newDefaultProjectLocation_ = newDefaultProjectLocation;
      vcsCloneOptions_ = vcsCloneOptions;
      openInNewWindow_ = false;
   }
   
   public String getProjectFile()
   {
      return projectFile_;
   }
   
   public String getNewDefaultProjectLocation()
   {
      return newDefaultProjectLocation_;
   }

   public VcsCloneOptions getVcsCloneOptions()
   {
      return vcsCloneOptions_;
   }
   
   public boolean getOpenInNewWindow()
   {
      return openInNewWindow_;
   }
   
   public void setOpenInNewWindow(boolean openInNewWindow)
   {
      openInNewWindow_ = openInNewWindow;
   }

   private final String projectFile_;
   private final String newDefaultProjectLocation_;
   private final VcsCloneOptions vcsCloneOptions_;
   private boolean openInNewWindow_;
}
