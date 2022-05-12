/*
 * NewDirectoryPage.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.NewShinyAppOptions;
import org.rstudio.studio.client.projects.model.ProjectTemplateOptions;
import org.rstudio.studio.client.quarto.model.QuartoNewProjectOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.files.model.DirectoryListing;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

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
      this(constants_.newProjectTitle(),
           constants_.newProjectSubTitle(),
           constants_.createNewProjectPageCaption(),
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
   private void initialize(Session session,
                           DependencyManager dependencyManager, 
                           FilesServerOperations server)
   {
      session_ = session;
      dependencyManager_ = dependencyManager;
      server_ = server;
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
      ElementIds.assignElementId(txtProjectName_,
         ElementIds.idWithPrefix(getTitle(), ElementIds.NEW_PROJECT_DIRECTORY));

      // create the dir name label
      dirNameLabel_ = new FormLabel(getDirNameLabel(), txtProjectName_);
      dirNameLabel_.addStyleName(styles.wizardTextEntryLabel());

      namePanel.add(dirNameLabel_);
      namePanel.add(txtProjectName_);
      panel.add(namePanel);
      addWidget(panel);
      
      onAddTopWidgets();
      
      addSpacer();
      
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox(
            constants_.newProjectParentLabel(),
            ElementIds.TextBoxButtonId.PROJECT_PARENT,
            txtProjectName_);
      addWidget(newProjectParent_);
      
      onAddMiddleWidgets();
      
      // if git is available then add git init
      UserPrefs userPrefs = RStudioGinjector.INSTANCE.getUserPrefs();
      SessionInfo sessionInfo = 
         RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      
      HorizontalPanel optionsPanel = null;
      if (getOptionsSideBySide())
         optionsPanel = new HorizontalPanel();
      
      chkGitInit_ = new CheckBox(constants_.createGitRepoLabel());
      chkGitInit_.addStyleName(styles.wizardCheckbox());
      ElementIds.assignElementId(chkGitInit_,
         ElementIds.idSafeString(getTitle()) + "_" + ElementIds.NEW_PROJECT_GIT_REPO);
      if (sessionInfo.isVcsAvailable(VCSConstants.GIT_ID))
      {  
         chkGitInit_.setValue(userPrefs.newProjGitInit().getValue());
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
      chkRenvInit_ = new CheckBox(constants_.chkRenvInitLabel());
      chkRenvInit_.setValue(userPrefs.newProjUseRenv().getValue());
      ElementIds.assignElementId(chkRenvInit_,
         ElementIds.idWithPrefix(getTitle(), ElementIds.NEW_PROJECT_RENV));
      chkRenvInit_.addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {
         if (event.getValue())
         {
            dependencyManager_.withRenv(constants_.chkRenvInitUserAction(), (Boolean success) -> {
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
      
    
      onAddBottomWidgets();
      
   }

   protected String getDirNameLabel()
   {
      return constants_.directoryNameLabel();
   }

   protected boolean getOptionsSideBySide()
   {
      return false;
   }
   
   
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
   }
   
   protected void onAddTopWidgets()
   {
   }
   
   protected void onAddMiddleWidgets()
   {
      
   }
   
   protected void onAddBottomWidgets()
   {
      
   }
   
   protected void setUseRenvVisible(boolean visible)
   {
      chkRenvInit_.setVisible(visible);
   }
   
   protected NewPackageOptions getNewPackageOptions()
   {
      return null;
   }
   
   protected NewShinyAppOptions getNewShinyAppOptions()
   {
      return null;
   }
   
   protected QuartoNewProjectOptions getNewQuartoProjectOptions()
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
      
      String path = input.getDefaultNewProjectLocation().getPath();
      if (StringUtil.isNullOrEmpty(path))
         path = session_.getSessionInfo().getDefaultProjectDir();
      
      newProjectParent_.setText(path);
   }


   @Override
   protected boolean validate(NewProjectResult input)
   {
      if (input == null)
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               constants_.errorCaption(),
               constants_.specifyProjectDirectoryName(),
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
                                     getNewQuartoProjectOptions(),
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
   
   @Override
   protected void validateAsync(final NewProjectResult input,
                                final OperationWithInput<Boolean> onValidated)
   {
      
      final FileSystemItem projFile = FileSystemItem.createFile(input.getProjectFile());
      final FileSystemItem projDir = projFile.getParentPath();
      server_.stat(projDir.getPath(), new ServerRequestCallback<FileSystemItem>()
      {
         @Override
         public void onResponseReceived(final FileSystemItem item)
         {
            // no file at this path -- safe for use
            if (!item.exists())
            {
               onValidated.execute(true);
               return;
            }
            
            // if it was a file, bail
            if (!item.isDirectory())
            {
               globalDisplay_.showMessage(
                     MessageDialog.WARNING,
                     constants_.creatingProjectError(),
                     constants_.fileAlreadyExistsMessage(item.getPath()));
               onValidated.execute(false);
               return;
            }
            
            // check if this directory is empty
            server_.listFiles(item, 
                  false, // monitor
                  false, // show hidden
                  new ServerRequestCallback<DirectoryListing>()
            {
               @Override
               public void onResponseReceived(DirectoryListing listing)
               {
                  boolean ok = true;
                  JsArray<FileSystemItem> children = listing.getFiles();
                  for (FileSystemItem child : JsUtil.asIterable(children))
                  {
                     boolean canIgnore =
                           child.getExtension() == ".Rproj" ||
                           child.getName().startsWith(".");
                     
                     if (canIgnore)
                        continue;
                     
                     ok = false;
                     break;
                  }
                  
                  if (!ok)
                  {
                     globalDisplay_.showMessage(
                          MessageDialog.WARNING,
                          constants_.creatingProjectError(),
                          constants_.directoryAlreadyExistsMessage(item.getPath()));
                  }
                  
                  onValidated.execute(ok);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  onValidated.execute(true);
               }
            });
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            onValidated.execute(true);
         }

      });
   }


   protected FormLabel dirNameLabel_;
   protected TextBox txtProjectName_;
   protected CheckBox chkGitInit_;
   protected CheckBox chkRenvInit_;
   
   private DirectoryChooserTextBox newProjectParent_;
   
   // Injected ----
   private Session session_;
   private DependencyManager dependencyManager_;
   private FilesServerOperations server_;
   
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
