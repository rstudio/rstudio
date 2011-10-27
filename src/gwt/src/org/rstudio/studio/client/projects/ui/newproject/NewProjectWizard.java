package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.NewProjectResult;


public class NewProjectWizard extends Wizard<FileSystemItem,NewProjectResult>
{
   public NewProjectWizard(
         GlobalDisplay globalDisplay,
         FileSystemItem defaultNewProjectLocation,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super("New Project", 
            "Create project from:", 
            defaultNewProjectLocation, 
            operation);
      
      globalDisplay_ = globalDisplay;
    
      setOkButtonCaption("Create Project");
      
      addPage(new NewDirectoryPage());
      addPage(new ExistingDirectoryPage());
      addPage(new VersionControlPage());
      
   }
   
 
   
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   
}
