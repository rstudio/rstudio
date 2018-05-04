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

import org.rstudio.core.client.Debug;
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
import org.rstudio.studio.client.workbench.views.jobs.events.JobInitEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobOutputEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobState;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;

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
      void showJobOutput(String id, JsArray<JobOutput> output);
      void addJobOutput(String id, int type, String output);
      void hideJobOutput(String id);
      void syncElapsedTime(int timestamp);
   }
   
   public interface Binder extends CommandBinder<Commands, JobsPresenter> {}
   
   @Inject
   public JobsPresenter(Display display, 
                        JobsServerOperations server,
                        Binder binder,
                        Commands commands,
                        EventBus events,
                        GlobalDisplay globalDisplay)
   {
      super(display);
      display_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
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
      if (event.selected())
      {
         selectJob(event.id());
      }
      else
      {
         unselectJob(event.id());
      }
   }
   
   @Override
   public void onJobElapsedTick(JobElapsedTickEvent event)
   {
      display_.syncElapsedTime(event.timestamp());
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setJobState(JobState state)
   {
      display_.setInitialJobs(state);
   }
   
   private void unselectJob(final String id)
   {
      server_.setJobListening(id, false, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            display_.hideJobOutput(id);
         }
         
         @Override
         public void onError(ServerError error)
         {
            // if we couldn't turn off listening on the server, it's not a big
            // deal (we'll ignore output from the job if we don't recognize it),
            // so hide the output anyway and don't complain to the user
            display_.hideJobOutput(id);
            Debug.logError(error);
         }
      });
   }
   
   private void selectJob(final String id)
   {
      server_.setJobListening(id, true, new ServerRequestCallback<JsArray<JobOutput>>()
      {
         @Override
         public void onResponseReceived(JsArray<JobOutput> output)
         {
            display_.showJobOutput(id, output);
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
}
