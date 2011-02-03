package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.SliderBar;

@SuppressWarnings("deprecation")
public class ManipulatorControlSlider extends ManipulatorControl
                                      implements SliderBar.LabelFormatter
{
   public ManipulatorControlSlider(String variable, 
                                   double value,
                                   Manipulator.Slider slider,
                                   ManipulatorChangedHandler changedHandler)
   {
      super(variable, slider, changedHandler);
      
      // get manipulator styles
      ManipulatorStyles styles = ManipulatorResources.INSTANCE.manipulatorStyles();
      
      // containing panel
      VerticalPanel panel = new VerticalPanel();
      
      // setup caption panel and add it
      HorizontalPanel captionPanel = new HorizontalPanel();
  
      Label captionLabel = new Label();
      captionLabel.setStyleName(styles.sliderCaptionLabel());
      captionLabel.setText(getLabel() + ":");
      captionPanel.add(captionLabel);
      final Label valueLabel = new Label();
      valueLabel.setStyleName(styles.sliderValueLabel());
      captionPanel.add(valueLabel);
      panel.add(captionPanel);
     
      // create with range and custom formatter
      final double min = slider.getMin();
      final double max = slider.getMax();
      final double range = max - min;
      final SliderBar sliderBar = new SliderBar(min, max, this);
      
      // show labels only at the beginning and end
      sliderBar.setNumLabels(1);
      
      // set step size (default to 1 or continuous decimal as appropriate)
      double step = slider.getStep();
      if (step == -1)
      {        
         // short range or decimals means continous decimal
         if (range < 2 || hasDecimals(max) || hasDecimals(min) )
            step = range / 250; // ~ one step per pixel
         else
            step = 1;
      }
      sliderBar.setStepSize(step);
      
      // optional tick marks 
      if (slider.getTicks())
      {
         double numTicks = range / step;
         sliderBar.setNumTicks(new Double(numTicks).intValue());
      }
      else 
      {
         // always at beginning and end
         sliderBar.setNumTicks(1); 
      }
      
      // update label on change
      sliderBar.addChangeListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            valueLabel.setText(formatLabel(sliderBar, 
                                           sliderBar.getCurrentValue()));
         } 
      });
      sliderBar.setCurrentValue(value);
      
      // fire changed even on slide completed
      sliderBar.addSlideCompletedListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            ManipulatorControlSlider.this.onValueChanged(
                        new JSONNumber(sliderBar.getCurrentValue()));
         }
         
      });
      
      // add slider bar and fully initialize widget
      panel.add(sliderBar);
      initWidget(panel);
      setStyleName(styles.slider());
   }
   
   
   
   @Override
   public String formatLabel(SliderBar slider, double value)
   {
     return StringUtil.prettyFormatNumber(value);
   }
   
   private static boolean hasDecimals(double value)
   {
      double truncatedValue = (double)(Math.round(value));
      return value != truncatedValue;       
   }
}
