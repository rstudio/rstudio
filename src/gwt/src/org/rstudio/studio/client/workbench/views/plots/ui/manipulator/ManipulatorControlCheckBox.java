/*
 * ManipulatorControlCheckBox.java
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

import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class ManipulatorControlCheckBox extends ManipulatorControl
{

   public ManipulatorControlCheckBox(String variable,
                                     boolean value,
                                     Manipulator.CheckBox checkBox,
                               final ManipulatorChangedHandler changedHandler)
   {
      super(variable, checkBox, changedHandler);
      
      // get manipulator styles
      ManipulatorStyles styles = ManipulatorResources.INSTANCE.manipulatorStyles();
   
      // main control
      HorizontalPanel panel = new HorizontalPanel();
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      
      // checkbox
      checkBox_ = new CheckBox();
      checkBox_.setValue(value);
      checkBox_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            ManipulatorControlCheckBox.this.onValueChanged(
                  JSONBoolean.getInstance(checkBox_.getValue()));
            
         }
      });
      panel.add(checkBox_);
      
      // label
      Label label = new Label(getLabel());
      panel.add(label);
    
      
      initWidget(panel);
      addControlStyle(styles.checkBox());
   }

   @Override
   public void focus()
   {
      checkBox_.setFocus(true);
   }
   
   private CheckBox checkBox_;

}
