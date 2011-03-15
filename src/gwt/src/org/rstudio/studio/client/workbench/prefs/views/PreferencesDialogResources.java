package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface PreferencesDialogResources extends ClientBundle
{
   public interface Styles extends CssResource
   {
      String preferencesDialog();
      
      String sectionChooser();
      String sectionChooserInner();
      String section();
      String activeSection();
      String outer();
      String indent();
      String first();
      String textBoxWithButton();
      String paneLayoutTable();
   }

   @Source("PreferencesDialog.css")
   Styles styles();

   ImageResource iconAppearance();
   ImageResource iconEdit();
   ImageResource iconPanes();
   ImageResource iconR();
}
