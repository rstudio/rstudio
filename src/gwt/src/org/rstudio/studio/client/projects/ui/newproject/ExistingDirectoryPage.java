/*
 * ExistingDirectoryPage.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;



public class ExistingDirectoryPage extends NewProjectWizardPage
{
   public ExistingDirectoryPage()
   {
      super("Existing Directory", 
            "Associate a project with an existing working directory",
            "Create Project from Existing Directory",
            new ImageResource2x(NewProjectResources.INSTANCE.existingDirectoryIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.existingDirectoryIconLarge2x()));
      

   }

   @Override
   protected void onAddWidgets()
   {
   
      existingProjectDir_ = new DirectoryChooserTextBox(
            "Project working directory:", null);
      addWidget(existingProjectDir_);
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      defaultNewProjectLocation_ = input.getDefaultNewProjectLocation();
      existingProjectDir_.setText(input.getContext().getWorkingDirectory());
   }
   
   @Override
   protected NewProjectResult collectInput()
   {
      String dir = existingProjectDir_.getText();
      if (dir.length() > 0)
      {
         return new NewProjectResult(
                     projFileFromDir(dir), false, false, null, null, null, null, null);
      }
      else
      {
         return null;
      }
   }

   @Override
   protected boolean validate(NewProjectResult input)
   {
      if (input == null)
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               "Error", 
               "You must specify an existing working directory to " +
               "create the new project within.");
         
         return false;
      }
      else
      {
         return true;
      }
      
   }

   @Override
   public void focus()
   {
      existingProjectDir_.focusButton();
   }

   
   private DirectoryChooserTextBox existingProjectDir_;
}
