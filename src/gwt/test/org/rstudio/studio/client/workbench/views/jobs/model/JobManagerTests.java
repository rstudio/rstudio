/*
 * JobManagerTests.java
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
package org.rstudio.studio.client.workbench.views.jobs.model;

import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

public class JobManagerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   /**
    * Tests a single job
    */
   public void testSummarizeSingle()
   {
      JobState state = JobState.create();
      Job job1 = new Job()
      {{
         id = "1";
         name = "Job1";
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job1);
      
      LocalJobProgress progress = JobManager.summarizeProgress(state);

      Assert.assertEquals(20, progress.elapsed());
      Assert.assertEquals(30, progress.received());
   }

   /**
    * Tests two jobs running concurrently to be sure that the summary correctly
    * aggregates the information from both jobs.
    */
   public void testSummarizeOverlapping()
   {
      JobState state = JobState.create();
      Job job1 = new Job()
      {{
         id = "1";
         name = "Job1";
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job1);
      Job job2 = new Job()
      {{
         id = "2";
         name = "Job2";
         started = 15;
         elapsed = 15;
         received = 30;
         completed = 0;
         progress = 9;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job2);
      LocalJobProgress progress = JobManager.summarizeProgress(state);
      
      // total progress units = 20, total completed = 14
      Assert.assertEquals(70, progress.percent(), 0.01);
      
      // total elapsed time is 20
      Assert.assertEquals(20, progress.elapsed());
   }

   /**
    * Tests two jobs that don't overlap to ensure that the first job's progress
    * is not included in the aggregation.
    */
   public void testSummarizeNonOverlapping()
   {
      JobState state = JobState.create();
      Job job1 = new Job()
      {{
         id = "1";
         name = "Job1";
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 30;
         progress = 10;
         max = 10;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job1);
      Job job2 = new Job()
      {{
         id = "2";
         name = "Job2";
         started = 40;
         elapsed = 15;
         received = 50;
         completed = 0;
         progress = 2;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job2);
      LocalJobProgress progress = JobManager.summarizeProgress(state);
      
      // only the first job's progress should be considered
      Assert.assertEquals(20, progress.percent(), 0.01);
      
      // only the first job's elapsed time should be considered
      Assert.assertEquals(15, progress.elapsed());
   }

   /**
    * Tests nested jobs: a running job during which a second job started and
    * then finished
    */
   public void testNested()
   {
      // Completed job
      JobState state = JobState.create();
      Job job1 = new Job()
      {{
         id = "1";
         name = "Job1";
         started = 5;
         elapsed = 10;
         received = 50;
         completed = 15;
         progress = 10;
         max = 10;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job1);
      
      // Outer job: started first, still running
      Job job2 = new Job()
      {{
         id = "2";
         name = "Job2";
         started = 10;
         elapsed = 40;
         received = 50;
         completed = 0;
         progress = 4;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job2);
      
      // Inner job: started second, but completed
      Job job3 = new Job()
      {{
         id = "3";
         name = "Job3";
         started = 20;
         elapsed = 10;
         received = 50;
         completed = 10;
         progress = 0;
         max = 0;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(job3);
      LocalJobProgress progress = JobManager.summarizeProgress(state);
      
      // Two completed jobs, one at 40% = 240 / 300 = 80%
      Assert.assertEquals(80, progress.percent(), 0.01);
      
      // Total elapsed time is the time from the start of the first job to the
      // start of the second job (5), plus the second job's elapsed time (40)
      Assert.assertEquals(45, progress.elapsed());
   }

   /**
    * Tests a complicated situation:
    *
    * - 2 completed jobs
    *   - 1 that finished before any jobs (A)
    *   - 1 that completed after the current set began running (B)
    * - 2 running jobs
    *   - 1 with ranged progress (C)
    *   - 1 without ranged progress (D)
    * - 1 job not yet started (E)
    *
    * The progress meter should correctly aggregate the data from jobs B, C, and
    * D, while ignoring A and E.
    */
   public void testSummarizeComplex()
   {
      JobState state = JobState.create();
      // Job A: completed, excluded (non-overlapping)
      Job jobA = new Job()
      {{
         id = "A";
         name = "JobA";
         started = 10;
         elapsed = 10;
         received = 100;
         completed = 20;
         progress = 10;
         max = 10;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobA);

      // Job B: completed, included since it completed after Job C started
      Job jobB = new Job()
      {{
         id = "B";
         name = "JobB";
         started = 30;
         elapsed = 10;
         received = 100;
         completed = 40;
         progress = 10;
         max = 10;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobB);
      
      // Job C: running, with ranged progress
      Job jobC = new Job()
      {{
         id = "C";
         name = "JobC";
         started = 35;
         elapsed = 65;
         received = 100;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobC);

      // Job D: running, without ranged progress
      Job jobD = new Job()
      {{
         id = "D";
         name = "JobD";
         started = 40;
         elapsed = 60;
         received = 100;
         completed = 0;
         progress = 0;
         max = 0;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobD);

      // Job E: not running yet
      Job jobE = new Job()
      {{
         id = "E";
         name = "JobE";
         started = 0;
         elapsed = 0;
         received = 100;
         completed = 0;
         progress = 0;
         max = 0;
         state = JobConstants.STATE_IDLE;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobE);

      LocalJobProgress progress = JobManager.summarizeProgress(state);
      
      // Elapsed time is from start of B (30) to start of D (40) plus D's
      // ongoing elapsed time (60)
      Assert.assertEquals(70, progress.elapsed());

      // B is 100% done, C is 50% done, D is 0% done
      Assert.assertEquals(50, progress.percent(), 0.01);
   }
   
   /**
    * Tests a staggered receive: one finished job was received in the past by
    * the client, and another one is currently running.
    */
   public void testStaggeredReceive()
   {
      JobState state = JobState.create();

      // Job A: completed
      Job jobA = new Job()
      {{
         id = "A";
         name = "JobA";
         started = 10;
         elapsed = 30;
         received = 40;
         completed = 40;
         progress = 10;
         max = 10;
         state = JobConstants.STATE_SUCCEEDED;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobA);

      // Job B: still running
      Job jobB = new Job()
      {{
         id = "B";
         name = "JobB";
         started = 20;
         elapsed = 30;
         received = 50;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
      }};
      state.addJob(jobB);

      LocalJobProgress progress = JobManager.summarizeProgress(state);

      // Currently on tick 50; that means that elapsed time should be 40 (Job A
      // was started at tick 10)
      Assert.assertEquals(50, progress.received());
      Assert.assertEquals(40, progress.elapsed());
   }
}
