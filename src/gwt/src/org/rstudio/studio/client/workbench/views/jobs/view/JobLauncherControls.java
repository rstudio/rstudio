/*
 * JobLauncherControls.java
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

import org.rstudio.core.client.widget.FileChooserTextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
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
      file_ = new FileChooserTextBox("R Script", null);

      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setScriptPath(String path)
   {
      file_.setText(path);
   }
   
   public String scriptPath()
   {
      return file_.getText();
   }

   @UiField(provided=true) FileChooserTextBox file_;
}
