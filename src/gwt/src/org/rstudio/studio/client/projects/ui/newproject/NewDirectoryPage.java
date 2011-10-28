package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;

public class NewDirectoryPage extends WizardPage<FileSystemItem,NewProjectResult>
{
   public NewDirectoryPage()
   {
      super("New Directory", 
            "Start a project in a brand new working directory",
            NewProjectResources.INSTANCE.newProjectDirectoryIcon());
      
      
      Label label = new Label("New Directory");
      initWidget(label);
   }

   @Override
   protected void initialize(FileSystemItem initData)
   {
      // TODO Auto-generated method stub
      
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

}
