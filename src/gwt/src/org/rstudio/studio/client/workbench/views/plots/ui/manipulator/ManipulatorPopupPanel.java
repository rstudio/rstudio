package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.MiniDialogPopupPanel;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ManipulatorPopupPanel extends MiniDialogPopupPanel 
                             
{
   public ManipulatorPopupPanel(final ManipulatorChangedHandler changedHandler,
                                Commands commands)
   {
      super(true, false);
      
      changedHandler_ = changedHandler;
      commands_ = commands;
      
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
      
      if (manipulator != null)
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
               // if you hide the manpulate dialog with the keyboard then
               // focus appears to go nowhere. put it back in the console.
               commands_.activateConsole().execute();
               break;
         } 
      }
   }
  
   private VerticalPanel mainPanel_;
   private ManipulatorControl firstControl_;
   private final ManipulatorChangedHandler changedHandler_;
   private final Commands commands_;

}
