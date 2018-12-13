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
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.jobs.events.JobExecuteActionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
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
      @Source("spinner_2x.png")
      ImageResource jobSpinner();

      @Source("select_2x.png")
      ImageResource jobSelect();

      @Source("info_2x.png")
      ImageResource jobInfo();
   
      @Source("launcher_2x.png")
      ImageResource launcherJobType();
   }
   
   public interface Styles extends CssResource
   {
      String item();
      String metadata();
      String name();
      String running();
      String succeeded();
      String cancelled();
      String pending();
      String state();
      String idle();
      String progressHost();
      String elapsed();
      String spinner();
      String outer();
      String panel();
      String select();
      String status();
      String progress();
      String failed();
      String jobType();
   }

   public JobItem(final Job job)
   {
      stop_ = new ToolbarButton(
            RStudioGinjector.INSTANCE.getCommands().interruptR().getImageResource(), evt ->
            {
               RStudioGinjector.INSTANCE.getEventBus().fireEvent(
                     new JobExecuteActionEvent(job.id, JobConstants.ACTION_STOP));
            });
      
      launcherStop_ = new ToolbarButton(
            RStudioGinjector.INSTANCE.getCommands().interruptR().getImageResource(), evt ->
            {
               // TODO: different UI that exposes both "stop" and "kill" if the job
               // supports it. Probably a dialog with a "Force" checkbox which is
               // always deselected by default.
               
               // TODO: also figure out how to incorporate "cancel", which is used
               // to stop a job that is still being scheduled; maybe handle that
               // entirely server-side?
               RStudioGinjector.INSTANCE.getServer().stopLauncherJob(
                     job.id, false /*kill*/, new VoidServerRequestCallback());
            });
      initWidget(uiBinder.createAndBindUi(this));
      
      name_.setText(job.name);
      spinner_.setResource(new ImageResource2x(RESOURCES.jobSpinner()));
      
      if (job.type == JobConstants.JOB_TYPE_LAUNCHER)
      {
         jobType_.setResource(new ImageResource2x(RESOURCES.launcherJobType()));
         jobType_.setTitle("Cluster: " + job.cluster);
      }
      else
         jobType_.setVisible(false);
      
      ImageResource2x detailsImage = new ImageResource2x(RESOURCES.jobSelect());
      if (JsArrayUtil.jsArrayStringContains(job.actions, JobConstants.ACTION_INFO))
      {
         detailsImage = new ImageResource2x(RESOURCES.jobInfo());
      }
      
      select_.setResource(detailsImage);

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
      
      outer_.addStyleName("rstudio-themes-border");
      
      update(job);
   }
   
   public void update(Job job)
   {
      // cache reference to job
      job_ = job;
      
      String clazz = "";
      String state = JobConstants.stateDescription(job_.state);

      switch(job_.state)
      {
         case JobConstants.STATE_RUNNING:
            clazz = styles_.running();
            break;
         case JobConstants.STATE_IDLE:
            clazz = styles_.idle();
            break;
         case JobConstants.STATE_CANCELLED:
            clazz = styles_.cancelled();
            break;
         case JobConstants.STATE_FAILED:
            clazz = styles_.failed();
            break;
         case JobConstants.STATE_SUCCEEDED:
            clazz = styles_.succeeded();
            break;
      }

      // apply the style appropriate for the current state to the row 
      panel_.setStyleName(styles_.panel() + " " + clazz);
      
      // if the job is completed, show when
      if (job_.completed != 0)
      {
         state += " " + StringUtil.friendlyDateTime(new Date(job_.completed * 1000));
      }
      state_.setText(state);

      // sync status and progress
      if (job_.state == JobConstants.STATE_RUNNING)
      {
         if (job.max > 0)
         {
            // show progress bar and status label if the job is running and has
            // a progress bar
            status_.setText(job.status);
            running_.setVisible(true);
         }
         else if (!StringUtil.isNullOrEmpty(job.status))
         {
            // no progress bar -- but we have a status, so show that as the
            // running state
            state_.setText(job.status);
            running_.setVisible(false);
         }
         else
         {
            running_.setVisible(false);
         }
      }
      else
      {
         // not running; hide the progress area
         running_.setVisible(false);
      }
      
      // show the state if we're not showing the progress bar
      state_.setVisible(!running_.isVisible());
      
      // show stop button if job has a "stop" action, and is not completed
      if (job_.completed == 0)
      {
         if (job_.type == JobConstants.JOB_TYPE_LAUNCHER)
         {
            stop_.setVisible(false);
            launcherStop_.setVisible(true);
         }
         else
         {
            stop_.setVisible(
                  JsArrayUtil.jsArrayStringContains(job_.actions, JobConstants.ACTION_STOP) &&
                  job_.completed == 0);
            launcherStop_.setVisible(false);
         }
      }
      else
      {
         stop_.setVisible(false);
         launcherStop_.setVisible(false);
      }
      
      // update progress bar if it's showing
      if (running_.isVisible())
      {
         double percent = ((double)job.progress / (double)job.max) * 100.0;
         progress_.setProgress(percent);
      }
      
      // show spinner if actively executing
      spinner_.setVisible(job.state == JobConstants.STATE_RUNNING);
      
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
   @UiField Image spinner_;
   @UiField Image jobType_;
   @UiField Label elapsed_;
   @UiField Label name_;
   @UiField Label status_;
   @UiField Label state_;
   @UiField VerticalPanel running_;
   @UiField HorizontalPanel outer_;
   @UiField FocusPanel panel_;
   @UiField(provided=true) ToolbarButton stop_;
   @UiField(provided=true) ToolbarButton launcherStop_;
   @UiField Styles styles_;
}
