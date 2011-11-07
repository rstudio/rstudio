package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.projects.model.RProjectConfig;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectSourceControlPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectSourceControlPreferencesPane()
   {
      
   }

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconSourceControl();
   }

   @Override
   public String getName()
   {
      return "Source Control";
   }

   @Override
   protected void initialize(RProjectConfig prefs)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void onApply(RProjectConfig prefs)
   {
      // TODO Auto-generated method stub
      
   }

}
