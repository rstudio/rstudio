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
import org.rstudio.studio.client.workbench.views.jobs.JobsPresenter;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;

import java.util.ArrayList;
import java.util.Collections;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.CompileOutputBufferWithHighlight;
import org.rstudio.studio.client.common.compile.CompilePanel;
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
                   final EventBus events)
   {
      super("Jobs");

      allJobs_ = new ToolbarButton(
            commands.helpBack().getImageResource(), evt ->
      {
         // deselect current job 
         events.fireEvent(new JobSelectionEvent(current_, false));
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
      output_ = new CompilePanel(new CompileOutputBufferWithHighlight());

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
            list_.addJob(job);
            break;

         case JobConstants.JOB_REMOVED:
            list_.removeJob(job);
            break;

         case JobConstants.JOB_UPDATED:
            list_.updateJob(job);
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
      ArrayList<Job> items = new ArrayList<Job>();
      for (String id: jobs.iterableKeys())
      {
         items.add((Job)jobs.getElement(id));
      }
      
      // sort jobs by most recently recorded first
      Collections.sort(items, (j1, j2) -> 
      {
          return j1.recorded - j2.recorded;
      });
      
      // add each to the panel
      for (Job job: items)
      {
         list_.addJob(job);
      }
   }

   @Override
   public void showJobOutput(String id, JsArray<JobOutput> output)
   {
      // cleear any existing output in the pane
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

      // show the output pane
      panel_.slideWidgets(SlidingLayoutPanel.Direction.SlideRight, true, () -> 
      {
         installJobToolbar();
      });
      
      // save job id as current job
      current_ = id;
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
   public void hideJobOutput(String id)
   {
      // make sure this output belongs to the job currently being displayed
      if (current_ == null || id != current_)
      {
         Debug.logWarning("Attempt to hide output for incorrect job '" + 
                          id + "'.");
         return;
      }
      
      panel_.slideWidgets(SlidingLayoutPanel.Direction.SlideLeft, true, () -> 
      {
         installMainToolbar();
      });
      
      current_ = null;
   }

   @Override
   public void syncElapsedTime(int timestamp)
   {
      list_.syncElapsedTime(timestamp);
   }

   // Private methods ---------------------------------------------------------
   
   private void installJobToolbar()
   {
      toolbar_.removeAllWidgets();
      toolbar_.addLeftWidget(allJobs_);
   }
   
   private void installMainToolbar()
   {
      toolbar_.removeAllWidgets();
   }

   // widgets
   CompilePanel output_;
   JobsList list_;
   SlidingLayoutPanel panel_;
   Toolbar toolbar_;
   ToolbarButton allJobs_;

   // internal state
   String current_;
}
