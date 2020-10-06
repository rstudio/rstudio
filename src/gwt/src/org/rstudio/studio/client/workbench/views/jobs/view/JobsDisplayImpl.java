/*
 * JobsDisplayImpl.java
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

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.jobs.events.JobSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;

import java.util.List;

/**
 * Implements common behavior for the JobsDisplay methods
 */
public class JobsDisplayImpl implements JobsDisplay
{
   public JobsDisplayImpl(WorkbenchPane pane,
                          JobsPaneOperations widgets)
   {
      pane_ = pane;
      widgets_ = widgets;
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
   }
   
   @Override
   public void updateJob(int updateType, Job job)
   {
      switch (updateType)
      {
         case JobConstants.JOB_ADDED:
            widgets_.insertJob(job);
            
            if (job.show)
            {
               // bring the pane to the front so the new job is visible
               pane_.bringToFront();
            
               // select the job
               events_.fireEvent(new JobSelectionEvent(job.id, job.type, true, false));
            }
            
            break;

         case JobConstants.JOB_REMOVED:
            // if this is the job we're currently tracking, do so no longer
            if (widgets_.isCurrent(job.id))
            {
               hideJobOutput(job.id, true);
            }
            
            // clean up
            widgets_.removeJob(job);
            break;

         case JobConstants.JOB_UPDATED:
            widgets_.updateJob(job);
            if (widgets_.isCurrent(job.id))
            {
               widgets_.showProgress(job);
            }
            break;
            
         default:
            Debug.logWarning("Unrecognized job update type " + updateType);
      }
   }
   
   @Override
   public void setInitialJobs(List<Job> jobs)
   {
      widgets_.setInitialJobs(jobs);
   }
   
   @Override
   public void showJobOutput(String id, JsArray<JobOutput> output, boolean animate)
   {
       // clear any existing output in the pane
      widgets_.getOutputPanel().clearOutput();

      // display all the output, but don't scroll as we go
      for (int i = 0; i < output.length(); i++)
      {
         widgets_.getOutputPanel().showOutput(CompileOutput.create(
               output.get(i).type(),
               output.get(i).output()), false /* scroll */);
      }
      
      // scroll to show all output so far
      widgets_.getOutputPanel().scrollToBottom();
      
      // remove the progress for the current job if we're showing it
      widgets_.removeProgressWidget();

      // save job id as current job
      widgets_.setCurrent(id);

      // show the output pane
      widgets_.getPanel().slideWidgets(SlidingLayoutPanel.Direction.SlideRight,
            animate, widgets_::installJobToolbar);
   }
   
   @Override
   public void addJobOutput(String id, int type, String output)
   {
      // make sure this output belongs to the job currently being displayed
      if (!widgets_.isCurrent(id))
      {
         Debug.logWarning("Attempt to show output for incorrect job '" +
                          id + "'.");
         return;
      }
      
      // add the output
      widgets_.getOutputPanel().showOutput(CompileOutput.create(type, output), true /* scroll */);
   }
   
   @Override
   public void hideJobOutput(String id, boolean animate)
   {
       // make sure this output belongs to the job currently being displayed
      if (!widgets_.isCurrent(id))
      {
         Debug.logWarning("Attempt to hide output for incorrect job '" +
                          id + "'.");
         return;
      }
      
      widgets_.getPanel().slideWidgets(SlidingLayoutPanel.Direction.SlideLeft,
            animate, widgets_::installMainToolbar);
      
      widgets_.setCurrent(null);
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
      // update list of running jobs
      widgets_.syncElapsedTime(timestamp);
      
      // update progress of current job if present
      widgets_.updateProgress(timestamp);
   }
   
   @Override
   public void setShowTabPref(boolean show)
   {
   }

   // WorkbenchView overrides; if there is common implementation add here
   // and delegate from owner
  
   @Override
   public void bringToFront()
   {
   
   }
   
   @Override
   public void maximize()
   {
   
   }
   
   @Override
   public void ensureHeight(int height)
   {
   
   }
   
   @Override
   public void setProgress(boolean showProgress)
   {
   
   }
   
   @Override
   public void onBeforeUnselected()
   {
   
   }
   
   @Override
   public void onBeforeSelected()
   {
   
   }
   
   @Override
   public void onSelected()
   {
   }
   
   @Override
   public void setFocus()
   {
      widgets_.focus();
   }

   // private state
   private final WorkbenchPane pane_;
   private final JobsPaneOperations widgets_;
  
   // injected
   private EventBus events_;
}
