package org.rstudio.studio.client.projects.ui.newproject;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface NewProjectResources extends ClientBundle
{
   static NewProjectResources INSTANCE = 
                  (NewProjectResources)GWT.create(NewProjectResources.class);
   
   ImageResource newProjectDirectoryIcon();
   ImageResource existingDirectoryIcon();
   ImageResource projectFromRepositoryIcon();
}
