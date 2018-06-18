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

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.jobs.events.JobExecuteActionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class JobItem extends Composite
{
   private static JobItemUiBinder uiBinder = GWT.create(JobItemUiBinder.class);

   interface JobItemUiBinder extends UiBinder<Widget, JobItem>
   {
   }
   
   public interface JobResources extends ClientBundle
   {
      @Source("idle_2x.png")
      ImageResource jobIdle();

      @Source("running_2x.png")
      ImageResource jobRunning();

      @Source("succeeded_2x.png")
      ImageResource jobSucceeded();

      @Source("failed_2x.png")
      ImageResource jobFailed();

      @Source("select_2x.png")
      ImageResource jobSelect();
   }

   public JobItem(final Job job)
   {
      stop_ = new ToolbarButton(
            RStudioGinjector.INSTANCE.getCommands().interruptR().getImageResource(), evt ->
            {
               RStudioGinjector.INSTANCE.getEventBus().fireEvent(
                     new JobExecuteActionEvent(job.id, JobConstants.ACTION_STOP));
            });
      initWidget(uiBinder.createAndBindUi(this));
      
      name_.setText(job.name);
      select_.setResource(new ImageResource2x(RESOURCES.jobSelect()));
      ClickHandler selectJob = evt -> 
      {
         if (DomUtils.isDescendant(
               Element.as(evt.getNativeEvent().getEventTarget()),
               running_.getElement()))
         {
            // ignore clicks occurring inside the progress area
            return;
         }
         RStudioGinjector.INSTANCE.getEventBus().fireEvent(
               new JobSelectionEvent(job.id, true, true));
      };
      select_.addClickHandler(selectJob);
      panel_.addClickHandler(selectJob);
      
      running_.addStyleName("rstudio-themes-background");
      outer_.addStyleName("rstudio-themes-border");
      
      update(job);
   }
   
   public void update(Job job)
   {
      // cache reference to job
      job_ = job;

      // sync status and progress
      status_.setText(job.status);
      
      // show progress bar if the job is running
      running_.setVisible(
            job.max > 0 && 
            job_.state == JobConstants.STATE_RUNNING);
      
      // show stop button if job has a "stop" action, and is not completed
      stop_.setVisible(
            JsArrayUtil.jsArrayStringContains(job_.actions, JobConstants.ACTION_STOP) &&
            job_.completed == 0);
      
      // udpate progress bar if it's showing
      if (running_.isVisible())
      {
         double percent = ((double)job.progress / (double)job.max) * 100.0;
         progress_.setProgress(percent);
      }
      
      // sync image
      switch(job.state)
      {
         case JobConstants.STATE_IDLE:
            state_.setResource(new ImageResource2x(RESOURCES.jobIdle()));
            break;
         case JobConstants.STATE_RUNNING:
            state_.setResource(new ImageResource2x(RESOURCES.jobRunning()));
            break;
         case JobConstants.STATE_CANCELLED:
         case JobConstants.STATE_FAILED:
            state_.setResource(new ImageResource2x(RESOURCES.jobFailed()));
            break;
         case JobConstants.STATE_SUCCEEDED:
            state_.setResource(new ImageResource2x(RESOURCES.jobSucceeded()));
            break;
      }
      
      // sync elapsed time to current time
      syncTime((int)((new Date()).getTime() * 0.001));
   }
   
   public Job getJob()
   {
      return job_;
   }
   
   public void syncTime(int timestamp)
   {
      // if job is not running, we have nothing to do
      if (job_.state == JobConstants.STATE_IDLE)
      {
         elapsed_.setText("Waiting");
         return;
      }
      
      // only use timestamp if job is still running
      int delta = 0;
      if (job_.state == JobConstants.STATE_RUNNING)
         delta = timestamp - job_.received;
      
      // display the server's understanding of elapsed time plus the amount of
      // time that has elapsed on the client
      elapsed_.setText(StringUtil.conciseElaspedTime(job_.elapsed + delta));
   }
   
   private Job job_;
   private static final JobResources RESOURCES = GWT.create(JobResources.class);
   
   @UiField ProgressBar progress_;
   @UiField Image select_;
   @UiField Image state_;
   @UiField Label elapsed_;
   @UiField Label name_;
   @UiField Label status_;
   @UiField VerticalPanel running_;
   @UiField HorizontalPanel outer_;
   @UiField FocusPanel panel_;
   @UiField(provided=true) ToolbarButton stop_;
}
