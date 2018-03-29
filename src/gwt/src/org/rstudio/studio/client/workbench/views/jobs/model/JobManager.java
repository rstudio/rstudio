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
import java.util.Collections;
import java.util.Date;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobInitEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobProgressEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRefreshEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class JobManager implements JobRefreshEvent.Handler,
                                   JobUpdatedEvent.Handler,
                                   SessionInitHandler
{
   @Inject
   public JobManager(Provider<Session> pSession,
                     EventBus events)
   {
      events_ = events;
      pSession_ = pSession;
      state_ = JobState.create();
      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(JobRefreshEvent.TYPE, this);
      events.addHandler(JobUpdatedEvent.TYPE, this);
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      Session session = pSession_.get();
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
            state_.addJob(job);
            break;

         case JobConstants.JOB_REMOVED:
            state_.removeJob(job);
            break;

         case JobConstants.JOB_UPDATED:
            state_.updateJob(job);
            break;
            
         default:
            Debug.logWarning("Unrecognized job update type " + event.getData().type);
      }
      
      // start timing jobs
      syncTimer();
      
      // update global status 
      emitJobProgress();
   }
   
   /**
    * Creates a progress event summarizing progress for a given state.
    * 
    * Decides which jobs to emit progress for by considering all of the jobs
    * with overlapping start/end times. This ensures that e.g. if you start
    * Job A, Job B, and Job C, then completing Job A (while B and C are still
    * running) won't suddenly cause your progress meter to go backwards when
    * Job A drops out of the list of running jobs.
    * 
    * Some jobs may not have ranged progress; that is, they are simply running
    * or not running. In this case, the progress is reported in terms of the
    * number of jobs.
    * 
    * @param state Job state to summarize
    * @return Progress of running jobs, or null if no progress.
    * 
    */
   public static GlobalJobProgress summarizeProgress(JobState state)
   {
      boolean running = false;
      
      // flatten job list to an array while looking for a running job
      ArrayList<Job> jobs = new ArrayList<Job>();
      for (String id: state.iterableKeys())
      {
         // push job into array
         Job job = state.getJob(id);
         jobs.add(job);
         
         // remember if we found a running job
         if (job.state == JobConstants.STATE_RUNNING)
            running = true;
      }
      
      // if we didn't find any running job, then we have no progress to report 
      if (!running)
      {
         return null;
      }
      
      // Now we need to find all of the jobs that overlap with the first running
      // job. This is done as follows:
      // 
      // 1. Sort all of the jobs by the time they started
      // 2. Find the currently running job that started first
      // 3. Work backwards (old jobs) until we find one that does not overlap
      // 4. Work forwards (new jobs) until we find one that does not overlap
      
      // sort by start time
      Collections.sort(jobs, (Job j1, Job j2) -> {
         return j1.started - j2.started;
      });
      
      // find index of first running job
      int idxRunning;
      for (idxRunning = 0; idxRunning < jobs.size(); idxRunning++)
      {
         if (jobs.get(idxRunning).state == JobConstants.STATE_RUNNING)
            break;
      }
      
      // starting at that index, we need to work backwards to find completed
      // jobs that overlap
      int idxFirst;
      for (idxFirst = idxRunning; idxFirst > 0; idxFirst--)
      {
         // consider the job to the left of the progress set
         Job job = jobs.get(idxFirst - 1);

         // if this job finished before the set started, then it is not in the
         // set
         if (job.completed < jobs.get(idxFirst).started)
            break;
         
         // if this job did not start, it is not in the set
         if (job.started == 0)
            break;
      }
      
      // now we need to walk forwards to find other jobs that overlap
      int idxLast;
      for (idxLast = idxRunning; idxLast < jobs.size() - 1; idxLast++)
      {
         // consider the job to the right of the progress set
         Job job = jobs.get(idxLast + 1);

         // if this job has not started, it is not in the progress set
         if (job.started == 0)
            break;
      }
      
      int numJobs = (idxLast - idxFirst) + 1;

      // compute name; if only one job is running, it's the name of that job
      String name;
      if (idxFirst == idxLast)
         name = jobs.get(idxFirst).name;
      else 
         name =  numJobs + " jobs";
      
      // compute total progress units and longest running job
      int progress = 0;
      int elapsed = 0;
      int idxElapsed = 0;
      for (int i = idxFirst; i <= idxLast; i++)
      {
         Job job = jobs.get(i);
         int max = job.max;
         
         // compute progress units
         if (max == 0)
         {
            // if the job does not have its own progress units, treat it as
            // all-or-nothing
            progress += job.completed > 0 ? 100 : 0;
         }
         else
         {
            // the job has its own progress units; scale them to 0 - 100
            progress += (int)(((double)job.progress / (double)job.max) * (double)100);
         }
         
         // compute elapsed time
         if (elapsed == 0 || job.elapsed > elapsed)
         {
            elapsed = job.elapsed;
            idxElapsed = i;
         }
      }
      
      // add to the elapsed time the time that has elapsed between the time the
      // first job started and the time the longest running job started
      elapsed += (jobs.get(idxElapsed).started - jobs.get(idxFirst).started);
      
      // add the time that has elapsed since the last job started
      // elapsed += jobs.get(idxLast).elapsed;
      
      return new GlobalJobProgress(
            name,                         // title of progress
            progress,                     // number of units completed
            numJobs * 100,                // total number of units, 100 per job 
            elapsed,                      // time elapsed so far
            jobs.get(idxLast).received    // received time
      );
   }

   // Private methods ---------------------------------------------------------
   
   private void emitJobProgress()
   {
      GlobalJobProgress progress = summarizeProgress(state_);
      events_.fireEvent(new JobProgressEvent(progress));
   }

   private void setJobState(JobState state)
   {
      state_ = state;
      events_.fireEvent(new JobInitEvent(state_));
      
      // start timing jobs and emitting progress
      syncTimer();
      emitJobProgress();
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

   private JobState state_;

   private final EventBus events_;
   private final Provider<Session> pSession_;
}
