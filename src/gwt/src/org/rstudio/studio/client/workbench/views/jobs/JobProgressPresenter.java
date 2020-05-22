/*
 * JobProgressPresenter.java
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
package org.rstudio.studio.client.workbench.views.jobs;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.jobs.events.JobElapsedTickEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;
import org.rstudio.studio.client.workbench.views.jobs.model.LocalJobProgress;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class JobProgressPresenter implements JobElapsedTickEvent.Handler,
                                             IsWidget
{
   public interface Display extends IsWidget
   {
      void updateElapsed(int timestamp);
      void showProgress(LocalJobProgress progress);
      void showJob(Job job);
   }
   
   @Inject
   public JobProgressPresenter(Display display,
                               EventBus events)
   {
      display_ = display;
      events.addHandler(JobElapsedTickEvent.TYPE, this);
   }
   
   @Override
   public void onJobElapsedTick(JobElapsedTickEvent event)
   {
      display_.updateElapsed(event.timestamp());
   }
   
   @Override
   public Widget asWidget()
   {
      return display_.asWidget();
   }

   public void showProgress(LocalJobProgress progress)
   {
      display_.showProgress(progress);
   }
   
   public void showJob(Job job)
   {
      display_.showJob(job);
   }
   
   private final Display display_;
}
