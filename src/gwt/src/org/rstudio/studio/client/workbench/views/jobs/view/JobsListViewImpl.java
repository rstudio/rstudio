/*
 * JobsListViewImpl.java
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

import com.google.gwt.user.client.ui.VerticalPanel;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobsListViewImpl
{
   JobsListViewImpl(VerticalPanel list)
   {
      jobs_ = new HashMap<>();
      list_ = list;
   }
   
   public boolean addJob(JobItemView item)
   {
      if (hasJob(item.getJob().id))
         return false;

      jobs_.put(item.getJob().id, item);
      list_.insert(item, 0);
      return true;
   }
   
   public boolean insertJob(JobItemView item)
   {
      if (hasJob(item.getJob().id))
         return false;
      
      // keep list sorted with most recently recorded jobs first
      int i;
      for (i = 0; i < list_.getWidgetCount(); i++)
      {
         if (((JobItemView)list_.getWidget(i)).getJob().recorded <= item.getJob().recorded)
            break;
      }
      insertJobAt(item, i);
      return true;
   }
   
   public boolean insertJobAt(JobItemView item, int position)
   {
      if (hasJob(item.getJob().id))
         return false;
      
      jobs_.put(item.getJob().id, item);
      list_.insert(item, position);
      return true;
   }
   
   public boolean removeJob(Job job)
   {
      if (!jobs_.containsKey(job.id))
         return false;
      
      list_.remove(jobs_.get(job.id));
      jobs_.remove(job.id);
      return true;
   }
   
   public void updateJob(Job job)
   {
      if (!jobs_.containsKey(job.id))
         return;
      jobs_.get(job.id).update(job);
   }
   
   public void clear()
   {
      list_.clear();
      jobs_.clear();
   }
   
   public void syncElapsedTime(int timestamp)
   {
      for (JobItemView item: jobs_.values())
      {
         item.syncTime(timestamp);
      }
   }
   
   public Job getJob(String id)
   {
      if (jobs_.containsKey(id))
         return jobs_.get(id).getJob();
      return null;
   }
   
   public int jobCount()
   {
      return jobs_.size();
   }
   
   public List<Job> getJobs()
   {
      ArrayList<Job> jobList = new ArrayList<>();

      // return jobs in same order they are displayed
      for (int i = 0; i < list_.getWidgetCount(); i++)
      {
         JobItemView jobItem = (JobItemView)list_.getWidget(i);
         jobList.add(jobItem.getJob());
      }
      return jobList;
   }

   public boolean hasJob(String id)
   {
      return jobs_.containsKey(id);
   }
   
   private final Map<String, JobItemView> jobs_;
   private final VerticalPanel list_;
}
