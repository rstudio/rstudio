/*
 * ManipulatorControlSlider.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.ui.*;
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
      captionLabel.setStyleName(styles.captionLabel());
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
      sliderBar_ = new SliderBar(min, max, this);
      
      // show labels only at the beginning and end
      sliderBar_.setNumLabels(1);
      
      // set step size (default to 1 or continuous decimal as appropriate)
      double step = slider.getStep();
      if (step == -1)
      {        
         // short range or decimals means continuous decimal
         if (range < 2 || hasDecimals(max) || hasDecimals(min) )
            step = range / 250; // ~ one step per pixel
         else
            step = 1;
      }
      sliderBar_.setStepSize(step);
      
      // optional tick marks 
      if (slider.getTicks())
      {
         double numTicks = range / step;
         if (numTicks <= 25) // no more than 25 ticks
            sliderBar_.setNumTicks(new Double(numTicks).intValue());
         else
            sliderBar_.setNumTicks(1);
      }
      else 
      {
         // always at beginning and end
         sliderBar_.setNumTicks(1); 
      }
      
      // update label on change
      sliderBar_.addChangeListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            valueLabel.setText(formatLabel(sliderBar_, 
                                           sliderBar_.getCurrentValue()));
         } 
      });
      sliderBar_.setCurrentValue(value);
      
      // fire changed even on slide completed
      sliderBar_.addSlideCompletedListener(new ChangeListener() {
         @Override
         public void onChange(Widget sender)
         {
            ManipulatorControlSlider.this.onValueChanged(
                        new JSONNumber(sliderBar_.getCurrentValue()));
         }
         
      });
      
      // add slider bar and fully initialize widget
      panel.add(sliderBar_);
      initWidget(panel);
      addControlStyle(styles.slider());
   }
   
   @Override
   public void focus()
   {
      sliderBar_.setFocus(true);
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
   
   private SliderBar sliderBar_;
}
