/*
 * JobsPresenterEventHandlersImpl.java
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
package org.rstudio.studio.client.workbench.views.jobs.events;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.SessionServer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;
import org.rstudio.studio.client.workbench.views.jobs.model.LauncherJobManager;
import org.rstudio.studio.client.workbench.views.jobs.view.JobsDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of JobsPresentEventHandlers.
 */
public class JobsPresenterEventHandlersImpl implements JobsPresenterEventHandlers
{
   public JobsPresenterEventHandlersImpl(int jobType,
                                         JobsDisplay display)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      jobType_ = jobType;
      display_ = display;
   }

   @Inject
   private void initialize(JobsServerOperations server,
                           GlobalDisplay globalDisplay,
                           Provider<JobManager> pJobManager,
                           Provider<LauncherJobManager> pLauncherJobManager,
                           EventBus eventBus)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
      pJobManager_ = pJobManager;
      pLauncherJobManager_ = pLauncherJobManager;
      eventBus_ = eventBus;
   }
   
   @Override
   public void onJobElapsedTick(JobElapsedTickEvent event)
   {
      display_.syncElapsedTime(event.timestamp());
   }
   
   @Override
   public void setInitialJobs(JobState state)
   {
      // make an array of all the jobs on the server
      ArrayList<Job> jobs = new ArrayList<>();
      if (state != null)
      {
         for (String id : state.iterableKeys())
         {
            Job job = state.getElement(id);
            if (!isSupportedJobType(job.type))
               continue;
      
            jobs.add(state.getElement(id));
         }
      }
      setJobState(jobs);
   }
   
   @Override
   public void onJobOutput(JobOutputEvent event)
   {
      Job job = pJobManager_.get().getJob(event.getData().id());
      if (isSupportedJobType(job.type))
      {
         display_.addJobOutput(event.getData().id(), event.getData().type(), event.getData().output());
      }
   }
   
   @Override
   public void onJobSelection(JobSelectionEvent event)
   {
      Job job = pJobManager_.get().getJob(event.id());
      if (!isSupportedJobType(job.type))
         return;
   
      if (JsArrayUtil.jsArrayStringContains(job.actions, JobConstants.ACTION_INFO))
      {
         if (event.selected())
         {
            eventBus_.fireEvent(new JobExecuteActionEvent(event.id(), JobConstants.ACTION_INFO));
         }
      }
      else
      {
         if (event.selected())
         {
            selectJob(event.id(), event.animate(), job.type == JobConstants.JOB_TYPE_LAUNCHER);
         }
         else
         {
            unselectJob(event.id(), event.animate(), job.type == JobConstants.JOB_TYPE_LAUNCHER);
         }
      }
   }
   
   @Override
   public void onJobUpdated(JobUpdatedEvent event)
   {
      if (isSupportedJobType(event.getData().job.type))
      {
         display_.updateJob(event.getData().type, event.getData().job);
      }
   }
   
   private boolean isSupportedJobType(int jobType)
   {
      return jobType == jobType_;
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setJobState(List<Job> jobs)
   {
      display_.setInitialJobs(jobs);
   }

   private void unselectJob(final String id, boolean animate, boolean isLauncherJob)
   {
      boolean bypassLauncherCall = (isLauncherJob && getSessionServer() != null);

      server_.setJobListening(id, false, bypassLauncherCall, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            if (bypassLauncherCall && Desktop.hasDesktopFrame())
            {
               Desktop.getFrame().stopLauncherJobOutputStream(id);
            }

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
   
   private void selectJob(final String id, boolean animate, boolean isLauncherJob)
   {
      boolean bypassLauncherCall = (isLauncherJob && getSessionServer() != null);

      server_.setJobListening(id, true, bypassLauncherCall, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            if (bypassLauncherCall && Desktop.hasDesktopFrame())
            {
               Desktop.getFrame().startLauncherJobOutputStream(id);
            }

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

   private SessionServer getSessionServer()
   {
      return pLauncherJobManager_.get().getSessionServer();
   }
   
   private final int jobType_;
   private final JobsDisplay display_;
   
   // injected
   private JobsServerOperations server_;
   private GlobalDisplay globalDisplay_;
   private Provider<JobManager> pJobManager_;
   private Provider<LauncherJobManager> pLauncherJobManager_;
   private EventBus eventBus_;
}
