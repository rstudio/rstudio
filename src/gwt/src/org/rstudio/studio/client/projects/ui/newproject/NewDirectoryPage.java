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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateOptions;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.dom.client.Style.Unit;
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
      this("New Project", 
           "Create a new project in an empty directory",
           "Create New Project",
           new ImageResource2x(NewProjectResources.INSTANCE.newProjectDirectoryIcon2x()),
           new ImageResource2x(NewProjectResources.INSTANCE.newProjectDirectoryIconLarge2x()));
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
      txtProjectName_.getElement().setAttribute("spellcheck", "false");
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
      UIPrefs uiPrefs = RStudioGinjector.INSTANCE.getUIPrefs();
      SessionInfo sessionInfo = 
         RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      
      HorizontalPanel optionsPanel = null;
      if (getOptionsSideBySide())
         optionsPanel = new HorizontalPanel();
      
      chkGitInit_ = new CheckBox("Create a git repository");
      chkGitInit_.addStyleName(styles.wizardCheckbox());
      if (sessionInfo.isVcsAvailable(VCSConstants.GIT_ID))
      {  
         chkGitInit_.setValue(uiPrefs.newProjGitInit().getValue());
         chkGitInit_.getElement().getStyle().setMarginRight(7, Unit.PX);
         if (optionsPanel != null)
         {
            optionsPanel.add(chkGitInit_);
         }
         else
         {
            addSpacer();
            addWidget(chkGitInit_);
         }
      }
      
      // Initialize project with packrat
      chkPackratInit_ = new CheckBox("Use packrat with this project");
      chkPackratInit_.setValue(uiPrefs.newProjUsePackrat().getValue());
      if (!sessionInfo.getPackratAvailable())
      {
         chkPackratInit_.setValue(false);
         chkPackratInit_.setVisible(false);
      }
      
      if (optionsPanel != null)
      {
         optionsPanel.add(chkPackratInit_);
      }
      else
      {
         addSpacer();
         addWidget(chkPackratInit_);
      }
      
      
      if (optionsPanel != null)
      {
         addSpacer();
         addWidget(optionsPanel);
      }
   }
   
   protected boolean getOptionsSideBySide()
   {
      return false;
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
   
   protected ProjectTemplateOptions getProjectTemplateOptions()
   {
      return null;
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
          
      newProjectParent_.setText(input.getDefaultNewProjectLocation().getPath());
      
      if (!input.getContext().isPackratAvailable())
      {
         chkPackratInit_.setValue(false);
         chkPackratInit_.setVisible(false);
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
                                     chkPackratInit_.getValue(), 
                                     newDefaultLocation,
                                     null,
                                     getNewPackageOptions(),
                                     getNewShinyAppOptions(),
                                     getProjectTemplateOptions());
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
   
   public String getProjectName()
   {
      return txtProjectName_.getText().trim();
   }
   
   protected Label dirNameLabel_;
   protected TextBox txtProjectName_;
   protected CheckBox chkGitInit_;
   protected CheckBox chkPackratInit_;
   
   private DirectoryChooserTextBox newProjectParent_;

}
