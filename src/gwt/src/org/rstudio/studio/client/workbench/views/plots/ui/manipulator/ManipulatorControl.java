/*
 * ManipulatorControl.java
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

import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Composite;

public abstract class ManipulatorControl extends Composite implements CanFocus
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
   
   protected void addControlStyle(String derivedStyleName)
   {
      addStyleName(ManipulatorResources.INSTANCE.manipulatorStyles().control());
      addStyleName(derivedStyleName);
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
