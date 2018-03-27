/*
 * JobManager.java
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
package org.rstudio.studio.client.workbench.views.jobs.model;

import java.util.ArrayList;
import java.util.Date;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobInitEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobProgressEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRefreshEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class JobManager implements JobRefreshEvent.Handler,
                                   JobUpdatedEvent.Handler
{
   @Inject
   public JobManager(Session session,
                     EventBus events)
   {
      events_ = events;
      setJobState(session.getSessionInfo().getJobState());
   }
   
   @Override
   public void onJobRefresh(JobRefreshEvent event)
   {
      setJobState(event.getData());
   }
   
   @Override
   public void onJobUpdated(JobUpdatedEvent event)
   {
      Job job = event.getData().job;
      switch(event.getData().type)
      {
         case JobConstants.JOB_ADDED:
            // if adding a running job, add it to the progress set
            if (job.state == JobConstants.STATE_RUNNING)
               addProgress(job);
            
            state_.addJob(job);
            break;

         case JobConstants.JOB_REMOVED:
            state_.removeJob(job);
            break;

         case JobConstants.JOB_UPDATED:
            if (state_.hasKey(job.id) &&
                state_.getJob(job.id).state != JobConstants.STATE_RUNNING &&
                job.state == JobConstants.STATE_RUNNING)
            {
               // this job was idle, but now it's running
               addProgress(job);
            }
            state_.updateJob(job);
            break;
            
         default:
            Debug.logWarning("Unrecognized job update type " + event.getData().type);
      }
      
      // start timing jobs
      syncTimer();
   }

   // Private methods ---------------------------------------------------------
   
   /**
    * Adds a job to the progress set. The progress set defines the set of jobs
    * we show a global progress indicator for.
    * 
    * @param job
    */
   private void addProgress(Job job)
   {
      
   }
   
   private void emitJobProgress()
   {
      ArrayList<Job> running = new ArrayList<Job>();
      
      // find all the running jobs
      for (String id: state_.iterableKeys())
      {
         Job job = state_.getJob(id);
         if (job.state == JobConstants.STATE_RUNNING)
            running.add(job);
      }
      
      // if no jobs are running, emit empty progress event
      if (running.isEmpty())
      {
         events_.fireEvent(new JobProgressEvent());
         return;
      }
      
      // compute name; if only one job is running, it's the name of that job
      String name;
      if (running.size() == 1)
         name = running.get(0).name;
      else 
         name = running.size() + " jobs";
      
      // flag to see if we're measuring individual units or job counts
      boolean jobUnits = false;
      
      // compute units
      int max = 0;
      for (int i = 0; i < running.size(); i++)
      {
         // if this job doesn't have ranged progress, then we can't show ranged
         // progress 
         if (running.get(i).max == 0)
         {
            // no progress max
            max = 0;
            
            // however, if we have more than one job--
            if (running.size() > 1)
            {
               // then our max is the number of jobs, and each unit is one job
               // TODO: this needs to account for completed jobs
               max = running.size();
               jobUnits = true;
            }
            break;
         }
      }
      
      int units = 0;
      if (jobUnits)
      {
         units = running.size();
      }
      else if (max > 0)
      {
         for (int i = 0; i < running.size(); i++)
         {
            units += running.get(i).progress;
         }
      }

      int elapsed = 0;
      events_.fireEvent(new JobProgressEvent(name, units, max, elapsed));
   }

   private void setJobState(JobState state)
   {
      events_.fireEvent(new JobInitEvent(state_));
      
      // start timing jobs
      syncTimer();
   }
   
   private void syncTimer()
   {
      // start or stop updating job elapsed times based on whether we have any
      // jobs running
      if (state_.keys().length() > 0 && !elapsed_.isRunning())
         elapsed_.scheduleRepeating(1000);
      else if (state_.keys().length() == 0 && elapsed_.isRunning())
         elapsed_.cancel();
   }

   Timer elapsed_ = new Timer()
   {
      @Override
      public void run()
      {
         // we use an event to updated all jobs' elapsed time in lockstep across
         // the whole UI
         int timestamp = (int)((new Date()).getTime() * 0.001);
         events_.fireEvent(new JobElapsedTickEvent(timestamp));
      }
   };

   private ArrayList<Job> progress_;
   private JobState state_;
   private final EventBus events_;
}
