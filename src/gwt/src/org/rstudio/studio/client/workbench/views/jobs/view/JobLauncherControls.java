/*
 * JobLauncherControls.java
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
         "R Script", "", ElementIds.TextBoxButtonId.JOB_SCRIPT, false, null, null);
      dir_ = new DirectoryChooserTextBox("Working Directory",
         ElementIds.TextBoxButtonId.JOB_WORKING_DIR,
         null);

      initWidget(uiBinder.createAndBindUi(this));
      
      exportEnv_.addItem("(Don't copy)", "");
      exportEnv_.addItem("To global environment", "R_GlobalEnv");
      exportEnv_.addItem("To results object in global environment", "local");
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
}
