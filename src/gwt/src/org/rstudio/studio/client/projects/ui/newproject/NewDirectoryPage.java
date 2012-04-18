/*
 * NewDirectoryPage.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class NewDirectoryPage extends NewProjectWizardPage
{
   public NewDirectoryPage()
   {
      super("New Directory", 
            "Start a project in a brand new working directory",
            "Create New Project",
            NewProjectResources.INSTANCE.newProjectDirectoryIcon(),
            NewProjectResources.INSTANCE.newProjectDirectoryIconLarge());
      
  
   }

   @Override
   protected void onAddWidgets()
   {
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      // dir name
      Label dirNameLabel = new Label("New project directory name:");
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      txtProjectName_ = new TextBox();
      txtProjectName_.addStyleName(styles.wizardTextEntry());
      addWidget(txtProjectName_);
      
      addSpacer();
      
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox(
            "Create project as subdirectory of:", txtProjectName_);
      addWidget(newProjectParent_);
      
      // if git is available then add git init
      SessionInfo sessionInfo = 
         RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      chkGitInit_ = new CheckBox("Create a git repository for this project");
      chkGitInit_.addStyleName(styles.wizardCheckbox());
      if (sessionInfo.isVcsAvailable(VCSConstants.GIT_ID))
      {
         for (int i=0; i<2; i++)
            addSpacer();
         
         UIPrefs uiPrefs = RStudioGinjector.INSTANCE.getUIPrefs();
         chkGitInit_.setValue(uiPrefs.newProjGitInit().getValue());
         
         addWidget(chkGitInit_);
      }
   }
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      super.initialize(defaultNewProjectLocation);
      newProjectParent_.setText(defaultNewProjectLocation.getPath());
   }


   @Override
   protected boolean validate(NewProjectResult input)
   {
      if (input == null)
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               "Error", 
               "You must specify a name for the new project directory.",
               txtProjectName_);
         
         return false;
      }
      else
      {
         return true;
      }
   }
   
   @Override
   protected NewProjectResult collectInput()
   {
      String name = txtProjectName_.getText().trim();
      String dir = newProjectParent_.getText();
      if (name.length() > 0 && dir.length() > 0)
      {
         String projDir = FileSystemItem.createDir(dir).completePath(name);
         String projFile = projFileFromDir(projDir);
         String newDefaultLocation = null;
         if (!dir.equals(defaultNewProjectLocation_))
            newDefaultLocation = dir;
         return new NewProjectResult(projFile, 
                                     chkGitInit_.getValue(), 
                                     newDefaultLocation, 
                                     null);
      }
      else
      {
         return null;
      }
   }


   @Override
   public void focus()
   {
      txtProjectName_.setFocus(true);
   }
   
   private TextBox txtProjectName_;
   private CheckBox chkGitInit_;
   
   private DirectoryChooserTextBox newProjectParent_;

}
