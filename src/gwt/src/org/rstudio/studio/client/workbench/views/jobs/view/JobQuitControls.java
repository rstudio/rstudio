/*
 * JobQuitControls.java
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

import java.util.List;

import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class JobQuitControls extends Composite
{

   private static JobQuitControlsUiBinder uiBinder = GWT.create(JobQuitControlsUiBinder.class);

   interface JobQuitControlsUiBinder extends UiBinder<Widget, JobQuitControls>
   {
   }

   public JobQuitControls(List<Job> runningJobs)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      // add running jobs to the list
      for (Job job: runningJobs)
      {
         jobList_.addItem(job.name);
      }
      if (jobList_.getItemCount() > 1)
      {
         jobListLabel_.setText("The following " + jobList_.getItemCount() + " jobs are still running.");
      }
   }

   @UiField FormLabel jobListLabel_;
   @UiField FormListBox jobList_;
}
