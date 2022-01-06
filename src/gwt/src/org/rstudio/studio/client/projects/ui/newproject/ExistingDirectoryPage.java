/*
 * ExistingDirectoryPage.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;



public class ExistingDirectoryPage extends NewProjectWizardPage
{
   public ExistingDirectoryPage()
   {
      super(constants_.existingDirectoryTitle(),
            constants_.existingDirectorySubTitle(),
            constants_.existingDirectoryPageCaption(),
            new ImageResource2x(NewProjectResources.INSTANCE.existingDirectoryIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.existingDirectoryIconLarge2x()));
      

   }

   @Override
   protected void onAddWidgets()
   {
   
      existingProjectDir_ = new DirectoryChooserTextBox(
            constants_.projectWorkingDirectoryTitle(), ElementIds.TextBoxButtonId.EXISTING_PROJECT_DIR, null);
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
                     Projects.projFileFromDir(dir), false, false, null, null, null, null, null, null, null);
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
               constants_.errorCaption(),
               constants_.validateMessage());
         
         return false;
      }
      else if (StringUtil.equals(
            FileSystemItem.createFile(input.getProjectFile()).getParentPathString(),
            "~"))
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               constants_.errorCaption(),
               constants_.homeDirectoryErrorMessage());
         
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

   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
   private DirectoryChooserTextBox existingProjectDir_;
}
