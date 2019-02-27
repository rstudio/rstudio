/*
 * JobsListView.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.user.client.Command;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import java.util.List;
import java.util.function.Consumer;

public interface JobsListView
{
   /**
    * Add a job to the beginning of the list, independent of sort order
    * @param job job to add
    * @param onAddedItem callback with added JobItem, not invoked if job already in list
    */
   void addJob(Job job, Consumer<JobItem> onAddedItem);

   /**
    * Insert a job in the list based on current sort order
    * @param job job to insert
    * @param onInsertedItem callback with added JobItem, not invoked if job already in list
    */
   void insertJob(Job job, Consumer<JobItem> onInsertedItem);

   /**
    * Remove job from list
    * @param job job to remove
    * @param onRemoved callback if job was removed, not invoked if job wasn't in list
    */
   void removeJob(Job job, Command onRemoved);

   /**
    * Process updates to an existing Job
    * @param job
    */
   void updateJob(Job job);

   /**
    * Remove all jobs from the list
    */
   void clear();

   /**
    *  Update timestamp of all jobs
    * @param timestamp
    */
   void syncElapsedTime(int timestamp);

   /**
    * @param id
    * @return Job or null
    */
   Job getJob(String id);

   /**
    * @return number of Jobs
    */
   int jobCount();

   /**
    * @return List of jobs in display order
    */
   List<Job> getJobs();

   /**
    * Populate with a list of jobs, using current sort order
    * @param jobs
    */
   void setInitialJobs(List<Job> jobs);
}
