package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Composite;

public class ManipulatorControl extends Composite
{
   public ManipulatorControl(String variable, 
                             Manipulator.Control control,
                             ManipulatorChangedHandler changedHandler)
   {
      super();
      variable_ = variable;
      if (control.getLabel() != null)
         label_ = control.getLabel();
      else
         label_ = variable;
      changedHandler_ = changedHandler;
   }
   
   protected String getLabel()
   {
      return label_;
   }
   
   protected void onValueChanged(JSONValue value)
   {
      JSONObject values = new JSONObject();
      values.put(variable_, value);
      changedHandler_.onManipulatorChanged(values);
   }
   
   
   private final String variable_;
   private final String label_;
   private final ManipulatorChangedHandler changedHandler_;
}
