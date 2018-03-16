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
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class JobsPane extends WorkbenchPane 
                      implements JobsPresenter.Display
{
   @Inject
   public JobsPane()
   {
      super("Jobs");

      // create widget
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      list_ = new JobsList();
      list_.addStyleName("ace_editor_theme");
      return list_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar mainToolbar = new Toolbar();
      return mainToolbar;
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

   JobsList list_;
}
