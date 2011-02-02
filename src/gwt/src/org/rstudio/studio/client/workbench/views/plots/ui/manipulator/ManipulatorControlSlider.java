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
      final SliderBar sliderBar = new SliderBar(slider.getMin(), 
                                                slider.getMax(),
                                                this);
      
      // show labels only at the beginning and end
      sliderBar.setNumLabels(1);
      
      // compute step size (default to 1, but if the range is less than 2
      // then is probably should be a continuous decimal treatment)
      boolean overrodeDefaultStep = false;
      double range = slider.getMax() - slider.getMin();
      double step = slider.getStep();
      if (step == 1 && range < 2)
      {
         step = range / 250; // ~ one step per pixel
         overrodeDefaultStep = true;
      }
      sliderBar.setStepSize(step);
      
      // optional tick marks
      if (slider.getTicks() && !overrodeDefaultStep)
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
}
