/*
 * JobItem.java
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.FireEvents;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsSubset;
import org.rstudio.studio.client.workbench.views.jobs.events.JobExecuteActionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class JobItem extends Composite implements JobItemView
{
   private static JobItemUiBinder uiBinder = GWT.create(JobItemUiBinder.class);

   interface JobItemUiBinder extends UiBinder<Widget, JobItem>
   {
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
      String noSelect();
      String status();
      String progress();
      String failed();
   }

   public interface Preferences
   {
      boolean reducedMotion();
   }

   public static class PreferencesImpl extends UserPrefsSubset
                                       implements Preferences
   {
      @Inject
      public PreferencesImpl(Provider<UserPrefs> pUserPrefs)
      {
         super(pUserPrefs);
      }

      @Override
      public boolean reducedMotion()
      {
         return getUserPrefs().reducedMotion().getValue();
      }
   }

   @Inject
   public JobItem(@Assisted Job job, FireEvents eventBus, Preferences prefs)
   {
      eventBus_ = eventBus;
      stop_ = new ToolbarButton(ToolbarButton.NoText, "Stop job", new ImageResource2x(RESOURCES.jobCancel()), evt ->
      {
         eventBus_.fireEvent(new JobExecuteActionEvent(job.id, JobConstants.ACTION_STOP));
      });
      
      initWidget(uiBinder.createAndBindUi(this));
      
      name_.setText(job.name);
      progress_.setLabel(job.name);
      spinner_.setResource(new ImageResource2x(RESOURCES.jobSpinner()));

      ImageResource2x detailsImage = new ImageResource2x(RESOURCES.jobSelect());
      if (JsArrayUtil.jsArrayStringContains(job.actions, JobConstants.ACTION_INFO))
      {
         select_.addStyleName(styles_.noSelect());
      }
      
      select_.setResource(detailsImage);
      select_.setAltText("Select Job");

      ClickHandler selectJob = evt ->
      {
         if (DomUtils.isDescendant(
               Element.as(evt.getNativeEvent().getEventTarget()),
                   stop_.getElement()))
         {
            // ignore clicks occurring inside the stop button
            return;
         }
         eventBus_.fireEvent(new JobSelectionEvent(job.id, job.type, true, !prefs.reducedMotion()));
      };
      select_.addClickHandler(selectJob);
      panel_.addClickHandler(selectJob);
      
      outer_.addStyleName("rstudio-themes-border");
      
      update(job);
   }
   
   @Override
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
         stop_.setVisible(
               JsArrayUtil.jsArrayStringContains(job_.actions, JobConstants.ACTION_STOP) &&
               job_.completed == 0);
      }
      else
      {
         stop_.setVisible(false);
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
   
   @Override
   public Job getJob()
   {
      return job_;
   }
   
   @Override
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
   @UiField Label elapsed_;
   @UiField Label name_;
   @UiField Label status_;
   @UiField Label state_;
   @UiField VerticalPanel running_;
   @UiField HorizontalPanel outer_;
   @UiField FocusPanel panel_;
   @UiField(provided=true) ToolbarButton stop_;
   @UiField Styles styles_;
   
   // injected
   private final FireEvents eventBus_;
}
