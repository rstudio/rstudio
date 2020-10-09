/*
 * JobsDisplay.java
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
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.JobOutput;

import java.util.List;

public interface JobsDisplay extends WorkbenchView
{
   void updateJob(int updateType, Job job);
   void setInitialJobs(List<Job> jobs);
   void showJobOutput(String id, JsArray<JobOutput> output, boolean animate);
   void addJobOutput(String id, int type, String output);
   void hideJobOutput(String id, boolean animate);
   void syncElapsedTime(int timestamp);
   void setShowTabPref(boolean show);
   void setFocus();
}
