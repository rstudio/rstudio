/*
 * NewDirectoryPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateOptions;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

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
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   private void initialize(DependencyManager dependencyManager)
   {
      dependencyManager_ = dependencyManager;
   }

   @Override
   protected void onAddWidgets()
   {
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      HorizontalPanel panel = new HorizontalPanel();
      panel.addStyleName(styles.wizardMainColumn());
      
      // top panel widgets
      onAddTopPanelWidgets(panel);
      
      // dir name
      VerticalPanel namePanel = new VerticalPanel();
      namePanel.addStyleName(styles.newProjectDirectoryName());
      txtProjectName_ = new TextBox();
      txtProjectName_.setWidth("100%");
      DomUtils.disableSpellcheck(txtProjectName_);
      Roles.getTextboxRole().setAriaRequiredProperty(txtProjectName_.getElement(), true);

      // create the dir name label
      dirNameLabel_ = new FormLabel(getDirNameLabel(), txtProjectName_);
      dirNameLabel_.addStyleName(styles.wizardTextEntryLabel());

      namePanel.add(dirNameLabel_);
      namePanel.add(txtProjectName_);
      panel.add(namePanel);
      addWidget(panel);
      
      onAddBodyWidgets();
      
      addSpacer();
      
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox(
            "Create project as subdirectory of:",
            ElementIds.TextBoxButtonId.PROJECT_PARENT,
            txtProjectName_);
      addWidget(newProjectParent_);
      
      // if git is available then add git init
      UserPrefs userState = RStudioGinjector.INSTANCE.getUserPrefs();
      SessionInfo sessionInfo = 
         RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      
      HorizontalPanel optionsPanel = null;
      if (getOptionsSideBySide())
         optionsPanel = new HorizontalPanel();
      
      chkGitInit_ = new CheckBox("Create a git repository");
      chkGitInit_.addStyleName(styles.wizardCheckbox());
      if (sessionInfo.isVcsAvailable(VCSConstants.GIT_ID))
      {  
         chkGitInit_.setValue(userState.newProjGitInit().getValue());
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
      
      // Initialize project with renv
      chkRenvInit_ = new CheckBox("Use renv with this project");
      chkRenvInit_.addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {
         if (event.getValue())
         {
            dependencyManager_.withRenv("Using renv", (Boolean success) -> {
               chkRenvInit_.setValue(success);
            });
         }
         
      });
      
      if (optionsPanel != null)
      {
         optionsPanel.add(chkRenvInit_);
      }
      else
      {
         addSpacer();
         addWidget(chkRenvInit_);
      }
      
      
      if (optionsPanel != null)
      {
         addSpacer();
         addWidget(optionsPanel);
      }
   }

   protected String getDirNameLabel()
   {
      return "Directory name:";
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
         String projFile = Projects.projFileFromDir(projDir);
         String newDefaultLocation = null;
         if (dir != defaultNewProjectLocation_.getPath())
            newDefaultLocation = dir;
         
         return new NewProjectResult(projFile, 
                                     chkGitInit_.getValue(), 
                                     chkRenvInit_.getValue(), 
                                     newDefaultLocation,
                                     null,
                                     getNewPackageOptions(),
                                     getNewShinyAppOptions(),
                                     getProjectTemplateOptions(),
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
   
   public String getProjectName()
   {
      return txtProjectName_.getText().trim();
   }
   
   protected FormLabel dirNameLabel_;
   protected TextBox txtProjectName_;
   protected CheckBox chkGitInit_;
   protected CheckBox chkRenvInit_;
   
   private DirectoryChooserTextBox newProjectParent_;
   
   // Injected ----
   private DependencyManager dependencyManager_;

}
