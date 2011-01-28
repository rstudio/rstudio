package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import com.google.gwt.widgetideas.client.SliderBar;

public class ManipulatorControlSlider extends ManipulatorControl
{
   public ManipulatorControlSlider()
   {
      super();
    
      
      SliderBar slider = new SliderBar(0.0, 100.0);
      slider.setStepSize(5.0);
      slider.setCurrentValue(50.0);
      slider.setNumTicks(10);
      slider.setNumLabels(5);
      
      
      
      addWidget(slider);
   }

}
