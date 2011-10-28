package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ExistingDirectoryPage extends WizardPage<FileSystemItem,NewProjectResult>
{
   public ExistingDirectoryPage()
   {
      super("Existing Directory", 
            "Associate a project with an existing working directory",
            "Create Project in Existing Directory",
            NewProjectResources.INSTANCE.existingDirectoryIcon(),
            NewProjectResources.INSTANCE.existingDirectoryIconLarge());
      

   }

   @Override
   protected Widget createWidget()
   {
      return new Label("Existing Directory");
      
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
