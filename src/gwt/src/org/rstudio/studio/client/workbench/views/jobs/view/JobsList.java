/*
 * JobsList.java
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

import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class JobsList extends Composite
                      implements JobsListView
{
   private static JobsListUiBinder uiBinder = GWT.create(JobsListUiBinder.class);

   interface JobsListUiBinder extends UiBinder<Widget, JobsList>
   {
   }

   @Inject
   public JobsList(JobItemFactory jobItemFactory)
   {
      jobItemFactory_ = jobItemFactory;
      initWidget(uiBinder.createAndBindUi(this));
   
      listImpl_ = new JobsListViewImpl(list_);
      
      updateVisibility();
   }
   
   @Override
   public boolean addJob(Job job)
   {
      if (listImpl_.hasJob(job.id))
         return false;
   
      listImpl_.addJob(jobItemFactory_.create(job)); 
      updateVisibility();
      return true;
   }
   
   @Override
   public boolean insertJob(Job job)
   {
      if (listImpl_.hasJob(job.id))
         return false;
      
      listImpl_.insertJob(jobItemFactory_.create(job));
      updateVisibility();
      return true;
   }
   
   @Override
   public boolean removeJob(Job job)
   {
      if (!listImpl_.removeJob(job))
         return false;
      
      updateVisibility();
      return true;         
   }
   
   @Override
   public void updateJob(Job job)
   {
      listImpl_.updateJob(job);
   }
   
   @Override
   public void clear()
   {
      listImpl_.clear();
      updateVisibility();
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
      listImpl_.syncElapsedTime(timestamp);
   }
   
   @Override
   public Job getJob(String id)
   {
      return listImpl_.getJob(id);
   }
   
   @Override
   public int jobCount()
   {
      return listImpl_.jobCount();
   }
   
   @Override
   public List<Job> getJobs()
   {
      return listImpl_.getJobs();
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
         addJob(job);
      }
   }
   
   private void updateVisibility()
   {
      scroll_.setVisible(jobCount() > 0);
      empty_.setVisible(jobCount() == 0);
   }
  
   @UiField VerticalPanel list_;
   @UiField Label empty_;
   @UiField ScrollPanel scroll_;

   private final JobsListViewImpl listImpl_;
   
   // injected
   private final JobItemFactory jobItemFactory_;
}
