package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class NewDirectoryPage extends WizardPage<FileSystemItem,NewProjectResult>
{
   public NewDirectoryPage()
   {
      super("New Directory", 
            "Start a project in a brand new working directory",
            "Create Project in New Directory",
            NewProjectResources.INSTANCE.newProjectDirectoryIcon(),
            NewProjectResources.INSTANCE.newProjectDirectoryIconLarge());
      
  
   }

   @Override
   protected Widget createWidget()
   {
      return new Label("New Directory");
   }
   
   @Override 
   protected void initialize(FileSystemItem initData)
   {
      
   }
   
   @Override
   protected NewProjectResult collectInput()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected boolean validate(NewProjectResult input)
   {
      
      return true;
   }

   @Override
   public void focus()
   {
      // TODO Auto-generated method stub
      
   }

}
