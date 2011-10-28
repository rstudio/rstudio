package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

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
   protected Widget createWidget()
   {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.setWidth("100%");
      
      addWidgets(flowPanel);
      
      return flowPanel;
   }
   
   protected abstract void addWidgets(FlowPanel panel);
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      defaultNewProjectLocation_ = defaultNewProjectLocation;
   }
   
   
   protected void addSpacer(FlowPanel panel)
   {
      Label spacerLabel = new Label();
      spacerLabel.addStyleName(
                     NewProjectResources.INSTANCE.styles().wizardSpacer());
      panel.add(spacerLabel);
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
