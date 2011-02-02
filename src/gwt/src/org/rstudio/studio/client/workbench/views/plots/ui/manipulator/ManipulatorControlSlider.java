package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

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
     
      final SliderBar sliderBar = new SliderBar(slider.getMin(), 
                                                slider.getMax());
      sliderBar.setNumLabels(1);
      sliderBar.setNumTicks(1);
      sliderBar.setStepSize((slider.getMax() - slider.getMin()) / 250);
      sliderBar.addChangeListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            String label = Double.toString(sliderBar.getCurrentValue());
            valueLabel.setText(label);
         } 
      });
      sliderBar.setCurrentValue(value);
      sliderBar.addSlideCompletedListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            ManipulatorControlSlider.this.onValueChanged(
                        new JSONNumber(sliderBar.getCurrentValue()));
         }
         
      });
      panel.add(sliderBar);
      
      
      initWidget(panel);
      setStyleName(styles.slider());
   }
}
