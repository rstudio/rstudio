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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class NewDirectoryPage extends NewProjectWizardPage
{
   public NewDirectoryPage()
   {
      super("New Project", 
            "Start a project in a new directory",
            "Create New Project",
            NewProjectResources.INSTANCE.newProjectDirectoryIcon(),
            NewProjectResources.INSTANCE.newProjectDirectoryIconLarge());
   }

   @Override
   protected void onAddWidgets()
   {
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      HorizontalPanel panel = new HorizontalPanel();
      panel.addStyleName(styles.wizardMainColumn());
      
      // project type
      String[] labels = {"(Default)", "Package"};
      String[] values = {"none", "package"};
      listProjectType_ = new SelectWidget("Type:",
                                          labels,
                                          values,
                                          false);
      listProjectType_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            txtProjectName_.setFocus(true);
            boolean isPackage = !listProjectType_.getValue().equals("none");
            listCodeFiles_.setVisible(isPackage);
            if (isPackage)
               dirNameLabel_.setText("Package name:");
            else
               dirNameLabel_.setText("Directory name:");
         }
      });
      panel.add(listProjectType_);
     
      
      // dir name
      VerticalPanel namePanel = new VerticalPanel();
      namePanel.addStyleName(styles.newProjectDirectoryName());
      dirNameLabel_ = new Label("Directory name:");
      dirNameLabel_.addStyleName(styles.wizardTextEntryLabel());
      namePanel.add(dirNameLabel_);
      txtProjectName_ = new TextBox();
      txtProjectName_.setWidth("100%");
      namePanel.add(txtProjectName_);
      panel.add(namePanel);
      addWidget(panel);
      
      // code files panel
      listCodeFiles_ = new CodeFilesList();
      listCodeFiles_.setVisible(false);
      addWidget(listCodeFiles_);
      
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
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      
      if (input.getContext().isRcppAvailable())
         listProjectType_.addChoice("Package w/ Rcpp", "package-rcpp");
      
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
         
         NewPackageOptions newPackageOptions = null;
         if (!listProjectType_.getValue().equals("none"))
         {
            newPackageOptions = NewPackageOptions.create(
                listProjectType_.getValue().equals("package-rcpp"),  
                JsUtil.toJsArrayString(listCodeFiles_.getCodeFiles()));
         }
         
         
         return new NewProjectResult(projFile, 
                                     chkGitInit_.getValue(), 
                                     newDefaultLocation, 
                                     null,
                                     newPackageOptions);
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
      
      // workaround qt crash on mac desktop
      if (BrowseCap.isMacintoshDesktop())
      {
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(), 
                                  listProjectType_.getListBox());
      }
   }
   
   private Label dirNameLabel_;
   private SelectWidget listProjectType_;
   private TextBox txtProjectName_;
   private CodeFilesList listCodeFiles_;
   private CheckBox chkGitInit_;
   
   private DirectoryChooserTextBox newProjectParent_;

}
