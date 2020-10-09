/*
 * LauncherJobsPaneWidgets.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.SlidingLayoutPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import java.util.List;

public class LauncherJobsPaneWidgets implements JobsPaneOperations
{
   @Inject
   public LauncherJobsPaneWidgets()
   {
   }
   
   @Override
   public Widget createMainWidget()
   {
      return new Label();
   }
   
   @Override
   public void installJobToolbar()
   {
   }
   
   @Override
   public void installMainToolbar()
   {
   }
   
   @Override
   public void removeProgressWidget()
   {
   }

   @Override
   public void focus()
   {
   }

   @Override
   public String getCurrent()
   {
      return null;
   }
   
   @Override
   public void setCurrent(String current)
   {
   }
   
   @Override
   public boolean isCurrent(String id)
   {
      return false;
   }
   
   @Override
   public void updateProgress(int timestamp)
   {
   }
   
   @Override
   public void showProgress(Job job)
   {
   }
   
   @Override
   public void setInitialJobs(List<Job> jobs)
   {
   }
   
   @Override
   public void addJob(Job job)
   {
   }
   
   @Override
   public void insertJob(Job job)
   {
   }
   
   @Override
   public void removeJob(Job job)
   {
   }
   
   @Override
   public void updateJob(Job job)
   {
   }
   
   @Override
   public void clear()
   {
   }
   
   @Override
   public void syncElapsedTime(int timestamp)
   {
   }
   
   @Override
   public Toolbar getToolbar()
   {
      return null;
   }
   
   @Override
   public JobOutputPanel getOutputPanel()
   {
      return null;
   }
   
   @Override
   public SlidingLayoutPanel getPanel()
   {
      return null;
   }
}
