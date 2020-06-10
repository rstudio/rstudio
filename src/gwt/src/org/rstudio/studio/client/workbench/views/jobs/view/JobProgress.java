/*
 * JobProgress.java
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

import java.util.Date;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.jobs.JobProgressPresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobExecuteActionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.LocalJobProgress;

import com.google.inject.Inject;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class JobProgress extends Composite
                         implements JobProgressPresenter.Display
{

   private static JobProgressUiBinder uiBinder = GWT.create(JobProgressUiBinder.class);

   interface JobProgressUiBinder extends UiBinder<Widget, JobProgress>
   {
   }

   @Inject
   public JobProgress(EventBus eventBus)
   {
      // stop must be defined before calling createAndBindUi
      stop_ = new ToolbarButton(ToolbarButton.NoText,
                                "Stop job",
                                new ImageResource2x(RESOURCES.jobCancel()));
      initWidget(uiBinder.createAndBindUi(this));
      eventBus_ = eventBus;
      complete_ = false;
      
      progress_.setHeight("10px");
      stop_.setVisible(false);
   }
   
   @Override
   public void showProgress(LocalJobProgress progress)
   {
      name_.setText(progress.name());
      progress_.setProgress(progress.units(), progress.max());
      progress_.setLabel(progress.name());
      jobProgress_ = progress;
   }
   
   @Override
   public void showJob(Job job)
   {
      name_.setText(job.name);
      progress_.setLabel(job.name);
      String status = JobConstants.stateDescription(job.state);
      stop_.addClickHandler(clickEvent ->
         eventBus_.fireEvent(new JobExecuteActionEvent(job.id, JobConstants.ACTION_STOP)));

      if (job.completed > 0)
      {
         // Job is not running; show its completion status and time
         progress_.setVisible(false);
         status += " " + StringUtil.friendlyDateTime(new Date(job.completed * 1000));
         elapsed_.setText(StringUtil.conciseElaspedTime(job.completed - job.started));
         status_.setVisible(true);
      }
      else if (job.max > 0)
      {
         // Job is running and has a progress bar; show it and hide the status indicator
         progress_.setVisible(true);
         progress_.setProgress(job.progress, job.max);
         status_.setVisible(false);
      }
      else
      {
         // Job is running but does not have progress; show the status field
         progress_.setVisible(false);
         status_.setVisible(true);
         if (!StringUtil.isNullOrEmpty(job.status))
         {
            // Still running; show its status
            status = job.status;
         }
      }

      // show stop button if job has a "stop" action, and is not completed
      if (job.completed == 0)
         stop_.setVisible(JsArrayUtil.jsArrayStringContains(job.actions, JobConstants.ACTION_STOP));
      else
         stop_.setVisible(false);

      status_.setText(status);
      jobProgress_ = new LocalJobProgress(job);
      complete_ = job.completed > 0;
   }

   @Override
   public void updateElapsed(int timestamp)
   {
      if (jobProgress_ == null || complete_)
         return;
      
      int delta = timestamp - jobProgress_.received();
      elapsed_.setText(StringUtil.conciseElaspedTime(jobProgress_.elapsed() + delta));
   }

   private static final JobResources RESOURCES = GWT.create(JobResources.class);

   @UiField Label name_;
   @UiField ProgressBar progress_;
   @UiField Label elapsed_;
   @UiField(provided=true) ToolbarButton stop_;
   @UiField Label status_;
   
   private final EventBus eventBus_;
   private LocalJobProgress jobProgress_;
   private boolean complete_;
}
