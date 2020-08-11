/*
 * JobsPaneWidgets.java
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

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import java.util.Comparator;
import java.util.List;

public class JobsPaneWidgets implements JobsPaneOperations
{
   @Inject
   public JobsPaneWidgets(Commands commands,
                          EventBus events,
                          UserPrefs userPrefs,
                          JobsList list)
   {
      commands_ = commands;
      events_ = events;
      userPrefs_ = userPrefs;
      list_ = list;

      toolbar_ = new Toolbar("Jobs Tab");
      
      allJobs_ = new ToolbarButton(
            ToolbarButton.NoText,
            "View all jobs",
            commands_.helpBack().getImageResource(), evt ->
      {
         // deselect current job
         events_.fireEvent(new JobSelectionEvent(current_,
               JobConstants.JOB_TYPE_SESSION, false, !userPrefs_.reducedMotion().getValue()));
      });
      
      installMainToolbar();
   }
   
   @Override
   public Widget createMainWidget()
   {
      output_ = new JobOutputPanel();

      panel_ = new SlidingLayoutPanel(list_, output_);
      panel_.addStyleName("ace_editor_theme");

      return panel_;
   }
   
   @Override
   public void installJobToolbar()
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
         progress_ = new JobProgress(events_);
         toolbar_.addLeftWidget(progress_);
         progress_.showJob(job);
      }
   }
   
   @Override
   public void installMainToolbar()
   {
      toolbar_.removeAllWidgets();
      toolbar_.addLeftWidget(startButton_ = commands_.startJob().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.clearJobs().createToolbarButton());
      progress_ = null;
   }
   
   @Override
   public void removeProgressWidget()
   {
      if (progress_ != null)
      {
         toolbar_.removeLeftWidget(progress_);
         progress_ = null;
      }
   }
  
   @Override
   public String getCurrent()
   {
      return current_;
   }
   
   @Override
   public void setCurrent(String current)
   {
      current_ = current;
   }
   
   @Override
   public boolean isCurrent(String id)
   {
      return StringUtil.equals(current_, id);
   }
   
   @Override
   public void updateProgress(int timestamp)
   {
      if (progress_ != null)
      {
         progress_.updateElapsed(timestamp);
      }
   }

   @Override
   public void showProgress(Job job)
   {
      if (progress_ == null)
      {
         progress_ = new JobProgress(events_);
         toolbar_.addLeftWidget(progress_);
      }
      progress_.showJob(job);
   }
   
   @Override
   public void setInitialJobs(List<Job> jobs)
   {
      // clear any current state
      clear();
     
      // sort jobs by most recently recorded first
      List<Job> sortedJobs = jobs;
      sortedJobs.sort(Comparator.comparingInt(j -> j.recorded));
      
      // add each to the panel
      for (Job job: sortedJobs)
      {
         addJob(job);
      }
   }
   
   @Override
   public void addJob(Job job)
   {
      list_.addJob(job);
   }

   @Override
   public void insertJob(Job job)
   {
      list_.insertJob(job);
   }
   
   @Override
   public void removeJob(Job job)
   {
      list_.removeJob(job);
   }
   
   @Override
   public void updateJob(Job job)
   {
      list_.updateJob(job);
   }
   
   @Override
   public void clear()
   {
      list_.clear();
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
      list_.syncElapsedTime(timestamp);
   }

   @Override
   public Toolbar getToolbar()
   {
      return toolbar_;
   }
   
   @Override
   public JobOutputPanel getOutputPanel()
   {
      return output_;
   }
   
   @Override
   public SlidingLayoutPanel getPanel()
   {
      return panel_;
   }

   @Override
   public void focus()
   {
      startButton_.setFocus(true);
   }

   // widgets
   private JobOutputPanel output_;
   private SlidingLayoutPanel panel_;
   private final Toolbar toolbar_;
   private final ToolbarButton allJobs_;
   private JobProgress progress_;
   private ToolbarButton startButton_;

   // internal state
   private String current_;
   
   // injected
   private final Commands commands_;
   private final EventBus events_;
   private final UserPrefs userPrefs_;
   private final JobsList list_;
}
