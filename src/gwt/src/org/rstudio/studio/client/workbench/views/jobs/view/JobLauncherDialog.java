/*
 * JobLauncherDialog.java
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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobLaunchSpec;

import com.google.gwt.user.client.ui.Widget;

public class JobLauncherDialog extends ModalDialog<JobLaunchSpec>
{
   public JobLauncherDialog(String caption, 
                            String scriptPath,
                            OperationWithInput<JobLaunchSpec> operation)
   {
      super(caption, operation);

      controls_ = new JobLauncherControls();
      if (scriptPath != null)
         controls_.setScriptPath(scriptPath);
   }

   @Override
   protected JobLaunchSpec collectInput()
   {
      return JobLaunchSpec.create(controls_.scriptPath());
   }

   @Override
   protected Widget createMainWidget()
   {
      return controls_;
   }
   
   private JobLauncherControls controls_;
}
