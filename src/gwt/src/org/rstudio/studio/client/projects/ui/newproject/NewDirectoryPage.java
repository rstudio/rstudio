/*
 * NewDirectoryPage.java
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

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class NewDirectoryPage extends NewProjectWizardPage
{
   public NewDirectoryPage()
   {
      this("Empty Project", 
           "Create a new project in an empty directory",
           "Create New Project",
           NewProjectResources.INSTANCE.newProjectDirectoryIcon(),
           NewProjectResources.INSTANCE.newProjectDirectoryIconLarge());
   }
   
   public NewDirectoryPage(String title, 
                           String subTitle, 
                           String pageCaption, 
                           ImageResource image,
                           ImageResource largeImage)
   {
      super(title, subTitle, pageCaption, image, largeImage);
   }


   @Override
   protected void onAddWidgets()
   {
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      HorizontalPanel panel = new HorizontalPanel();
      panel.addStyleName(styles.wizardMainColumn());
      
      // create the dir name label
      dirNameLabel_ = new Label("Directory name:");
      dirNameLabel_.addStyleName(styles.wizardTextEntryLabel());
      
      // top panel widgets
      onAddTopPanelWidgets(panel);
      
      // dir name
      VerticalPanel namePanel = new VerticalPanel();
      namePanel.addStyleName(styles.newProjectDirectoryName());
      namePanel.add(dirNameLabel_);
      txtProjectName_ = new TextBox();
      txtProjectName_.setWidth("100%");
      namePanel.add(txtProjectName_);
      panel.add(namePanel);
      addWidget(panel);
      
      onAddBodyWidgets();
      
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
         UIPrefs uiPrefs = RStudioGinjector.INSTANCE.getUIPrefs();
         chkGitInit_.setValue(uiPrefs.newProjGitInit().getValue());
         
         addWidget(chkGitInit_);
      }
   }
   
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
   }
   
   protected void onAddBodyWidgets()
   {
   }
   
   protected NewPackageOptions getNewPackageOptions()
   {
      return null;
   }
   
   protected NewShinyAppOptions getNewShinyAppOptions()
   {
      return null;
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
          
      newProjectParent_.setText(input.getDefaultNewProjectLocation().getPath());
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
                                     null,
                                     getNewPackageOptions(),
                                     getNewShinyAppOptions());
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
   
   protected Label dirNameLabel_;
   protected TextBox txtProjectName_;
   private CheckBox chkGitInit_;
   
   private DirectoryChooserTextBox newProjectParent_;

}
