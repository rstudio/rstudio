package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.Debug;
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
      
      if (manipulator != null)
      {         
         // iterate over the variables
         JsArrayString variables = manipulator.getVariables();
         for (int i=0; i<variables.length(); i++)
         {
            String variable = variables.get(i);
            try
            {
               Manipulator.Control control = manipulator.getControl(variable);
               switch(control.getType())
               {
               case Manipulator.Control.SLIDER:
                  Manipulator.Slider slider = control.cast();
                  addSliderControl(variable, 
                                   manipulator.getDoubleValue(variable), 
                                   slider);
                  break;
               case Manipulator.Control.PICKER:
                  Manipulator.Picker picker = control.cast();
                  addPickerControl(variable, picker);
                  break;
               case Manipulator.Control.CHECKBOX:
                  Manipulator.Checkbox checkbox = control.cast();
                  addCheckboxControl(variable, checkbox);
                  break;
               }
            }
            catch(Throwable e)
            {
               Debug.log("WARNING: exception occurred during addition of " +
                         "variable " + variable + ", " + e.getMessage());
            }
            
         }
      }
   }
   
   private void addSliderControl(String variable, 
                                 double value, 
                                 Manipulator.Slider slider)
   {
      ManipulatorControlSlider sliderControl = 
         new ManipulatorControlSlider(variable, value, slider, changedHandler_);
      mainPanel_.add(sliderControl);
   }
   
   private void addPickerControl(String variable, Manipulator.Picker picker)
   {
      
   }
   
   private void addCheckboxControl(String variable, 
                                   Manipulator.Checkbox checkbox)
   {
      
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
   private final ManipulatorChangedHandler changedHandler_;

}
