package org.rstudio.studio.client.workbench.views.plots.ui.export;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface ExportPlotDialogResources extends ClientBundle
{  
   public static interface Styles extends CssResource
   {
      String mainWidget();
      String imagePreview();
   }

  
   @Source("ExportPlotDialog.css")
   Styles styles();
   
   public static ExportPlotDialogResources INSTANCE = 
      (ExportPlotDialogResources)GWT.create(ExportPlotDialogResources.class) ;
  
}