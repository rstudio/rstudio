/*
 * JobQuitDialog.java
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

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.jobs.model.Job;

import com.google.gwt.user.client.ui.Widget;

public class JobQuitDialog extends ModalDialog<Boolean>
{
   public JobQuitDialog(List<Job> runningJobs, 
                        OperationWithInput<Boolean> onConfirmed,
                        Operation cancelOperation)
   {
      super("Terminate Running Jobs", Roles.getAlertdialogRole(), onConfirmed, cancelOperation);
      running_ = runningJobs;
      setOkButtonCaption("Terminate Jobs");
   }

   @Override
   protected Widget createMainWidget()
   {
      return new JobQuitControls(running_);
   }
   
   @Override
   protected Boolean collectInput()
   {
      return true;
   }

   private List<Job> running_;
}
