/*
 * ManipulatorPopupPanel.java
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.MiniDialogPopupPanel;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ManipulatorPopupPanel extends MiniDialogPopupPanel 
                             
{
   public ManipulatorPopupPanel(final ManipulatorChangedHandler changedHandler)
   {
      super(true, false);
      
      changedHandler_ = changedHandler;
      
      
      setCaption("Manipulate");
   
       
      
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new VerticalPanel();
      mainPanel_.setWidth("250px");
      return mainPanel_;
   }
   
   public void update(Manipulator manipulator)
   {  
      mainPanel_.clear();
      
      if (manipulator != null && manipulator.getVariables() != null)
      {         
         // iterate over the variables
         JsArrayString variables = manipulator.getVariables();
         for (int i=0; i<variables.length(); i++)
         {
            String variable = variables.get(i);
            try
            {
               ManipulatorControl addedControl = null;
               Manipulator.Control control = manipulator.getControl(variable);
               switch(control.getType())
               {
               case Manipulator.Control.SLIDER:
                  Manipulator.Slider slider = control.cast();
                  addedControl = addSliderControl(
                                          variable, 
                                          manipulator.getDoubleValue(variable),
                                          slider);
                  break;
               case Manipulator.Control.PICKER:
                  Manipulator.Picker picker = control.cast();
                  addedControl = addPickerControl(
                                          variable,
                                          manipulator.getStringValue(variable),
                                          picker);
                  break;
               case Manipulator.Control.CHECKBOX:
                  Manipulator.CheckBox checkBox = control.cast();
                  addedControl = addCheckBoxControl(
                                          variable, 
                                          manipulator.getBooleanValue(variable),
                                          checkBox);
                  break;
               case Manipulator.Control.BUTTON:
                  Manipulator.Button button = control.cast();
                  addedControl = addButtonControl(variable, button); 
                  break;
               }
               
               // save reference to first control (for setting focus)
               if (i == 0)
                  firstControl_ = addedControl;
            }
            catch(Throwable e)
            {
               Debug.log("WARNING: exception occurred during addition of " +
                         "variable " + variable + ", " + e.getMessage());
            }
            
         }
      }
   }
   
   public void focusFirstControl()
   {
      if (firstControl_ != null) // defensive
      {
         FocusHelper.setFocusDeferred(firstControl_);
      }
   }
   
   private ManipulatorControl addSliderControl(String variable, 
                                               double value, 
                                               Manipulator.Slider slider)
   {
      ManipulatorControlSlider sliderControl = 
         new ManipulatorControlSlider(variable, value, slider, changedHandler_);
      mainPanel_.add(sliderControl);
      return sliderControl;
   }
   
   private ManipulatorControl addPickerControl(String variable,
                                               String value,
                                               Manipulator.Picker picker)
   {
      ManipulatorControlPicker pickerControl =
         new ManipulatorControlPicker(variable, value, picker, changedHandler_);
      mainPanel_.add(pickerControl);
      return pickerControl;
   }
   
   private ManipulatorControl addCheckBoxControl(String variable, 
                                                 boolean value,
                                                 Manipulator.CheckBox checkBox)
   {
      ManipulatorControlCheckBox checkBoxControl =
         new ManipulatorControlCheckBox(variable, value, checkBox, changedHandler_);
      mainPanel_.add(checkBoxControl);
      return checkBoxControl;
   }
   
   private ManipulatorControl addButtonControl(String variable, 
                                               Manipulator.Button button)
{
      ManipulatorControlButton buttonControl =
         new ManipulatorControlButton(variable, button, changedHandler_);
      mainPanel_.add(buttonControl);
      return buttonControl;
}
   
   
   
   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
 
      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         NativeEvent nativeEvent = event.getNativeEvent();
         switch (nativeEvent.getKeyCode())
         {
            case KeyCodes.KEY_ESCAPE:
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               event.cancel();
               hideMiniDialog();
               break;
         } 
      }
   }
  
   private VerticalPanel mainPanel_;
   private ManipulatorControl firstControl_;
   private final ManipulatorChangedHandler changedHandler_;

}
