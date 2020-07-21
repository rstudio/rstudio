/*
 * JobsPaneOperations.java
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
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import java.util.List;

public interface JobsPaneOperations
{
   Widget createMainWidget();
   void installJobToolbar();
   void installMainToolbar();
   void removeProgressWidget();
   void focus();
   String getCurrent();
   void setCurrent(String current);
   boolean isCurrent(String id);
   void updateProgress(int timestamp);
   void showProgress(Job job);
   void addJob(Job job);
   void setInitialJobs(List<Job> jobs);
   void insertJob(Job job);
   void removeJob(Job job);
   void updateJob(Job job);
   void clear();
   void syncElapsedTime(int timestamp);
   Toolbar getToolbar();
   JobOutputPanel getOutputPanel();
   SlidingLayoutPanel getPanel();
}
