/*
 * JobLauncherDialog.java
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

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobLaunchSpec;

import com.google.gwt.user.client.ui.Widget;

public class JobLauncherDialog extends ModalDialog<JobLaunchSpec>
{
   public enum JobSource
   {
      Script,
      Selection
   }

   public JobLauncherDialog(String caption,
                            JobSource source,
                            FileSystemItem scriptPath,
                            OperationWithInput<JobLaunchSpec> operation)
   {
      this(caption, source, scriptPath, scriptPath.getParentPath(), null, operation);
   }
   
   public JobLauncherDialog(String caption,
                            JobSource source,
                            FileSystemItem scriptPath,
                            FileSystemItem workingDir,
                            String code,
                            OperationWithInput<JobLaunchSpec> operation)
   {
      super(caption, Roles.getDialogRole(), operation);

      controls_ = new JobLauncherControls();

      if (scriptPath != null)
         controls_.setScriptPath(scriptPath);

      if (workingDir != null)
         controls_.setWorkingDir(workingDir);

      if (code != null)
         controls_.hideScript();
      
      code_ = code;
      source_ = source;

      setOkButtonCaption("Start");
   }

   @Override
   protected JobLaunchSpec collectInput()
   {
      // Compute a reasonable name for the job based on the selected script (if any)
      String jobName;
      if (source_ == JobSource.Selection)
      {
         jobName = computeSelectionJobName(controls_.scriptPath());
      }
      else 
      {
         jobName = FileSystemItem.getNameFromPath(controls_.scriptPath());
      }

      if (code_ == null)
      {
         return JobLaunchSpec.create(jobName,
               controls_.scriptPath(), 
               "unknown", // encoding unknown (will try to look it up later)
               controls_.workingDir(),
               controls_.importEnv(),
               controls_.exportEnv());
      }
      else
      {
         return JobLaunchSpec.create(jobName,
               code_, 
               controls_.workingDir(), 
               controls_.importEnv(), 
               controls_.exportEnv());
      }
   }
   
   @Override
   protected boolean validate(JobLaunchSpec spec)
   {
      // we need either a path to a script to run or a code snippet
      return !StringUtil.isNullOrEmpty(spec.path()) || 
             !StringUtil.isNullOrEmpty(code_);
   }

   @Override
   protected Widget createMainWidget()
   {
      return controls_;
   }
   
   public static String computeSelectionJobName(String path)
   {
      if (StringUtil.isNullOrEmpty(path))
      {
         return "Current selection";
      }
      else
      {
         return FileSystemItem.getNameFromPath(path) + " selection";
      }
   }
   
   private final String code_;
   private final JobSource source_;
   private JobLauncherControls controls_;
}
