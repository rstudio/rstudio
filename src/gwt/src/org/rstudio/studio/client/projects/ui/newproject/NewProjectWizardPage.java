package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.resources.client.ImageResource;

public abstract class NewProjectWizardPage 
                     extends WizardPage<FileSystemItem,NewProjectResult>
{
   public NewProjectWizardPage(String title, 
                               String subTitle, 
                               String pageCaption, 
                               ImageResource image,
                               ImageResource largeImage)
   {
      super(title, subTitle, pageCaption, image, largeImage);
      
   }
   
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      defaultNewProjectLocation_ = defaultNewProjectLocation;
   }
   
   
   
   
   protected String projFileFromDir(String dir)
   {
      FileSystemItem dirItem = FileSystemItem.createDir(dir);
      return FileSystemItem.createFile(
        dirItem.completePath(dirItem.getStem() + ".Rproj")).getPath();
   }
   
   protected FileSystemItem defaultNewProjectLocation_;
   
   protected final GlobalDisplay globalDisplay_ = 
                           RStudioGinjector.INSTANCE.getGlobalDisplay();
}
