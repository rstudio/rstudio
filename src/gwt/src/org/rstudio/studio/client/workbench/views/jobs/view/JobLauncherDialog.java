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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.jobs.model.JobLaunchSpec;

import com.google.gwt.user.client.ui.Widget;

public class JobLauncherDialog extends ModalDialog<JobLaunchSpec>
{
   public JobLauncherDialog(String caption,
                            String jobName,
                            FileSystemItem scriptPath,
                            OperationWithInput<JobLaunchSpec> operation)
   {
      this(caption, jobName, scriptPath, scriptPath.getParentPath(), null, operation);
   }
   
   public JobLauncherDialog(String caption,
                            String jobName,
                            FileSystemItem scriptPath,
                            FileSystemItem workingDir,
                            String code,
                            OperationWithInput<JobLaunchSpec> operation)
   {
      super(caption, operation);

      controls_ = new JobLauncherControls();

      if (scriptPath != null)
         controls_.setScriptPath(scriptPath);

      if (workingDir != null)
         controls_.setWorkingDir(workingDir);

      if (code != null)
         controls_.hideScript();
      
      code_ = code;
      jobName_ = jobName;

      setOkButtonCaption("Start");
   }

   @Override
   protected JobLaunchSpec collectInput()
   {
      if (code_ == null)
      {
         return JobLaunchSpec.create(jobName_,
               controls_.scriptPath(), 
               "unknown", // encoding unknown (will try to look it up later)
               controls_.workingDir(),
               controls_.importEnv(),
               controls_.exportEnv());
      }
      else
      {
         return JobLaunchSpec.create(jobName_,
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
   
   private final String code_;
   private final String jobName_;
   private JobLauncherControls controls_;
}
