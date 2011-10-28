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
      flowPanel_ = new FlowPanel();
      flowPanel_.setWidth("100%");
      
      onAddWidgets();
      
      return flowPanel_;
   }
   
   protected abstract void onAddWidgets();
   
   
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      defaultNewProjectLocation_ = defaultNewProjectLocation;
   }
   
   protected void addWidget(Widget widget)
   {
      widget.addStyleName(NewProjectResources.INSTANCE.styles().wizardWidget());
      flowPanel_.add(widget);
   }
   
   protected void addSpacer()
   {
      Label spacerLabel = new Label();
      spacerLabel.addStyleName(
                     NewProjectResources.INSTANCE.styles().wizardSpacer());
      flowPanel_.add(spacerLabel);
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
   
   private FlowPanel flowPanel_;
}
