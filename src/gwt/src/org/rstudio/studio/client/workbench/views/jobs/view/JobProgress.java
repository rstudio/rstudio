/*
 * JobProgress.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.jobs.view;

import java.util.Date;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.jobs.JobProgressPresenter;
import org.rstudio.studio.client.workbench.views.jobs.JobsConstants;
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
      // buttons must be defined before calling createAndBindUi
      stop_ = new ToolbarButton(ToolbarButton.NoText,
         constants_.stopJobTitle(),
         new ImageResource2x(RESOURCES.jobCancel()));
      replay_ = new ToolbarButton(ToolbarButton.NoText,
         constants_.replayJobText(),
         new ImageResource2x(RESOURCES.jobReplay()));
      initWidget(uiBinder.createAndBindUi(this));
      eventBus_ = eventBus;
      complete_ = false;
      registrations_ = new HandlerRegistrations();
      id_ = "";
      
      progress_.setHeight("10px");
      stop_.setVisible(false);
      replay_.setVisible(false);
   }
   
   @Override
   public void showProgress(LocalJobProgress progress)
   {
      name_.setText(progress.name());
      progress_.setProgress(progress.units(), progress.max());
      progress_.setLabel(progress.name());
      jobProgress_ = progress;
   }

   /**
    * Adds or updates job information in the widget.
    *
    * @param job The job object to show or update.
    */
   @Override
   public void showJob(Job job)
   {
      name_.setText(job.name);
      progress_.setLabel(job.name);

      String status = JobConstants.stateDescription(job.state);

      // If this job is different than the one we were looking at previously, update our handlers to fire events for
      // the new job.
      if (!StringUtil.equals(id_, job.id))
      {
         // Save new ID
         id_ = job.id;

         // Detach any previous event handlers
         registrations_.removeHandler();

         // Set new button IDs
         ElementIds.assignElementId(stop_,
            ElementIds.JOB_STOP + "_" + ElementIds.idSafeString(job.name));
         ElementIds.assignElementId(replay_,
            ElementIds.JOB_REPLAY + "_" + ElementIds.idSafeString(job.name));

         // Attach new handlers
         registrations_.add(
            stop_.addClickHandler(clickEvent ->
               eventBus_.fireEvent(new JobExecuteActionEvent(job.id, JobConstants.ACTION_STOP))));
         registrations_.add(
            replay_.addClickHandler(clickEvent ->
               eventBus_.fireEvent(new JobExecuteActionEvent(job.id, JobConstants.ACTION_REPLAY))));
      }

      if (job.completed > 0)
      {
         // Job is not running; show its completion status and time
         progress_.setVisible(false);
         status += " " + StringUtil.friendlyDateTime(new Date(job.completed * 1000));
         elapsed_.setText(StringUtil.conciseElaspedTime(job.completed - job.started));
         status_.setVisible(true);
         replay_.setVisible(JsArrayUtil.jsArrayStringContains(job.actions, JobConstants.ACTION_REPLAY));
      }
      else if (job.max > 0)
      {
         // Job is running and has a progress bar; show it and hide the status indicator
         progress_.setVisible(true);
         progress_.setProgress(job.progress, job.max);
         status_.setVisible(false);
         replay_.setVisible(false);
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
         replay_.setVisible(false);
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
   @UiField(provided=true) ToolbarButton replay_;
   @UiField Label status_;

   private HandlerRegistrations registrations_;
   private final EventBus eventBus_;
   private LocalJobProgress jobProgress_;
   private boolean complete_;
   private String id_;
   private static final JobsConstants constants_ = GWT.create(JobsConstants.class);
}
