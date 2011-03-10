package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class PaneLayoutPreferencesPane extends PreferencesPane
{
   @Inject
   public PaneLayoutPreferencesPane(PreferencesDialogResources res)
   {
      res_ = res;
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconPanes();
   }

   @Override
   public void onApply()
   {
   }

   @Override
   public String getName()
   {
      return "Pane Layout";
   }

   private final PreferencesDialogResources res_;
}
