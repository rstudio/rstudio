package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.Label;

public class ExistingDirectoryPage extends WizardPage<FileSystemItem,NewProjectResult>
{
   public ExistingDirectoryPage()
   {
      super("Existing Directory", 
            "Attach a projec to an existing working directory",
            NewProjectResources.INSTANCE.existingDirectoryIcon());
      
      
      Label label = new Label("Existing Directory");
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
