/*
 * JobsPresenter.java
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

import java.util.List;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobsPresenterEventHandlers;
import org.rstudio.studio.client.workbench.views.jobs.events.JobsPresenterEventHandlersImpl;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.view.JobsDisplay;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class JobsPresenter extends BasePresenter  
                           implements JobsPresenterEventHandlers
{
   public interface Display extends JobsDisplay
   {}
   public interface Binder extends CommandBinder<Commands, JobsPresenter> {}
   
   @Inject
   public JobsPresenter(Display display, 
                        Binder binder,
                        Commands commands,
                        GlobalDisplay globalDisplay,
                        Provider<JobManager> pJobManager)
   {
      super(display);
      
      jobEventHandler_ = new JobsPresenterEventHandlersImpl(JobConstants.JOB_TYPE_SESSION,
                                                            display);
      
      display_ = display;
      globalDisplay_ = globalDisplay;
      pJobManager_ = pJobManager;
      commands_ = commands;
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

   public void confirmClose(Command onConfirmed)
   {
      List<Job> jobs = pJobManager_.get().getJobs();
      
      // if there are no jobs, go ahead and let the tab close
      if (jobs.isEmpty())
      {
         display_.setShowTabPref(false);
         onConfirmed.execute();
      }

      // count the number of running session jobs
      long running = jobs.stream()
            .filter(t -> t.type == JobConstants.JOB_TYPE_SESSION &&
                         t.state == JobConstants.STATE_RUNNING).count();
      
      if (running > 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, 
               "Local Jobs Still Running", 
               "The Jobs tab cannot be closed while there " +
               (running > 1 ?
                  "are unfinished local jobs" : "is an unfinished local job") + "." +
               "\n\nWait until all local jobs have completed.");
         return;
      }
      
      // done, okay to close
      display_.setShowTabPref(false);
      onConfirmed.execute();
   }
   
   @Handler
   public void onActivateJobs()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      display_.bringToFront();
   }
  
   private JobsPresenterEventHandlersImpl jobEventHandler_;
   
   // injected
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Provider<JobManager> pJobManager_;
}
