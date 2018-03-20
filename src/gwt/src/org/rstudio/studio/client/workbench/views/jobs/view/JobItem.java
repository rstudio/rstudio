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

import java.util.Date;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
      
      // no ranged progress
      if (job.max <= 0)
      {
        progressHost_.setVisible(false);
      }
      update(job);
   }
   
   public void update(Job job)
   {
      // cache reference to job
      job_ = job;

      // sync status and progress
      status_.setText(job.status);
      
      if (job.max > 0 && progressHost_.isVisible())
      {
         double percent = ((double)job.progress / (double)job.max) * 100.0;
         bar_.setWidth(percent + "%");
      }
      
      // sync elapsed time to current time
      syncTime((int)((new Date()).getTime() * 0.001));
   }
   
   public void syncTime(int timestamp)
   {
      // if job is not running, we have nothing to do
      if (job_.state == JobConstants.STATE_IDLE)
      {
         elapsed_.setText("");
         return;
      }
      
      // display the server's understanding of elapsed time plus the amount of
      // time that has elapsed on the client
      elapsed_.setText(StringUtil.conciseElaspedTime(job_.elapsed + (timestamp - job_.received)));
   }
   
   private Job job_;
   
   @UiField Label name_;
   @UiField Label status_;
   @UiField Label elapsed_;
   @UiField HorizontalPanel progressHost_;
   @UiField HTMLPanel progress_;
   @UiField HTMLPanel bar_;
}
