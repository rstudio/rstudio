package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;


import com.google.gwt.resources.client.CssResource;

public interface ManipulatorStyles extends CssResource
{
   public static ManipulatorStyles INSTANCE = ManipulatorResources.INSTANCE.manipulatorStyles();

   String captionLabel();
   
   String slider();
   String sliderValueLabel();

   String picker();
   
   String checkBox();
   
   String manipulateButton();
}


