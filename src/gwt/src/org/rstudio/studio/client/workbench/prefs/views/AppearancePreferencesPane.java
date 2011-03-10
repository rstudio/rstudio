package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class AppearancePreferencesPane extends PreferencesPane
{
   @Inject
   public AppearancePreferencesPane(PreferencesDialogResources res)
   {
      res_ = res;
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconAppearance();
   }

   @Override
   public void onApply()
   {
   }

   @Override
   public String getName()
   {
      return "Appearance";
   }

   private final PreferencesDialogResources res_;
}
