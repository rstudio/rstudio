/*
 * SvnPage.java
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

public class SvnPage extends VersionControlPage
{
   public SvnPage(boolean isSvnAvailable)
   {
      super("Subversion", 
            "Checkout a project from a Subversion repository",
            "Checkout Subversion Repository",
            NewProjectResources.INSTANCE.svnIcon(),
            NewProjectResources.INSTANCE.svnIconLarge(),
            isSvnAvailable);
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
      
      
      Label dirNameLabel = new Label("Checkout directory:");
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      txtDirName_ = new TextBox();
      txtDirName_.addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (!event.getValue().equals(guessSvnRepoDir()))
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
         
         VcsCloneOptions options = VcsCloneOptions.create(VCSConstants.SVN_ID, 
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
               "You must specify an SVN repository URL and " +
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
      String guess = guessSvnRepoDir();
      if (guess != null)
         txtDirName_.setText(guess); 
   }
   
   private String guessSvnRepoDir()
   {
      return guessSvnRepoDir(txtRepoUrl_.getText().trim());
   }
   
   private static String guessSvnRepoDir(String url)
   {
      String guess = FileSystemItem.createFile(url).getStem();
      if (guess.length() > 1)
         return guess;
      else
         return null;
   }
  
   private TextBox txtRepoUrl_;
   private TextBox txtDirName_;
   private DirectoryChooserTextBox existingRepoDestDir_;
   private boolean suppressDirNameDetection_ = false;
}
