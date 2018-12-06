/*
 * JobsPane.java
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
import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UIPrefMenuItem;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.jobs.JobsPresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;

import java.util.ArrayList;
import java.util.Comparator;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class JobsPane extends WorkbenchPane 
                      implements JobsPresenter.Display
{
   @Inject
   public JobsPane(Commands commands,
                   UIPrefs uiPrefs,
                   final EventBus events)
   {
      super("Jobs");
      commands_ = commands;
      events_ = events;
      uiPrefs_ = uiPrefs;

      allJobs_ = new ToolbarButton(
            commands.helpBack().getImageResource(), evt ->
      {
         // deselect current job 
         events.fireEvent(new JobSelectionEvent(current_, false, true));
      });
      allJobs_.setTitle("View all jobs");
      
      toolbar_ = new Toolbar();
      installMainToolbar();

      // create widget
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      list_ = new JobsList();
      output_ = new JobOutputPanel();

      panel_ = new SlidingLayoutPanel(list_, output_);
      panel_.addStyleName("ace_editor_theme");
      
      return panel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      return toolbar_;
   }

   @Override
   public void updateJob(int type, Job job)
   {
      switch(type)
      {
         case JobConstants.JOB_ADDED:
            list_.insertJob(job);
            
            if (job.show)
            {
               // bring the pane to the front so the new job is visible
               bringToFront();
            
               // select the job
               events_.fireEvent(new JobSelectionEvent(job.id, true, false));
            }
            
            break;

         case JobConstants.JOB_REMOVED:
            // if this is the job we're currently tracking, do so no longer
            if (job.id == current_)
            {
               hideJobOutput(job.id, true);
            }
            
            // clean up 
            list_.removeJob(job);
            break;

         case JobConstants.JOB_UPDATED:
            list_.updateJob(job);
            if (job.id == current_)
            {
               if (job.completed > 0 && progress_ != null)
               {
                  progress_.setComplete();
               }
               else
               {
                  // update progress
                  if (progress_ == null)
                  {
                     progress_ = new JobProgress();
                     toolbar_.addLeftWidget(progress_);
                  }
                  progress_.showJob(job);
               }
            }
            break;
            
         default:
            Debug.logWarning("Unrecognized job update type " + type);
      }
   }
   
   @Override
   public void setInitialJobs(JsObject jobs)
   {
      // clear any current state
      list_.clear();
      
      if (jobs == null)
         return;

      // make an array of all the jobs on the server
      ArrayList<Job> items = new ArrayList<>();
      for (String id: jobs.iterableKeys())
      {
         items.add(jobs.getElement(id));
      }
      
      // sort jobs by most recently recorded first
      items.sort(Comparator.comparingInt(j -> j.recorded));
      
      // add each to the panel
      for (Job job: items)
      {
         list_.addJob(job);
      }
   }

   @Override
   public void showJobOutput(String id, JsArray<JobOutput> output, boolean animate)
   {
      // clear any existing output in the pane
      output_.clearOutput();

      // display all the output, but don't scroll as we go
      for (int i = 0; i < output.length(); i++)
      {
         output_.showOutput(CompileOutput.create(
               output.get(i).type(), 
               output.get(i).output()), false /* scroll */);
      }
      
      // scroll to show all output so far
      output_.scrollToBottom();
      
      // remove the progress for the current job if we're showing it
      if (progress_ != null)
      {
         toolbar_.removeLeftWidget(progress_);
         progress_ = null;
      }

      // save job id as current job
      current_ = id;

      // show the output pane
      panel_.slideWidgets(SlidingLayoutPanel.Direction.SlideRight,
            animate, this::installJobToolbar);
   }

   @Override
   public void addJobOutput(String id, int type, String output)
   {
      // make sure this output belongs to the job currently being displayed
      if (current_ == null || id != current_)
      {
         Debug.logWarning("Attempt to show output for incorrect job '" + 
                          id + "'.");
         return;
      }
      
      // add the output
      output_.showOutput(CompileOutput.create(type, output), true /* scroll */);
   }
   

   @Override
   public void hideJobOutput(String id, boolean animate)
   {
      // make sure this output belongs to the job currently being displayed
      if (current_ == null || id != current_)
      {
         Debug.logWarning("Attempt to hide output for incorrect job '" + 
                          id + "'.");
         return;
      }
      
      panel_.slideWidgets(SlidingLayoutPanel.Direction.SlideLeft,
            animate, this::installMainToolbar);
      
      current_ = null;
   }

   @Override
   public void syncElapsedTime(int timestamp)
   {
      // update list of running jobs
      list_.syncElapsedTime(timestamp);
      
      // update progress of current job if present
      if (progress_ != null)
      {
         progress_.updateElapsed(timestamp);
      }
   }
   
   @Override
   public void bringToFront()
   {
      setShowJobsTabPref(true);
      super.bringToFront();
   }
   
   @Override
   public void setShowJobsTabPref(boolean show)
   {
      if (uiPrefs_.showJobsTab().getValue() != show)
      {
         uiPrefs_.showJobsTab().setGlobalValue(show);
         uiPrefs_.writeUIPrefs();
      }
   }
   
   @Override
   public void refreshPaneStatusMessage()
   {
      paneStatus_.setText(uiPrefs_.hideCompletedJobs().getValue() ? "(previously completed jobs hidden)" : "");
   }

   // Private methods ---------------------------------------------------------
   
   private void installJobToolbar()
   {
      toolbar_.removeAllWidgets();
      toolbar_.addLeftWidget(allJobs_);

      // if we're currently tracking a job:
      if (current_ != null)
      {
         // show the progress bar if the job hasn't been completed yet
         Job job = list_.getJob(current_);

         // clear previous progress
         if (progress_ != null)
            toolbar_.removeLeftWidget(progress_);
         
         // show progress
         progress_ = new JobProgress();
         toolbar_.addLeftWidget(progress_);
         progress_.showJob(job);

         // if job is complete, mark that
         if (job.completed > 0)
         {
            progress_.setComplete();
         }
      }
   }
   
   private void installMainToolbar()
   {
      toolbar_.removeAllWidgets();
      toolbar_.addLeftWidget(commands_.startJob().createToolbarButton());
      toolbar_.addLeftSeparator();

      // More
      StandardIcons icons = StandardIcons.INSTANCE;
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands_.clearJobs().createMenuItem(false));
      moreMenu.addItem(new UIPrefMenuItem<Boolean>(
            uiPrefs_.hideCompletedJobs(), true, "Hide Previously Completed Jobs", uiPrefs_));
      
      ToolbarButton moreButton = new ToolbarButton("More",
                                                  new ImageResource2x(icons.more_actions2x()),
                                                  moreMenu);

      toolbar_.addLeftWidget(moreButton);
      
      toolbar_.addLeftSeparator();
      paneStatus_ = new Label();
      paneStatus_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar_.addLeftWidget(paneStatus_);
      refreshPaneStatusMessage();
      progress_ = null;
   }

   // widgets
   private JobOutputPanel output_;
   private JobsList list_;
   private SlidingLayoutPanel panel_;
   private final Toolbar toolbar_;
   private final ToolbarButton allJobs_;
   private Label paneStatus_;
   private JobProgress progress_;

   // internal state
   private String current_;

   // injected
   private final Commands commands_;
   private final EventBus events_;
   private final UIPrefs uiPrefs_;
}
