/*
 * VersionControlPage.java
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.common.vcs.VcsHelpLink;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
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
               "<p>" + getTitle() + " was not detected " +
               "on the system path.</p>" +
               "<p>To create projects from " + getTitle() + " " + 
               "repositories you should install " + getTitle() + " " +
               "and then restart RStudio.</p>" +
               "<p>Note that if " + getTitle() + " is installed " +
               "and not on the path, then you can specify its location using " +
               "the " + (BrowseCap.isMacintosh() ? "Preferences" : "Options") +
               " dialog.</p>");
            msg.setWidth("100%");
            
            verticalPanel.add(msg);
            
            HelpLink vcsHelpLink = new VcsHelpLink();
            vcsHelpLink.setCaption("Using " + getTitle() + " with RStudio");
            vcsHelpLink.addStyleName(styles.vcsHelpLink());
            verticalPanel.add(vcsHelpLink);
         }
         else
         {
            HTML msg = new HTML(
                  "<p>An installation of " + getTitle() + " was not detected " +
                  "on this system.</p>" +
                  "<p>To create projects from " + getTitle() + " " + 
                  "repositories you should request that your server " +
                  "administrator install the " + getTitle() + " package.</p>");
               msg.setWidth("100%");
               
               verticalPanel.add(msg);
         }
         
         MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
                                               getTitle() + " Not Found",
                                               verticalPanel);
         
         
         dlg.addButton("OK", (Operation)null, true, false);
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
      Label urlLabel = new Label("Repository URL:");
      urlLabel.addStyleName(styles.wizardTextEntryLabel());
      urlPanel.add(urlLabel);
      txtRepoUrl_ = new TextBox();
      txtRepoUrl_.addDomHandler(new KeyDownHandler() {
         public void onKeyDown(KeyDownEvent event)
         {
            handleAutoFillCheckoutDir();
         }
      }, KeyDownEvent.getType());
        
      txtRepoUrl_.setWidth("100%");
      urlPanel.add(txtRepoUrl_);
     
      addWidget(urlPanel);
      
      addSpacer();
      
      txtUsername_ = new TextBox();
      txtUsername_.setWidth("100%");
      
      if (includeCredentials())
      {  
         VerticalPanel usernamePanel = new VerticalPanel();
         usernamePanel.addStyleName(styles.wizardMainColumn());
         Label usernameLabel = new Label("Username (if required for this repository URL):");
         usernameLabel.addStyleName(styles.wizardTextEntryLabel());
         usernamePanel.add(usernameLabel);
         usernamePanel.add(txtUsername_);
         addWidget(usernamePanel);
         
         addSpacer();
      }
      
      Label dirNameLabel = new Label("Project directory name:");
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      txtDirName_ = new TextBox();
      txtDirName_.addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (!event.getValue().equals(guessRepoDir()))
               suppressDirNameDetection_ = true;
         }
         
      });
      txtDirName_.addStyleName(styles.wizardMainColumn());
      addWidget(txtDirName_);
      
      addSpacer();
    
      existingRepoDestDir_ = new DirectoryChooserTextBox(
            "Create project as subdirectory of:", txtRepoUrl_);
      addWidget(existingRepoDestDir_);
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      existingRepoDestDir_.setText(
                           input.getDefaultNewProjectLocation().getPath());
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
         String projFile = projFileFromDir(
               FileSystemItem.createDir(dir).completePath(checkoutDir));
         
         VcsCloneOptions options = VcsCloneOptions.create(getVcsId(), 
                                                          url, 
                                                          username,
                                                          checkoutDir, 
                                                          dir);
         
         return new NewProjectResult(projFile, false, false, dir, options, null, null, null);
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
               "You must specify a repository URL and " +
               "directory to create the new project within.",
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
}
