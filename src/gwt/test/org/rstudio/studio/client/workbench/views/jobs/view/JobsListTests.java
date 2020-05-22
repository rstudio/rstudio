/*
 * JobsListTests.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.application.events.FireEvents;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import java.util.ArrayList;
import java.util.List;

public class JobsListTests extends GWTTestCase
{
   private final native JsArrayString emptyJsArrayString() /*-{
      return [];
   }-*/;

   private Job getJob1()
   {
      return new Job()
      {{
         id = "12345";
         name = "Job1";
         recorded = 5;
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
         actions = emptyJsArrayString();
      }};
   }

   private Job getJob2()
   {
      return new Job()
      {{
         id = "1267890";
         name = "Job2";
         recorded = 10;
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
         actions = emptyJsArrayString();
      }};
   }

   private Job getJob3()
   {
      return new Job()
      {{
         id = "abcdef32";
         name = "Job3";
         recorded = 15;
         started = 10;
         elapsed = 20;
         received = 30;
         completed = 0;
         progress = 5;
         max = 10;
         state = JobConstants.STATE_RUNNING;
         type = JobConstants.JOB_TYPE_SESSION;
         actions = emptyJsArrayString();
      }};
   }
   
   private static class FakeEventBus implements FireEvents
   {
      @Override
      public void fireEvent(GwtEvent<?> event)
      {
      }

      @Override
      public void fireEventToAllSatellites(CrossWindowEvent<?> event)
      {
      }

      @Override
      public void fireEventToSatellite(CrossWindowEvent<?> event, WindowEx satelliteWindow)
      {
      }

      @Override
      public void fireEventToMainWindow(CrossWindowEvent<?> event)
      {
      }
   }

   private static class FakeJobItemPrefs implements JobItem.Preferences
   {
      @Override
      public boolean reducedMotion()
      {
         return false;
      }
   }

   private static class Factory implements JobItemFactory
   {
      @Override
      public JobItem create(Job job)
      {
         return new JobItem(job, new FakeEventBus(), new FakeJobItemPrefs());
      }
   }
   
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   /**
    * Verify we can create an instance in the test harness
    */
   public void testAllocate()
   {
      @SuppressWarnings("unused")
      JobsList list = new JobsList(new Factory());
      Assert.assertTrue(true);
   }

   // add ------------------------------------------------------

   /**
    * Verify we can add a single Job
    */
   public void testAddOneJob()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();

      Assert.assertTrue(list.addJob(job1));
      Assert.assertEquals(list.jobCount(), 1);
   }

   /**
    * Verify we can add a single Job and fetch it
    */
   public void testAddFetchJob()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id = job1.id;

      Assert.assertTrue(list.addJob(job1));
      Job fetchedJob = list.getJob(id);
      Assert.assertNotNull(fetchedJob);
      Assert.assertEquals(fetchedJob.id, id);
   }

   /**
    * Verify we get null if we try to fetch job from empty list.
    */
   public void testFetchBogusJob()
   {
      JobsList list = new JobsList(new Factory());
      Job fetchedJob = list.getJob("foo");
      Assert.assertNull(fetchedJob);
   }

   /**
    * Verify we can add a single Job and get null if we try to fetch a
    * job that isn't there
    */
   public void testAddFetchBogusJob()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id = job1.id;

      Assert.assertTrue(list.addJob(job1));
      Job fetchedJob = list.getJob(id + "foo");
      Assert.assertNull(fetchedJob);
   }

   /**
    * Verify we can add a single job and get back the Job
    */
   public void testAddOneJobWithNotify()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id = job1.id;

      Assert.assertTrue(list.addJob(job1));
      Job addedJob = list.getJob(id);
      Assert.assertEquals(addedJob.id, id);
   }

    /**
    * Verify we can add multiple jobs
    */
   public void testAddThreeJobs()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id1 = job1.id;
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();
      String id3 = job3.id;

      Assert.assertTrue(list.addJob(job1));
      Job addedJob = list.getJob(id1);
      Assert.assertEquals(addedJob.id, id1);

      Assert.assertTrue(list.addJob(job2));
      Job addedJob2 = list.getJob(id2);
      Assert.assertEquals(addedJob2.id, id2);

      Assert.assertTrue(list.addJob(job3));
      Job addedJob3 = list.getJob(id3);
      Assert.assertEquals(addedJob3.id, id3);
   }

   /**
    * Verify that trying to add the same job id is a no-op.
    */
   public void testAddThreeJobsWithDups()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      Job job2 = getJob2();
      Job job3 = getJob3();

      Assert.assertTrue(list.addJob(job1));
      Assert.assertTrue(list.addJob(job2));
      Assert.assertTrue(list.addJob(job3));

      Assert.assertEquals(3, list.jobCount());

      Assert.assertFalse(list.addJob(job2));
      Assert.assertEquals(3, list.jobCount());
   }

   /**
    * Verify returned list of jobs is correct size.
    */
   public void testGettingList()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      Job job2 = getJob2();
      Job job3 = getJob3();

      Assert.assertTrue(list.addJob(job2));
      Assert.assertTrue(list.addJob(job3));
      Assert.assertTrue(list.addJob(job1));

      List<Job> jobs = list.getJobs();
      Assert.assertEquals(3, jobs.size());
   }

   /**
    * Verify added jobs are not sorted. Add is the one way to put things in the
    * list that doesn't do any sorting; most recently added goes at the top
    * of the list (position 0).
    */
   public void testAddSortOrder()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id1 = job1.id;
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();
      String id3 = job3.id;

      // adding in a different order than their "recorded" time to validate
      // we aren't getting them back in "recorded" order but rather in the
      // order they were added (most recently added earliest in the list).
      Assert.assertTrue(list.addJob(job2));
      Assert.assertTrue(list.addJob(job3));
      Assert.assertTrue(list.addJob(job1));

      List<Job> jobs = list.getJobs();

      Job fetchJob1 = jobs.get(0);
      Job fetchJob2 = jobs.get(2);
      Job fetchJob3 = jobs.get(1);

      Assert.assertEquals(id1, fetchJob1.id);
      Assert.assertEquals(id2, fetchJob2.id);
      Assert.assertEquals(id3, fetchJob3.id);
   }

   //  Insert --------------------------------------------------

   /**
    * Verify we can insert a single Job
    */
   public void testInsertOneJob()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();

      Assert.assertTrue(list.insertJob(job1));
      Assert.assertEquals(list.jobCount(), 1);
   }

   /**
    * Verify we can insert a single Job and fetch it
    */
   public void testInsertFetchJob()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id = job1.id;

      Assert.assertTrue(list.insertJob(job1));
      Job fetchedJob = list.getJob(id);
      Assert.assertNotNull(fetchedJob);
      Assert.assertEquals(fetchedJob.id, id);
   }

   /**
    * Verify we can insert a single job and get back the Job
    */
   public void testInsertOneJobWithNotify()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id = job1.id;

      Assert.assertTrue(list.insertJob(job1));
      Job insertedJob = list.getJob(id);
      Assert.assertEquals(insertedJob.id, id);
   }

    /**
    * Verify we can insert multiple jobs
    */
   public void testInsertThreeJobs()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id1 = job1.id;
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();
      String id3 = job3.id;

      Assert.assertTrue(list.insertJob(job1));
      Job insertedJob = list.getJob(id1);
      Assert.assertEquals(insertedJob.id, id1);

      Assert.assertTrue(list.insertJob(job2));
      Job insertedJob2 = list.getJob(id2);
      Assert.assertEquals(insertedJob2.id, id2);

      Assert.assertTrue(list.insertJob(job3));
      Job insertedJob3 = list.getJob(id3); 
      Assert.assertEquals(insertedJob3.id, id3);
   }

   /**
    * Verify that trying to insert the same job id is a no-op.
    */
   public void testInsertThreeJobsWithDups()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      Job job2 = getJob2();
      Job job3 = getJob3();

      Assert.assertTrue(list.insertJob(job1));
      Assert.assertTrue(list.insertJob(job2));
      Assert.assertTrue(list.insertJob(job3));

      Assert.assertEquals(3, list.jobCount());

      Assert.assertFalse(list.insertJob(job2));
      Assert.assertEquals(3, list.jobCount());
   }

   /**
    * Verify returned list of inserted jobs is correct size.
    */
   public void testInsertedGettingList()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      Job job2 = getJob2();
      Job job3 = getJob3();

      Assert.assertTrue(list.insertJob(job2));
      Assert.assertTrue(list.insertJob(job3));
      Assert.assertTrue(list.insertJob(job1));

      List<Job> jobs = list.getJobs();
      Assert.assertEquals(3, jobs.size());
   }

   /**
    * Verify inserted jobs are sorted by "recorded" field. The most recently
    * recorded (highest value) should appear earliest in the list.
    */
   public void testInsertSortOrder()
   {
      JobsList list = new JobsList(new Factory());
      Job job1 = getJob1();
      String id1 = job1.id;
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();
      String id3 = job3.id;

      // adding in a different order than their "recorded" time to validate
      // they are being sorted when added
      list.insertJob(job2); // middle-aged should come back in middle of pack
      list.insertJob(job3); // newest, should be at beginning
      list.insertJob(job1); // oldest, should be at end

      List<Job> jobs = list.getJobs();

      Job fetchJob1 = jobs.get(2);
      Job fetchJob2 = jobs.get(1);
      Job fetchJob3 = jobs.get(0);

      Assert.assertEquals(id1, fetchJob1.id);
      Assert.assertEquals(id2, fetchJob2.id);
      Assert.assertEquals(id3, fetchJob3.id);
   }

   // setInitialJobs -------------------------------------------

   /**
    * Test adding a batch of jobs. These should come back sorted by the
    * "recorded" field, with the most recently recorded at the beginning of the list
    * (position 0).
    */
   public void testSetInitialJobs()
   {
      JobsList list = new JobsList(new Factory());

      ArrayList<Job> jobs = new ArrayList<>();

      Job job1 = getJob1();
      String id1 = job1.id;
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();
      String id3 = job3.id;

      jobs.add(job2); // middle-aged item, should stay in middle
      jobs.add(job1); // oldest item, should be at the end
      jobs.add(job3); // newest item, should be at beginning of list

      list.setInitialJobs(jobs);

      Assert.assertEquals(3, list.jobCount());

      List<Job> sortedJobs = list.getJobs();

      Job fetchJob1 = sortedJobs.get(2);
      Job fetchJob2 = sortedJobs.get(1);
      Job fetchJob3 = sortedJobs.get(0);

      Assert.assertEquals(id1, fetchJob1.id);
      Assert.assertEquals(id2, fetchJob2.id);
      Assert.assertEquals(id3, fetchJob3.id);
   }

   // clear ----------------------------------------------------

   public void testClearJobs()
   {
      JobsList list = new JobsList(new Factory());

      ArrayList<Job> jobs = new ArrayList<>();

      jobs.add(getJob1());
      jobs.add(getJob2());
      jobs.add(getJob3());

      list.setInitialJobs(jobs);

      Assert.assertEquals(3, list.jobCount());

      list.clear();

      Assert.assertEquals(0, list.jobCount());
      List<Job> fetchedJobs = list.getJobs();
      Assert.assertEquals(0, fetchedJobs.size());
   }

   // remove ----------------------------------------------------

   public void testRemoveJob()
   {
      JobsList list = new JobsList(new Factory());

      ArrayList<Job> jobs = new ArrayList<>();

      Job job1 = getJob1();
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();

      jobs.add(job2); // middle-aged item, should stay in middle
      jobs.add(job1); // oldest item, should be at the end
      jobs.add(job3); // newest item, should be at beginning of list

      list.setInitialJobs(jobs);

      Assert.assertEquals(3, list.jobCount());

      Job fetchJob2 = list.getJob(id2);
      Assert.assertNotNull(fetchJob2);

      Assert.assertTrue(list.removeJob(job2));
      Job fetchJob2Again = list.getJob(id2);
      Assert.assertNull(fetchJob2Again);
      Assert.assertEquals(2, list.jobCount());
   }

   // update ----------------------------------------------------

   public void testUpdateJob()
   {
      JobsList list = new JobsList(new Factory());

      ArrayList<Job> jobs = new ArrayList<>();

      Job job1 = getJob1();
      Job job2 = getJob2();
      String id2 = job2.id;
      Job job3 = getJob3();

      jobs.add(job2);
      jobs.add(job1);
      jobs.add(job3);

      list.setInitialJobs(jobs);

      Job job2Modified = getJob2();
      job2Modified.name = "New and Improved";

      list.updateJob(job2Modified);

      Job fetchedJob = list.getJob(id2);
      Assert.assertEquals(job2Modified.name, fetchedJob.name);

      // updateJob does computations in JobItem, based on state changes,
      // but that's better covered via JobItem tests
   }

}

