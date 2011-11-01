/*
 * VersionControlPage.java
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
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.VcsCloneOptions;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class VersionControlPage extends NewProjectWizardPage
{
   public VersionControlPage()
   {
      super("Version Control", 
            "Checkout a project from a version control repository",
            "Create Project from Version Control",
            NewProjectResources.INSTANCE.projectFromRepositoryIcon(),
            NewProjectResources.INSTANCE.projectFromRepositoryIconLarge());
      
      
     
   }


   @Override
   protected void onAddWidgets()
   { 
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      Label dirNameLabel = new Label("Git Repository URL/Location:");
      dirNameLabel.addStyleName(styles.wizardTextEntryLabel());
      addWidget(dirNameLabel);
      txtRepoUrl_ = new TextBox();
      txtRepoUrl_.addStyleName(styles.wizardTextEntry());
      addWidget(txtRepoUrl_);
      
      addSpacer();
     
      existingRepoDestDir_ = new DirectoryChooserTextBox(
            "Clone project into subdirectory of:", txtRepoUrl_);
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
      String url = txtRepoUrl_.getText().trim();
      String dir = existingRepoDestDir_.getText().trim();
      if (url.length() > 0 && dir.length() > 0)
      {
         String repo = guessGitRepoDir(url);
         if (repo.length() == 0)
            return null;

         String repoDir = FileSystemItem.createDir(dir).completePath(repo);
         String projFile = projFileFromDir(repoDir);
         
         VcsCloneOptions vcsOptions = VcsCloneOptions.create("git", 
                                                             url, 
                                                             repo, 
                                                             dir);
         
         return new NewProjectResult(projFile, dir, vcsOptions);
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
         if (txtRepoUrl_.getText().trim().length() == 0
               || existingRepoDestDir_.getText().trim().length() == 0)
         {
            globalDisplay_.showMessage(
                  MessageDialog.WARNING,
                  "Error",
                  "You must specify a git repository URL and existing " +
                  "directory to create the new project within.",
                  txtRepoUrl_);
         }
         else if (guessGitRepoDir(txtRepoUrl_.getText().trim()).length() == 0)
         {
            globalDisplay_.showMessage(
                  MessageDialog.WARNING,
                  "Error",
                  "Could not guess the git repository directory name from " +
                  "the git repository URL. Please clone the repository " +
                  "manually, and then create a new RStudio project within " +
                  "it.");
         }
         
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
   private DirectoryChooserTextBox existingRepoDestDir_;

  

}
