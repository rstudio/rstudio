/*
 * ProjectsPreferencesPane.java
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

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

import org.rstudio.studio.client.workbench.prefs.model.ProjectsPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

public class ProjectsPreferencesPane extends PreferencesPane
{
   @Inject
   public ProjectsPreferencesPane(PreferencesDialogResources res)
   {
      res_ = res;

      restoreLastProject_ = new CheckBox("Restore most recently opened project at startup");
      extraSpaced(restoreLastProject_);
      add(restoreLastProject_);
  
      
      restoreLastProject_.setEnabled(false);
   }

   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      // projects prefs
      ProjectsPrefs projectsPrefs = rPrefs.getProjectsPrefs();
      restoreLastProject_.setEnabled(true);
      restoreLastProject_.setValue(projectsPrefs.getRestoreLastProject());
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconProjects();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Projects";
   }

   @Override
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);
     
      // set projects prefs
      ProjectsPrefs projectsPrefs = ProjectsPrefs.create(
                                          restoreLastProject_.getValue());
      rPrefs.setProjectsPrefs(projectsPrefs);
   }

   private final PreferencesDialogResources res_;
   
   private CheckBox restoreLastProject_;
}
