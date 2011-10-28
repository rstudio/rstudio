package org.rstudio.studio.client.projects.ui.newproject;



import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;


public interface NewProjectResources extends ClientBundle
{
   static NewProjectResources INSTANCE = 
                  (NewProjectResources)GWT.create(NewProjectResources.class);
   
   
   ImageResource newProjectDirectoryIcon();
   ImageResource newProjectDirectoryIconLarge();
   ImageResource existingDirectoryIcon();
   ImageResource existingDirectoryIconLarge();
   ImageResource projectFromRepositoryIcon();
   ImageResource projectFromRepositoryIconLarge();
   
   static interface Styles extends CssResource
   {
      String wizardTextEntry();
      String wizardTextEntryLabel();
      String wizardSpacer();
   }
   
   @Source("NewProjectWizard.css")
   Styles styles();
}
