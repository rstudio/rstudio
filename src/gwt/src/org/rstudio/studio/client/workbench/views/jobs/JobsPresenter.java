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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRefreshEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;

public class JobsPresenter extends BasePresenter  
                           implements JobUpdatedEvent.Handler,
                                      JobRefreshEvent.Handler,
                                      JobOutputEvent.Handler
{
   public interface Display extends WorkbenchView
   {
      void updateJob(int type, Job job);
      void setInitialJobs(JsObject jobs);
      void showJobOutput(Job job, JsArray<JobOutput> output);
      void addJobOutput(String id, int type, String output);
   }
   
   public interface Binder extends CommandBinder<Commands, JobsPresenter> {}
   
   @Inject
   public JobsPresenter(Display display, 
                        JobsServerOperations server,
                        Binder binder,
                        Commands commands,
                        EventBus events,
                        Session session)
   {
      super(display);
      display_ = display;
      server_ = server;
      binder.bind(commands, this);
      setJobState(session.getSessionInfo().getJobState());
   }

   @Override
   public void onJobUpdated(JobUpdatedEvent event)
   {
      JobState.recordReceived(event.getData().job);
      display_.updateJob(event.getData().type, event.getData().job);
   }

   @Override
   public void onJobRefresh(JobRefreshEvent event)
   {
      setJobState(event.getData());
   }
   
   @Override
   public void onJobOutput(JobOutputEvent event)
   {
      display_.addJobOutput(event.getData().id(), 
            event.getData().type(), event.getData().output());
   }
   
   private void setJobState(JobState state)
   {
      state.recordReceived();
      display_.setInitialJobs(state);
   }
   
   private final JobsServerOperations server_;
   private final Display display_;
}
