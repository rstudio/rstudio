package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.model.NewProjectResult;


public class NewProjectWizard extends Wizard<FileSystemItem,NewProjectResult>
{
   public NewProjectWizard(
         FileSystemItem defaultNewProjectLocation,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super("New Project", 
            "Create project from:", 
            defaultNewProjectLocation, 
            operation);
    
      setOkButtonCaption("Create Project");
      
      addPage(new NewDirectoryPage());
      addPage(new ExistingDirectoryPage());
      
      if (RStudioGinjector.INSTANCE.getSession()
                                       .getSessionInfo().isVcsAvailable())
      {
         addPage(new VersionControlPage());
      }
   }  
}
