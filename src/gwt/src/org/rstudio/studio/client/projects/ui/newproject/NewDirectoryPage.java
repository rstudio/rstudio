package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewDirectoryPage extends NewProjectWizardPage
{
   public NewDirectoryPage()
   {
      super("New Directory", 
            "Start a project in a brand new working directory",
            "Create New Project",
            NewProjectResources.INSTANCE.newProjectDirectoryIcon(),
            NewProjectResources.INSTANCE.newProjectDirectoryIconLarge());
      
  
   }

   @Override
   protected Widget createWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      Label dirNameLabel = new Label("Project directory name:");
      panel.add(dirNameLabel);
      txtProjectName_ = new TextBox();
      panel.add(txtProjectName_);
      
      // project dir
      newProjectParent_ = new DirectoryChooserTextBox("Create in:",
                                                      txtProjectName_);
      panel.add(newProjectParent_);
      
      return panel;
   }
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      super.initialize(defaultNewProjectLocation);
      newProjectParent_.setText(defaultNewProjectLocation.getPath());
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
         return new NewProjectResult(projFile, newDefaultLocation, null);
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
   
   private TextBox txtProjectName_;
   
   private DirectoryChooserTextBox newProjectParent_;

}
