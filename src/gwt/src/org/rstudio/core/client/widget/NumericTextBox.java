/*
 * NumericTextBox.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextBox;

public class NumericTextBox extends TextBox
                                    implements CanSetControlId
{
   public NumericTextBox(Integer min, Integer max, Integer step)
   {
      super();
      
      getElement().setAttribute("type", "number");
      
      setMin(min);
      setMax(max);
      setStep(step);
      
      addDomHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.stopPropagation();
               event.preventDefault();
               setFocus(false);
            }
         }
      }, KeyDownEvent.getType());
   }
   
   public NumericTextBox()
   {
      this(null, null, null);
   }
 
   public NumericTextBox(Integer min, Integer max)
   {
      this(min, max, null);
   }
   
   public void setMin(Integer min)
   {
      if (min != null)
         getElement().setAttribute("min", String.valueOf(min));
   }

   public void setMax(Integer max)
   {
      if (max != null)
         getElement().setAttribute("max", String.valueOf(max));
   }
   
   public void setStep(Integer step)
   {
      if (step != null)
         getElement().setAttribute("step", String.valueOf(step));
   }

   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }
}
