/*
 * JobsList.java
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
package org.rstudio.studio.client.workbench.views.jobs.view;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class JobsList extends Composite
{

   private static JobsListUiBinder uiBinder = GWT.create(JobsListUiBinder.class);

   interface JobsListUiBinder extends UiBinder<Widget, JobsList>
   {
   }

   public JobsList()
   {
      jobs_ = new HashMap<String, JobItem>();

      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void addJob(Job job)
   {
      if (jobs_.containsKey(job.id))
         return;
      JobItem item = new JobItem(job);
      jobs_.put(job.id, item);
      list_.add(item);
   }
   
   public void removeJob(Job job)
   {
      if (!jobs_.containsKey(job.id))
         return;
      list_.remove(jobs_.get(job.id));
      jobs_.remove(job.id);
   }
   
   public void updateJob(Job job)
   {
      if (!jobs_.containsKey(job.id))
         return;
      jobs_.get(job.id).update(job);
   }

   @UiField VerticalPanel list_;

   private final Map<String, JobItem> jobs_;
}
