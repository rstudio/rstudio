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
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class JobsPane extends WorkbenchPane 
                      implements JobsPresenter.Display
{
   @Inject
   public JobsPane()
   {
      super("Jobs");
   }

   @Override
   protected Widget createMainWidget()
   {
      return new Label("Jobs");
   }

   @Override
   public void updateJob(int type, Job job)
   {
      // TODO Auto-generated method stub
   }
}
