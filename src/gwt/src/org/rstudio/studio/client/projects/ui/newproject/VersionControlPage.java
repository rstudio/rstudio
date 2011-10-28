package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class VersionControlPage extends WizardPage<FileSystemItem,NewProjectResult>
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
      return new Label("Version Control");
      
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
