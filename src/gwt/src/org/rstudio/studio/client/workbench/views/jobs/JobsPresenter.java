/*
 * JobsPresenter.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobExecuteActionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobInitEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class JobsPresenter extends BasePresenter  
                           implements JobUpdatedEvent.Handler,
                                      JobInitEvent.Handler,
                                      JobOutputEvent.Handler,
                                      JobSelectionEvent.Handler,
                                      JobElapsedTickEvent.Handler
{
   public interface Display extends WorkbenchView
   {
      void updateJob(int type, Job job);
      void setInitialJobs(JsObject jobs);
      void showJobOutput(String id, JsArray<JobOutput> output, boolean animate);
      void addJobOutput(String id, int type, String output);
      void hideJobOutput(String id, boolean animate);
      void syncElapsedTime(int timestamp);
   }
   
   public interface Binder extends CommandBinder<Commands, JobsPresenter> {}
   
   @Inject
   public JobsPresenter(Display display, 
                        JobsServerOperations server,
                        Binder binder,
                        Commands commands,
                        EventBus events,
                        GlobalDisplay globalDisplay,
                        Provider<JobManager> pJobManager,
                        EventBus eventBus)
   {
      super(display);
      display_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      pJobManager_ = pJobManager;
      eventBus_ = eventBus;
      binder.bind(commands, this);
   }

   @Override
   public void onJobUpdated(JobUpdatedEvent event)
   {
      display_.updateJob(event.getData().type, event.getData().job);
   }

   @Override
   public void onJobInit(JobInitEvent event)
   {
      setJobState(event.state());
   }
   
   @Override
   public void onJobOutput(JobOutputEvent event)
   {
      display_.addJobOutput(event.getData().id(), 
            event.getData().type(), event.getData().output());
   }
   
   @Override
   public void onJobSelection(final JobSelectionEvent event)
   {
      Job job = pJobManager_.get().getJob(event.id());
      if (JsArrayUtil.jsArrayStringContains(job.actions, "info"))
      {
         if (event.selected())
            eventBus_.fireEvent(new JobExecuteActionEvent(event.id(), "info"));
      }
      else
      {
         if (event.selected())
         {
            selectJob(event.id(), event.animate());
         }
         else
         {
            unselectJob(event.id(), event.animate());
         }
      }
   }
   
   @Override
   public void onJobElapsedTick(JobElapsedTickEvent event)
   {
      display_.syncElapsedTime(event.timestamp());
   }
   
   public void confirmClose(Command onConfirmed)
   {
      List<Job> jobs = pJobManager_.get().getJobs();
      
      // if there are no jobs, go ahead and let the tab close
      if (jobs.isEmpty())
         onConfirmed.execute();

      // count the number of running jobs
      int running = 0;
      for (int i = 0; i < jobs.size(); i++)
      {
         running++;
      }
      
      if (running > 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, 
               "Jobs Still Running", 
               "The Jobs tab cannot be closed while there " +
               (running > 1 ?
                  "are unfinished jobs" : "is an unfinished job") + "." +
               "\n\nWait until all jobs have completed.");
         return;
      }
      
      // done, okay to close
      onConfirmed.execute();
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setJobState(JobState state)
   {
      display_.setInitialJobs(state);
   }
   
   private void unselectJob(final String id, boolean animate)
   {
      server_.setJobListening(id, false, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            display_.hideJobOutput(id, animate);
         }
         
         @Override
         public void onError(ServerError error)
         {
            // if we couldn't turn off listening on the server, it's not a big
            // deal (we'll ignore output from the job if we don't recognize it),
            // so hide the output anyway and don't complain to the user
            display_.hideJobOutput(id, animate);
            Debug.logError(error);
         }
      });
   }
   
   private void selectJob(final String id, boolean animate)
   {
      server_.setJobListening(id, true, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            display_.showJobOutput(id, output, animate);
         }
         
         @Override
         public void onError(ServerError error)
         {
            // CONSIDER: this error is unlikely, but it'd be nicer to show the
            // job output anyway, with a non-modal error in it
            globalDisplay_.showErrorMessage("Cannot retrieve job output", 
                  error.getMessage());
         }
      });
   }

   private final JobsServerOperations server_;
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   private final Provider<JobManager> pJobManager_;
   private final EventBus eventBus_;
}
