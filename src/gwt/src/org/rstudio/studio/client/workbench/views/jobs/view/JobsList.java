/*
 * JobsList.java
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

import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.user.client.Command;
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

   public JobsList()
   {
      initWidget(uiBinder.createAndBindUi(this));
   
      baseImpl_ = new JobsListViewImpl(list_);
      
      updateVisibility();
   }
   
   @Override
   public void addJob(Job job, Consumer<JobItem> onAddedItem)
   {
      baseImpl_.addJob(job, (item) -> {
         updateVisibility();
         
         if (onAddedItem != null)
            onAddedItem.accept(item);
      });
   }
   
   @Override
   public void insertJob(Job job, Consumer<JobItem> onInsertedItem)
   {
      baseImpl_.insertJob(job, (item) -> {
         updateVisibility();
         
         if (onInsertedItem != null)
            onInsertedItem.accept(item);
      });
   }
   
   @Override
   public void removeJob(Job job, Command onRemoved)
   {
      baseImpl_.removeJob(job, () -> {
         updateVisibility();
         
         if (onRemoved != null)
            onRemoved.execute();
      });
   }
   
   @Override
   public void updateJob(Job job)
   {
      baseImpl_.updateJob(job);
   }
   
   @Override
   public void clear()
   {
      baseImpl_.clear();
      updateVisibility();
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
      baseImpl_.syncElapsedTime(timestamp);
   }
   
   @Override
   public Job getJob(String id)
   {
      return baseImpl_.getJob(id);
   }
   
   @Override
   public int jobCount()
   {
      return baseImpl_.jobCount();
   }
   
   @Override
   public List<Job> getJobs()
   {
      return baseImpl_.getJobs();
   }
   
   @Override
   public void setInitialJobs(List<Job> jobs)
   {
      baseImpl_.setInitialJobs(jobs);
   }
   
   private void updateVisibility()
   {
      scroll_.setVisible(jobCount() > 0);
      empty_.setVisible(jobCount() == 0);
   }
  
   @UiField VerticalPanel list_;
   @UiField Label empty_;
   @UiField ScrollPanel scroll_;

   private final JobsListViewImpl baseImpl_;
}
