/*
 * LatchingToolbarButton.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.PressedValue;
import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;

/**
 * A button that toggles between pressed and unpressed. If the text is changed to indicate
 * which state the button is in, then "textIndicatesState" must be true; otherwise aria-pressed
 * property will be used so screen readers know what state the button is in.
 */
public class LatchingToolbarButton extends ToolbarButton
{
   public LatchingToolbarButton(String text, 
                                String title,
                                boolean textIndicatesState,
                                ImageResource leftImage,
                                ClickHandler clickHandler)
   {
      super(text, title, leftImage, clickHandler);
      textIndicatesState_ = textIndicatesState;
      if (!textIndicatesState_)
         Roles.getButtonRole().setAriaPressedState(getElement(), PressedValue.FALSE);
      getElement().addClassName(ThemeStyles.INSTANCE.toolbarButtonLatchable());
   }
   
   public void setLatched(boolean latched)
   {
      // no op if we're already in requested state
      if (latched == latched_)
         return;
      latched_ = latched;
      
      if (latched_)
      {
         if (!textIndicatesState_)
            Roles.getButtonRole().setAriaPressedState(getElement(), PressedValue.TRUE);
         getElement().addClassName(ThemeStyles.INSTANCE.toolbarButtonLatched());
      }
      else
      {
         if (!textIndicatesState_)
            Roles.getButtonRole().setAriaPressedState(getElement(), PressedValue.FALSE);
         getElement().removeClassName(ThemeStyles.INSTANCE.toolbarButtonLatched());
      }
   }
   
   private boolean latched_ = false;
   private boolean textIndicatesState_;
}
