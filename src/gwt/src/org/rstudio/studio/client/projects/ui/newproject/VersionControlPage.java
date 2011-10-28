package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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
   protected Widget createWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      
      Label dirNameLabel = new Label("Git repository URL/location:");
      panel.add(dirNameLabel);
      txtRepoUrl_ = new TextBox();
      panel.add(txtRepoUrl_);
      
      existingRepoDestDir_ = new DirectoryChooserTextBox("Create in:",
                                                         txtRepoUrl_);
      panel.add(existingRepoDestDir_);
      
      return panel;
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
         return new NewProjectResult(projFile, dir, url);
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
