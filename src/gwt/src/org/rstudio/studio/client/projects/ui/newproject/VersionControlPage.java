/*
 * VersionControlPage.java
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
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.common.vcs.VcsHelpLink;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class VersionControlPage extends NewProjectWizardPage
{

   public VersionControlPage(String title, 
                             String subTitle, 
                             String pageCaption, 
                             ImageResource image,
                             ImageResource largeImage)
   {
      super(title, subTitle, pageCaption, image, largeImage);  
   }
   
   @Override
   protected boolean acceptNavigation()
   {
      SessionInfo sessionInfo = 
                     RStudioGinjector.INSTANCE.getSession().getSessionInfo();
      if (!sessionInfo.isVcsAvailable(getVcsId()))
      {         
         NewProjectResources.Styles styles = 
                                 NewProjectResources.INSTANCE.styles();   
         
         VerticalPanel verticalPanel = new VerticalPanel();
         verticalPanel.addStyleName(styles.vcsNotInstalledWidget());
         
         if (Desktop.isDesktop())
         {
            HTML msg = new HTML(
                    constants_.acceptNavigationHTML(getTitle(),(BrowseCap.isMacintosh() ? constants_.preferencesLabel() : constants_.optionsLabel()))
            );
            msg.setWidth("100%");
            
            verticalPanel.add(msg);
            
            HelpLink vcsHelpLink = new VcsHelpLink();
            vcsHelpLink.setCaption(constants_.vcsHelpLink(getTitle()));
            vcsHelpLink.addStyleName(styles.vcsHelpLink());
            verticalPanel.add(vcsHelpLink);
         }
         else
         {
            HTML msg = new HTML(
                    constants_.installtionNotDetectedHTML(getTitle())
            );
               msg.setWidth("100%");
               
               verticalPanel.add(msg);
         }
         
         MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
                                               constants_.titleNotFound(getTitle()),
                                               verticalPanel);
         
         
         dlg.addButton(constants_.okLabel(), ElementIds.DIALOG_OK_BUTTON, (Operation)null, true, false);
         dlg.showModal();
         
         return false;
      }
      else
      {
         return true;
      }
   }

   @Override
   protected void onAddWidgets()
   { 
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      VerticalPanel urlPanel = new VerticalPanel();
      urlPanel.addStyleName(styles.wizardMainColumn());

      txtRepoUrl_ = new TextBox();
      txtRepoUrl_.addDomHandler(new KeyDownHandler() {
         public void onKeyDown(KeyDownEvent event)
         {
            handleAutoFillCheckoutDir();
         }
      }, KeyDownEvent.getType());

      txtRepoUrl_.setWidth("100%");

      FormLabel urlLabel = new FormLabel(constants_.repoURLLabel(), txtRepoUrl_);
      urlLabel.addStyleName(styles.wizardTextEntryLabel());
      urlPanel.add(urlLabel);
      urlPanel.add(txtRepoUrl_);

      addWidget(urlPanel);
      
      addSpacer();
      
      txtUsername_ = new TextBox();
      txtUsername_.setWidth("100%");
      
      if (includeCredentials())
      {  
         VerticalPanel usernamePanel = new VerticalPanel();
         usernamePanel.addStyleName(styles.wizardMainColumn());
         FormLabel usernameLabel = new FormLabel(constants_.usernameLabel(),
                                                 txtUsername_);
         usernameLabel.addStyleName(styles.wizardTextEntryLabel());
         usernamePanel.add(usernameLabel);
         usernamePanel.add(txtUsername_);
         addWidget(usernamePanel);
         
         addSpacer();
      }
      

      txtDirName_ = new TextBox();
      txtDirName_.addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (event.getValue() != guessRepoDir())
               suppressDirNameDetection_ = true;
         }
         
      });
      txtDirName_.addStyleName(styles.wizardMainColumn());

      FormLabel dirNameLabel = new FormLabel(constants_.projDirNameLabel(), txtDirName_);
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      addWidget(txtDirName_);
      
      addSpacer();
    
      existingRepoDestDir_ = new DirectoryChooserTextBox(
            constants_.existingRepoDestDirLabel(),
            ElementIds.TextBoxButtonId.PROJECT_REPO_DIR,
            txtRepoUrl_);
      addWidget(existingRepoDestDir_);
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      String path = input.getDefaultNewProjectLocation().getPath();
      if (StringUtil.isNullOrEmpty(path))
         path = this.getSessionInfo().getDefaultProjectDir();
      existingRepoDestDir_.setText(path);
   }

   @Override
   protected NewProjectResult collectInput()
   {
      if (txtDirName_.getText().trim().length() == 0)
         autoFillCheckoutDir(); 
     
      String url = txtRepoUrl_.getText().trim();
      String username = txtUsername_.getText().trim();
      String checkoutDir = txtDirName_.getText().trim();
      String dir = existingRepoDestDir_.getText().trim();
      if (url.length() > 0 && checkoutDir.length() > 0 && dir.length() > 0)
      {
         String projFile = Projects.projFileFromDir(
               FileSystemItem.createDir(dir).completePath(checkoutDir));
         
         VcsCloneOptions options = VcsCloneOptions.create(getVcsId(), 
                                                          url, 
                                                          username,
                                                          checkoutDir, 
                                                          dir);
         
         return new NewProjectResult(projFile, false, false, dir, options, null, null, null, null, null);
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
               constants_.specifyRepoURLErrorMessage(),
               txtRepoUrl_);
         
         
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
      txtRepoUrl_.setFocus(true);
      
   }
   
   private void handleAutoFillCheckoutDir()
   {
      if (suppressDirNameDetection_)
         return;
      
      // delay so the text has a chance to populate
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         @Override
         public void execute()
         {
            autoFillCheckoutDir();
         }
      }); 
   }
   
   private void autoFillCheckoutDir()
   {
      txtDirName_.setText(guessRepoDir()); 
   }
   
   private String guessRepoDir()
   {
      return guessRepoDir(txtRepoUrl_.getText().trim());
   }
   
   protected abstract String getVcsId();
   
   protected boolean includeCredentials()
   {
      return false;
   }
   
   protected abstract String guessRepoDir(String url);

   private TextBox txtRepoUrl_;
   private TextBox txtUsername_;
   private TextBox txtDirName_;
   private DirectoryChooserTextBox existingRepoDestDir_;
   private boolean suppressDirNameDetection_ = false;
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
