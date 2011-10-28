package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ExistingDirectoryPage extends NewProjectWizardPage
{
   public ExistingDirectoryPage()
   {
      super("Existing Directory", 
            "Associate a project with an existing working directory",
            "Create Project from Existing Directory",
            NewProjectResources.INSTANCE.existingDirectoryIcon(),
            NewProjectResources.INSTANCE.existingDirectoryIconLarge());
      

   }

   @Override
   protected Widget createWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      
      existingProjectDir_ = new DirectoryChooserTextBox("Directory:", null);

      panel.add(existingProjectDir_);
      
      return panel;
   }
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      super.initialize(defaultNewProjectLocation);
   }

   @Override
   protected NewProjectResult collectInput()
   {
      String dir = existingProjectDir_.getText();
      if (dir.length() > 0)
      {
         return new NewProjectResult(projFileFromDir(dir), null, null);
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
               "You must specify an existing working directory to " +
               "create the new project within.");
         
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
      existingProjectDir_.focusButton();
      
   }

   
   private DirectoryChooserTextBox existingProjectDir_;
}
