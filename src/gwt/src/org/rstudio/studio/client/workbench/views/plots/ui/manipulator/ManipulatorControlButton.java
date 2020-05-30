/*
 * ManipulatorControlButton.java
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

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONBoolean;


public class ManipulatorControlButton extends ManipulatorControl
{
   public ManipulatorControlButton(
                       String variable,
                       Manipulator.Button button,
                       final ManipulatorChangedHandler changedHandler)
   {
      super(variable, button, changedHandler);
         
      // button
      button_ = new ThemedButton(button.getLabel());
      button_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            ManipulatorControlButton.this.onValueChanged(
                  JSONBoolean.getInstance(true));
         }  
      });
      
      initWidget(button_);
      
      addControlStyle(
            ManipulatorResources.INSTANCE.manipulatorStyles().button());
   }

   @Override
   public void focus()
   {
      
   }
   
   private ThemedButton button_;

}
