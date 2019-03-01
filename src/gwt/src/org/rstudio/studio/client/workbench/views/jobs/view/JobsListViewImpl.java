/*
 * JobsListViewImpl.java
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
import com.google.gwt.user.client.ui.VerticalPanel;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JobsListViewImpl implements JobsListView
{
   JobsListViewImpl(VerticalPanel list)
   {
      jobs_ = new HashMap<>();
      list_ = list;
   }
   
   @Override
   public void addJob(Job job, Consumer<JobItem> onAddedItem)
   {
      if (jobs_.containsKey(job.id))
         return;
   
      JobItem item = new JobItem(job);
      jobs_.put(job.id, item);
      list_.insert(item, 0);
      
      if (onAddedItem != null)
         onAddedItem.accept(item);
   }
   
   @Override
   public void insertJob(Job job, Consumer<JobItem> onInsertedItem)
   {
      if (jobs_.containsKey(job.id))
         return;
      
      JobItem item = new JobItem(job);
      jobs_.put(job.id, item);
      
      // keep list sorted with most recently recorded jobs first
      int i;
      for (i = 0; i < list_.getWidgetCount(); i++)
      {
         if (((JobItem)list_.getWidget(i)).getJob().recorded <= job.recorded)
            break;
      }
      list_.insert(item, i);
      
      if (onInsertedItem != null)
         onInsertedItem.accept(item);
   }
   
   @Override
   public void removeJob(Job job, Command onRemoved)
   {
      if (!jobs_.containsKey(job.id))
         return;
      
      list_.remove(jobs_.get(job.id));
      jobs_.remove(job.id);
      
      if (onRemoved != null)
         onRemoved.execute();
   }
   
   @Override
   public void updateJob(Job job)
   {
      if (!jobs_.containsKey(job.id))
         return;
      jobs_.get(job.id).update(job);
   }
   
   @Override
   public void clear()
   {
      list_.clear();
      jobs_.clear();
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
      for (JobItem item: jobs_.values())
      {
         item.syncTime(timestamp);
      }
   }
   
   @Override
   public Job getJob(String id)
   {
      if (jobs_.containsKey(id))
         return jobs_.get(id).getJob();
      return null;
   }
   
   @Override
   public int jobCount()
   {
      return jobs_.size();
   }
   
   @Override
   public List<Job> getJobs()
   {
      ArrayList<Job> jobList = new ArrayList<>();

      // return jobs in same order they are displayed
      for (int i = 0; i < list_.getWidgetCount(); i++)
      {
         JobItem jobItem = (JobItem)list_.getWidget(i);
         jobList.add(jobItem.getJob());
      }
      return jobList;
   }

   @Override
   public void setInitialJobs(List<Job> jobs)
   {
       // clear any current state
      clear();
     
      // sort jobs by most recently recorded first
      List<Job> sortedJobs = jobs;
      sortedJobs.sort(Comparator.comparingInt(j -> j.recorded));
      
      // add each to the panel
      for (Job job: sortedJobs)
      {
         addJob(job, null);
      }
   }
   
   private final Map<String, JobItem> jobs_;
   private final VerticalPanel list_;
}
