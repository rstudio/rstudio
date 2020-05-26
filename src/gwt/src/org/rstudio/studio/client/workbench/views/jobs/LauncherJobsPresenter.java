/*
 * LauncherJobsPresenter.java
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

package org.rstudio.studio.client.workbench.views.jobs;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobsPresenterEventHandlers;
import org.rstudio.studio.client.workbench.views.jobs.events.JobsPresenterEventHandlersImpl;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.LauncherJobManager;
import org.rstudio.studio.client.workbench.views.jobs.view.JobsDisplay;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class LauncherJobsPresenter extends BasePresenter
                                   implements JobsPresenterEventHandlers
{
   public interface Display extends JobsDisplay {}
   
   public interface Binder extends CommandBinder<Commands, LauncherJobsPresenter> {}
   
   @Inject
   public LauncherJobsPresenter(Display display,
                                Binder binder,
                                Commands commands,
                                LauncherJobManager launcherJobManager)
   {
      super(display);
   
      jobEventHandler_ = new JobsPresenterEventHandlersImpl(JobConstants.JOB_TYPE_LAUNCHER, display);
      
      display_ = display;
      commands_ = commands;
      launcherJobManager_ = launcherJobManager;
      binder.bind(commands, this);
    }

   @Override
   public void onJobUpdated(JobUpdatedEvent event)
   {
      jobEventHandler_.onJobUpdated(event);
   }

   @Override
   public void setInitialJobs(JobState state)
   {
      jobEventHandler_.setInitialJobs(state);
   }
   
   @Override
   public void onJobOutput(JobOutputEvent event)
   {
      jobEventHandler_.onJobOutput(event);
   }
   
   @Override
   public void onJobSelection(final JobSelectionEvent event)
   {
      jobEventHandler_.onJobSelection(event);
   }
   
   @Override
   public void onJobElapsedTick(JobElapsedTickEvent event)
   {
      jobEventHandler_.onJobElapsedTick(event);
   }
   
   @Override
   public void onBeforeUnselected()
   {
      super.onBeforeUnselected();
      launcherJobManager_.stopTrackingAllJobStatuses();
   }
   
   @Override
   public void onBeforeSelected()
   {
      super.onBeforeSelected();
      launcherJobManager_.startTrackingAllJobStatuses();
   }
   
   public void confirmClose(Command onConfirmed)
   {
      // launcher jobs are not tied to the session so always ok to close
      display_.setShowTabPref(false);
      onConfirmed.execute();
   }
   
   @Handler
   public void onActivateLauncherJobs()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      display_.bringToFront();
   }
   
   // Private methods ---------------------------------------------------------
  
   private JobsPresenterEventHandlersImpl jobEventHandler_;
   
   // injected
   private final Display display_;
   private final Commands commands_;
   private final LauncherJobManager launcherJobManager_;
}
