package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface ProjectPreferencesDialogResources extends ClientBundle
{
   static interface Styles extends CssResource
   {
      String panelContainer();
      String infoLabel();
      String workspaceGrid();
      String enableCodeIndexing();
      String useSpacesForTab();
      String numberOfTabs();
      String encodingChooser();
   }
  

   @Source("ProjectPreferencesDialog.css")
   Styles styles();
   
   static ProjectPreferencesDialogResources INSTANCE = (ProjectPreferencesDialogResources)GWT.create(ProjectPreferencesDialogResources.class);
}
