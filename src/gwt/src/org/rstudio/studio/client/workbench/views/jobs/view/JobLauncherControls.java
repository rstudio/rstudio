/*
 * JobLauncherControls.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.jobs.view;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FileChooserTextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.views.jobs.JobsConstants;

public class JobLauncherControls extends Composite
{
   private static JobLauncherControlsUiBinder uiBinder = GWT
         .create(JobLauncherControlsUiBinder.class);

   interface JobLauncherControlsUiBinder extends UiBinder<Widget, JobLauncherControls>
   {
   }

   public JobLauncherControls()
   {
      file_ = new FileChooserTextBox(
         constants_.rScriptLabel(), "", ElementIds.TextBoxButtonId.JOB_SCRIPT, false, null, null);
      dir_ = new DirectoryChooserTextBox(constants_.workingDirectoryCaption(),
         ElementIds.TextBoxButtonId.JOB_WORKING_DIR,
         null);

      initWidget(uiBinder.createAndBindUi(this));
      
      exportEnv_.addItem(constants_.dontCopyText(), "");
      exportEnv_.addItem(constants_.toGlobalEnvironmentText(), "R_GlobalEnv");
      exportEnv_.addItem(constants_.toResultObjectText(), "local");
   }
   
   public void setScriptPath(FileSystemItem path)
   {
      file_.setText(path.getPath());
   }
   
   public void setWorkingDir(FileSystemItem dir)
   {
      dir_.setText(dir.getPath());
   }
   
   public void hideScript()
   {
      file_.setVisible(false);
   }
   
   public String scriptPath()
   {
      return file_.getText();
   }
   
   public String workingDir()
   {
      return dir_.getText();
   }
   
   public boolean importEnv()
   {
      return importEnv_.getValue();
   }
   
   public String exportEnv()
   {
      String env = exportEnv_.getSelectedValue();
      if (env == "local")
      {
         env = FileSystemItem.createFile(file_.getText()).getStem() + "_results";
      }
      return env;
   }

   @UiField(provided=true) FileChooserTextBox file_;
   @UiField(provided=true) DirectoryChooserTextBox dir_;
   @UiField CheckBox importEnv_;
   @UiField ListBox exportEnv_;
   private static final JobsConstants constants_ = GWT.create(JobsConstants.class);
}
