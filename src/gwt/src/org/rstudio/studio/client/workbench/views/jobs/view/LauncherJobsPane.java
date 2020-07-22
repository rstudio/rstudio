/*
 * LauncherJobsPane.java
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
package org.rstudio.studio.client.workbench.views.jobs.view;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.jobs.LauncherJobsPresenter;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;

import java.util.List;

public class LauncherJobsPane extends WorkbenchPane
                              implements LauncherJobsPresenter.Display
{
   @Inject
   public LauncherJobsPane(UserPrefs uiPrefs,
                           LauncherJobsPaneWidgets widgets)
   {
      super("Launcher");
      
      uiPrefs_ = uiPrefs;
      widgets_ = widgets;
      
      // create widget
      ensureWidget();
      
      // defer most behavior to a shared implementation
      baseImpl_ = new JobsDisplayImpl(this, widgets_);
   }

   @Override
   protected Widget createMainWidget()
   {
      return widgets_.createMainWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      return widgets_.getToolbar();
   }

   @Override
   public void updateJob(int updateType, Job job)
   {
      baseImpl_.updateJob(updateType, job);
   }
   
   @Override
   public void setInitialJobs(List<Job> jobs)
   {
      baseImpl_.setInitialJobs(jobs);
   }

   @Override
   public void showJobOutput(String id, JsArray<JobOutput> output, boolean animate)
   {
      baseImpl_.showJobOutput(id, output, animate);
   }

   @Override
   public void addJobOutput(String id, int type, String output)
   {
      baseImpl_.addJobOutput(id, type, output);
   }

   @Override
   public void hideJobOutput(String id, boolean animate)
   {
      baseImpl_.hideJobOutput(id, animate);
   }

   @Override
   public void syncElapsedTime(int timestamp)
   {
      baseImpl_.syncElapsedTime(timestamp);
   }

   @Override
   public void bringToFront()
   {
      setShowTabPref(true);
      super.bringToFront();
   }
   
   @Override
   public void setShowTabPref(boolean show)
   {
      if (uiPrefs_.showLauncherJobsTab().getValue() != show)
      {
         uiPrefs_.showLauncherJobsTab().setGlobalValue(show);
         uiPrefs_.writeUserPrefs();
      }
   }

   // internal state
   private JobsDisplayImpl baseImpl_;

   // injected
   private final UserPrefs uiPrefs_;
   private final LauncherJobsPaneWidgets widgets_;
}
