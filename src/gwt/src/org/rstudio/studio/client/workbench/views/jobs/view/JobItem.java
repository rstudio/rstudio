/*
 * JobItem.java
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

import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class JobItem extends Composite
{

   private static JobItemUiBinder uiBinder = GWT.create(JobItemUiBinder.class);

   interface JobItemUiBinder extends UiBinder<Widget, JobItem>
   {
   }

   public JobItem(Job job)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      name_.setText(job.name);
      update(job);
   }
   
   public void update(Job job)
   {
      // sync status and progress
      status_.setText(job.status);
      
      if (job.max > 0)
      {
         double percent = ((double)job.progress / (double)job.max) * 100.0;
         bar_.setWidth(percent + "%");
      }
   }
   
   @UiField Label name_;
   @UiField Label status_;
   @UiField HTMLPanel progress_;
   @UiField HTMLPanel bar_;
}
