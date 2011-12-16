/*
 * GitPage.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class GitPage extends VersionControlPage
{
   public GitPage(boolean isGitAvailable)
   {
      super("Git", 
            "Clone a project from a Git repository",
            "Clone Git Repository",
            NewProjectResources.INSTANCE.gitIcon(),
            NewProjectResources.INSTANCE.gitIconLarge(),
            isGitAvailable);
   }


   @Override
   protected void onAddWidgets()
   { 
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();   
      
      VerticalPanel urlPanel = new VerticalPanel();
      urlPanel.addStyleName(styles.wizardTextEntry());
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
      
      
      Label dirNameLabel = new Label("Directory name:");
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      txtDirName_ = new TextBox();
      txtDirName_.addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (!event.getValue().equals(guessGitRepoDir()))
               suppressDirNameDetection_ = true;
         }
         
      });
      txtDirName_.addStyleName(styles.wizardTextEntry());
      addWidget(txtDirName_);
      
      addSpacer();
    
      existingRepoDestDir_ = new DirectoryChooserTextBox(
            "Create project as subdirectory of:", txtRepoUrl_);
      addWidget(existingRepoDestDir_);
   }
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      super.initialize(defaultNewProjectLocation);
      existingRepoDestDir_.setText(defaultNewProjectLocation.getPath());
   }

   @Override
   protected NewProjectResult collectInput()
   {
      if (txtDirName_.getText().trim().length() == 0)
         autoFillCheckoutDir(); 
     
      String url = txtRepoUrl_.getText().trim();
      String checkoutDir = txtDirName_.getText().trim();
      String dir = existingRepoDestDir_.getText().trim();
      if (url.length() > 0 && checkoutDir.length() > 0 && dir.length() > 0)
      {
         String projFile = projFileFromDir(
               FileSystemItem.createDir(dir).completePath(checkoutDir));
         
         VcsCloneOptions options = VcsCloneOptions.create(VCSConstants.GIT_ID, 
                                                          url, 
                                                          checkoutDir, 
                                                          dir);
         
         return new NewProjectResult(projFile, false, dir, options);
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
               "You must specify a git repository URL and " +
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
      txtDirName_.setText(guessGitRepoDir()); 
   }
   
   private String guessGitRepoDir()
   {
      return guessGitRepoDir(txtRepoUrl_.getText().trim());
   }
   
   private static String guessGitRepoDir(String url)
   {
      /*
       * Strip trailing spaces, slashes and /.git
       */
      while (url.endsWith("/") || url.endsWith(" ") || url.endsWith("\t"))
         url = url.substring(0, url.length() - 1);
      if (url.endsWith("/.git"))
      {
         url = url.substring(0, url.length() - 5);
         while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);
      }

      /*
       * Find last component, but be prepared that repo could have
       * the form  "remote.example.com:foo.git", i.e. no slash
       * in the directory part.
       */
      url = url.replaceFirst(".*[:/]", ""); // greedy

      /*
       * Strip .{bundle,git}.
       */
      url = url.replaceAll(".(bundle|git)$", "");
      url = url.replaceAll("[\u0000-\u0020]+", " ");
      url = url.trim();
      return url;
   }
  
   private TextBox txtRepoUrl_;
   private TextBox txtDirName_;
   private DirectoryChooserTextBox existingRepoDestDir_;
   private boolean suppressDirNameDetection_ = false;
}
